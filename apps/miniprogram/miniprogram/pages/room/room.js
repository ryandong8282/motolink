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
    rtcMode: 'mock',
    rtcStatusText: 'Mock RTC：仅验证抢麦流程',
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
    this.setData({ teamId: this.teamId });
    wx.setKeepScreenOn({ keepScreenOn: true });
    this.initialize();
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
        rtcStatusText: rtcMode === 'mock'
          ? 'Mock RTC：抢麦状态可用，暂不传输真实声音'
          : 'TRTC 已连接',
      });

      this.connectRealtime();
      this.startTimers();
    } catch (error) {
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
      isTalking: mine,
      pressPending: false,
    });
    if (mine && this.rtc) {
      try {
        await this.rtc.startPublishing();
        wx.vibrateShort({ type: 'light' });
      } catch (error) {
        this.releaseFloor();
        wx.showToast({ title: error.message, icon: 'none' });
      }
    }
  },

  async onFloorReleased(payload) {
    if (payload.userId === this.session.userId && this.rtc) {
      await this.rtc.stopPublishing();
    }
    this.setData({
      floorHolderId: '',
      floorHolderName: '当前无人讲话',
      isTalking: false,
      pressPending: false,
    });
  },

  onPressStart() {
    if (!this.socket || this.data.pressPending || this.data.isTalking) return;
    if (this.data.floorHolderId && this.data.floorHolderId !== this.session.userId) {
      wx.showToast({ title: '请等对方说完', icon: 'none', duration: 1000 });
      return;
    }
    this.setData({ pressPending: true });
    const sent = this.socket.send('FLOOR_REQUEST');
    if (!sent) {
      this.setData({ pressPending: false });
      wx.showToast({ title: '实时连接尚未恢复', icon: 'none', duration: 1200 });
    }
  },

  onPressEnd() {
    if (!this.data.isTalking && !this.data.pressPending) return;
    if (this.rtc) this.rtc.stopPublishing();
    this.releaseFloor();
  },

  releaseFloor() {
    if (this.socket) this.socket.send('FLOOR_RELEASE');
    this.setData({ isTalking: false, pressPending: false });
  },

  onCopyCode() {
    wx.setClipboardData({ data: this.data.roomCode });
  },

  async onLeaveTeam() {
    const result = await showConfirm('退出车队', '队长退出会直接解散当前 MVP 车队，确定继续吗？');
    if (!result) return;
    try {
      await api.leaveTeam(this.teamId);
      this.cleanup();
      wx.navigateBack();
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    }
  },

  async cleanup() {
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
