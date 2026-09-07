const api = require('../../services/api');
const config = require('../../config/index');
const locationService = require('../../services/location');
const { TeamSocketClient } = require('../../services/realtime');
const sessionStore = require('../../services/session');
const { createRtcProvider } = require('../../rtc/index');

Page({
  data: {
    teamId: '',
    team: null,
    roomCode: '',
    members: [],
    connectionState: '初始化中',
    connectionTone: 'warning',
    rtcMode: config.rtcMode,
    rtcConnected: false,
    rtcStatusText: config.rtcMode === 'trtc'
      ? '正在连接腾讯 TRTC 纯音频通道'
      : 'Mock RTC：仅验证抢麦流程',
    rtcNetworkText: '等待网络质量数据',
    pusher: {},
    playerList: [],
    floorHolderId: '',
    floorHolderName: '当前无人讲话',
    isTalking: false,
    pressPending: false,
    onlineLocationCount: 0,
  },

  onLoad(options) {
    const session = sessionStore.requireSession();
    if (!session) return;
    this.session = session;
    this.teamId = options.teamId;
    this.locationUserIds = new Set();
    this.pressActive = false;
    this.cleanedUp = false;
    this.setData({ teamId: this.teamId });
    wx.setKeepScreenOn({ keepScreenOn: true });
    this.initialize();
  },

  onHide() {
    // 进入后台时无法继续可靠地保持“按住”手势，立即关麦，避免意外持续采集。
    this.pressActive = false;
    this.stopLocalPublishingForSafety();
  },

  onUnload() {
    this.cleanup();
    wx.setKeepScreenOn({ keepScreenOn: false });
  },

  async initialize() {
    try {
      const [team, rtcTicket] = await Promise.all([
        api.getTeam(this.teamId),
        api.getRtcTicket(this.teamId),
      ]);
      this.setData({
        team,
        roomCode: team.roomCode,
        members: team.members.map((member) => ({
          ...member,
          roleText: member.leader ? '队长' : '队员',
          avatarText: member.nickname.substring(0, 1),
        })),
      });

      this.rtc = createRtcProvider(this);
      await this.rtc.initialize(rtcTicket);
      await this.rtc.join();
      const rtcMode = this.rtc.getMode();
      this.setData({
        rtcMode,
        rtcConnected: true,
        rtcStatusText: rtcMode === 'mock'
          ? 'Mock RTC：抢麦状态可用，暂不传输真实声音'
          : '腾讯 TRTC 纯音频已连接 · 基础降噪开启',
      });

      this.connectRealtime();
      this.startTimers();
    } catch (error) {
      await this.cleanup();
      wx.showModal({
        title: '无法进入车队',
        content: error.message,
        showCancel: false,
        success: () => wx.navigateBack(),
      });
    }
  },

  connectRealtime() {
    this.socket = new TeamSocketClient(this.teamId, {
      onStateChange: (state) => this.onSocketState(state),
      onEvent: (event) => this.onRealtimeEvent(event),
      onError: () => {
        this.setData({
          connectionState: '连接波动，正在恢复',
          connectionTone: 'warning',
        });
        this.stopLocalPublishingForSafety();
      },
    });
    this.socket.connect();
  },

  startTimers() {
    this.heartbeatTimer = setInterval(() => {
      if (this.socket) this.socket.send('PING');
    }, config.heartbeatIntervalMs);

    const pushLocation = async () => {
      try {
        const location = await locationService.getCurrentLocation();
        if (this.socket) this.socket.send('LOCATION_UPDATE', location);
      } catch (error) {
        console.warn('[location]', error.message);
      }
    };
    pushLocation();
    this.locationTimer = setInterval(pushLocation, config.locationIntervalMs);
  },

  onSocketState(state) {
    const view = {
      connected: ['实时连接正常', 'success'],
      connecting: ['正在连接车队', 'warning'],
      reconnecting: ['弱网重连中', 'warning'],
      closed: ['连接已关闭', 'danger'],
    }[state] || ['连接状态未知', 'warning'];
    this.setData({ connectionState: view[0], connectionTone: view[1] });

    if (state !== 'connected') {
      this.pressActive = false;
      this.stopLocalPublishingForSafety();
    }
  },

  async onRealtimeEvent(event) {
    const payload = event.payload || {};
    switch (event.type) {
      case 'SNAPSHOT': {
        const floor = payload.floor && payload.floor.userId ? payload.floor : null;
        this.locationUserIds = new Set(
          (payload.locations || []).map((location) => location.userId),
        );
        this.setData({
          onlineLocationCount: this.locationUserIds.size,
          floorHolderId: floor ? floor.userId : '',
          floorHolderName: floor ? `${floor.nickname} 正在讲话` : '当前无人讲话',
        });
        break;
      }
      case 'LOCATION_UPDATED':
        if (payload.userId) this.locationUserIds.add(payload.userId);
        this.setData({ onlineLocationCount: this.locationUserIds.size });
        break;
      case 'MEMBER_LEFT':
        if (payload.userId) this.locationUserIds.delete(payload.userId);
        this.setData({ onlineLocationCount: this.locationUserIds.size });
        break;
      case 'FLOOR_GRANTED':
        await this.onFloorGranted(payload);
        break;
      case 'FLOOR_DENIED':
        this.pressActive = false;
        this.setData({ pressPending: false });
        wx.showToast({
          title: payload.reason || '当前有人占麦',
          icon: 'none',
          duration: 1200,
        });
        break;
      case 'FLOOR_RELEASED':
        await this.onFloorReleased(payload);
        break;
      case 'ERROR':
        this.pressActive = false;
        await this.stopLocalPublishingForSafety();
        wx.showToast({ title: payload.message || '实时消息错误', icon: 'none' });
        break;
      default:
        break;
    }
  },

  async onFloorGranted(lease) {
    const mine = lease.userId === this.session.userId;
    this.setData({
      floorHolderId: lease.userId,
      floorHolderName: mine ? '你正在讲话' : `${lease.nickname} 正在讲话`,
      isTalking: false,
      pressPending: false,
    });

    if (!mine || !this.rtc) return;

    // 麦权返回前手指可能已经松开。此时必须立即归还，绝不能再开启麦克风。
    if (!this.pressActive) {
      this.releaseFloor();
      return;
    }

    try {
      await this.rtc.startPublishing();
      if (!this.pressActive) {
        await this.rtc.stopPublishing();
        this.releaseFloor();
        return;
      }
      this.setData({ isTalking: true });
      wx.vibrateShort({ type: 'light' });
    } catch (error) {
      this.pressActive = false;
      await this.stopLocalPublishingForSafety();
      this.releaseFloor();
      wx.showToast({ title: error.message, icon: 'none', duration: 2500 });
    }
  },

  async onFloorReleased(payload) {
    if (payload.userId === this.session.userId && this.rtc) {
      await this.rtc.stopPublishing();
    }
    this.pressActive = false;
    this.setData({
      floorHolderId: '',
      floorHolderName: '当前无人讲话',
      isTalking: false,
      pressPending: false,
    });
  },

  onPressStart() {
    if (!this.socket || this.data.pressPending || this.data.isTalking) return;
    if (!this.data.rtcConnected) {
      wx.showToast({ title: '语音通道尚未连接', icon: 'none', duration: 1200 });
      return;
    }
    if (this.data.floorHolderId && this.data.floorHolderId !== this.session.userId) {
      wx.showToast({ title: '请等对方说完', icon: 'none', duration: 1000 });
      return;
    }

    this.pressActive = true;
    this.setData({ pressPending: true });
    const sent = this.socket.send('FLOOR_REQUEST');
    if (!sent) {
      this.pressActive = false;
      this.setData({ pressPending: false });
      wx.showToast({ title: '实时连接尚未恢复', icon: 'none', duration: 1200 });
    }
  },

  async onPressEnd() {
    if (!this.data.isTalking && !this.data.pressPending && !this.pressActive) return;
    this.pressActive = false;
    try {
      if (this.rtc) await this.rtc.stopPublishing();
    } finally {
      this.releaseFloor();
    }
  },

  releaseFloor() {
    if (this.socket) this.socket.send('FLOOR_RELEASE');
    this.setData({ isTalking: false, pressPending: false });
  },

  async stopLocalPublishingForSafety() {
    if (this.rtc) {
      try {
        await this.rtc.stopPublishing();
      } catch (error) {
        console.warn('[rtc] stop publishing failed', error.message);
      }
    }
    this.setData({ isTalking: false, pressPending: false });
    if (this.socket) this.socket.send('FLOOR_RELEASE');
  },

  onRtcJoining() {
    this.setData({
      rtcConnected: false,
      rtcStatusText: '正在进入腾讯 TRTC 纯音频房间',
    });
  },

  onRtcJoined() {
    this.setData({
      rtcConnected: true,
      rtcStatusText: '腾讯 TRTC 纯音频已连接 · 基础降噪开启',
    });
  },

  onRtcLeft() {
    this.setData({
      rtcConnected: false,
      rtcStatusText: '已退出腾讯 TRTC 房间',
    });
  },

  onRtcPublishingChanged(publishing) {
    if (!publishing) this.setData({ isTalking: false });
  },

  onRtcNetworkUpdate(status) {
    const quality = status && status.quality;
    let text = '网络质量未知';
    if (quality === 1 || quality === 2) text = '语音网络良好';
    else if (quality === 3 || quality === 4) text = '语音网络一般';
    else if (quality >= 5) text = '语音网络较差，可能出现卡顿';
    this.setData({ rtcNetworkText: text });
  },

  async onRtcError(message) {
    this.pressActive = false;
    await this.stopLocalPublishingForSafety();
    this.setData({
      rtcConnected: false,
      rtcStatusText: message || '腾讯 TRTC 音频通道发生错误',
    });
  },

  onPusherStateChange(event) {
    if (this.rtc && typeof this.rtc.handlePusherStateChange === 'function') {
      this.rtc.handlePusherStateChange(event);
    }
  },

  onPusherNetStatus(event) {
    if (this.rtc && typeof this.rtc.handlePusherNetStatus === 'function') {
      this.rtc.handlePusherNetStatus(event);
    }
  },

  onPusherError(event) {
    if (this.rtc && typeof this.rtc.handlePusherError === 'function') {
      this.rtc.handlePusherError(event);
    }
  },

  onPusherAudioVolumeNotify(event) {
    if (this.rtc && typeof this.rtc.handlePusherAudioVolumeNotify === 'function') {
      this.rtc.handlePusherAudioVolumeNotify(event);
    }
  },

  onPlayerStateChange(event) {
    if (this.rtc && typeof this.rtc.handlePlayerStateChange === 'function') {
      this.rtc.handlePlayerStateChange(event);
    }
  },

  onPlayerNetStatus(event) {
    if (this.rtc && typeof this.rtc.handlePlayerNetStatus === 'function') {
      this.rtc.handlePlayerNetStatus(event);
    }
  },

  onPlayerFullscreenChange(event) {
    if (this.rtc && typeof this.rtc.handlePlayerFullscreenChange === 'function') {
      this.rtc.handlePlayerFullscreenChange(event);
    }
  },

  onPlayerAudioVolumeNotify(event) {
    if (this.rtc && typeof this.rtc.handlePlayerAudioVolumeNotify === 'function') {
      this.rtc.handlePlayerAudioVolumeNotify(event);
    }
  },

  onCopyCode() {
    wx.setClipboardData({ data: this.data.roomCode });
  },

  async onLeaveTeam() {
    const result = await showConfirm('退出车队', '队长退出会直接解散当前 MVP 车队，确定继续吗？');
    if (!result) return;
    try {
      await api.leaveTeam(this.teamId);
      await this.cleanup();
      wx.navigateBack();
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    }
  },

  async cleanup() {
    if (this.cleanedUp) return;
    this.cleanedUp = true;
    this.pressActive = false;
    clearInterval(this.heartbeatTimer);
    clearInterval(this.locationTimer);
    if (this.socket) {
      this.socket.send('FLOOR_RELEASE');
      this.socket.close();
      this.socket = null;
    }
    if (this.rtc) {
      await this.rtc.stopPublishing();
      await this.rtc.leave();
      this.rtc = null;
    }
  },
});

function showConfirm(title, content) {
  return new Promise((resolve) => {
    wx.showModal({
      title,
      content,
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    });
  });
}
