const TrtcModule = require('trtc-wx-sdk');

const TRTC = TrtcModule && TrtcModule.default ? TrtcModule.default : TrtcModule;
const JOIN_TIMEOUT_MS = 15000;

class TrtcRtcProvider {
  constructor(pageContext) {
    this.pageContext = pageContext;
    this.trtc = null;
    this.event = null;
    this.ticket = null;
    this.joined = false;
    this.publishing = false;
    this.publishRequested = false;
    this.handlers = null;
    this.joinResolve = null;
    this.joinReject = null;
    this.joinTimer = null;
  }

  async initialize(ticket) {
    validateTicket(ticket);
    if (typeof TRTC !== 'function') {
      throw new Error('TRTC SDK 未正确构建，请在微信开发者工具中执行“构建 npm”');
    }

    this.ticket = ticket;
    this.trtc = new TRTC(this.pageContext);
    this.event = this.trtc.EVENT;
    this.bindEvents();

    const pusher = this.trtc.createPusher({
      mode: 'RTC',
      enableCamera: false,
      enableMic: false,
      enableAgc: true,
      enableAns: true,
      audioQuality: 'high',
      audioVolumeType: 'voicecall',
    });
    await this.setPageData({ pusher, playerList: [] });
  }

  async join() {
    if (!this.trtc || !this.ticket) {
      throw new Error('TRTC 尚未初始化');
    }
    if (this.joined) return;

    const joinPromise = new Promise((resolve, reject) => {
      this.joinResolve = resolve;
      this.joinReject = reject;
      this.joinTimer = setTimeout(() => {
        this.rejectJoin(new Error('TRTC 进房超时，请检查企业小程序权限、网络和 SDKAppID'));
      }, JOIN_TIMEOUT_MS);
    });

    const pusher = this.trtc.enterRoom({
      sdkAppID: Number(this.ticket.sdkAppId),
      userID: this.ticket.userId,
      userSig: this.ticket.userSig,
      strRoomID: this.ticket.roomId,
      scene: 'rtc',
      recvMode: 2,
      enableCamera: false,
      enableMic: false,
      enableAgc: true,
      enableAns: true,
      audioQuality: 'high',
      audioVolumeType: 'voicecall',
    });

    await this.setPageData({ pusher });
    this.notify('onRtcJoining');

    try {
      this.trtc.getPusherInstance().start({
        fail: (error) => {
          this.rejectJoin(new Error(normalizeError(error, 'TRTC 推流组件启动失败')));
        },
      });
    } catch (error) {
      this.rejectJoin(new Error(normalizeError(error, 'TRTC 推流组件启动失败')));
    }

    return joinPromise;
  }

  async startPublishing() {
    if (!this.joined || !this.trtc) {
      throw new Error('TRTC 尚未进入房间');
    }
    if (this.publishing) return;

    this.publishRequested = true;
    await ensureRecordPermission();
    if (!this.publishRequested || !this.joined || !this.trtc) return;
    const pusher = this.trtc.setPusherAttributes({
      enableCamera: false,
      enableMic: true,
      enableAgc: true,
      enableAns: true,
      audioQuality: 'high',
      audioVolumeType: 'voicecall',
    });
    await this.setPageData({ pusher });
    this.publishing = true;
    this.notify('onRtcPublishingChanged', true);
  }

  async stopPublishing() {
    this.publishRequested = false;
    if (!this.trtc) return;

    const pusher = this.trtc.setPusherAttributes({
      enableCamera: false,
      enableMic: false,
    });
    await this.setPageData({ pusher });
    if (this.publishing) {
      this.publishing = false;
      this.notify('onRtcPublishingChanged', false);
    }
  }

  async leave() {
    if (!this.trtc) return;

    try {
      await this.stopPublishing();
      const result = this.trtc.exitRoom();
      await this.setPageData({
        pusher: result && result.pusher ? result.pusher : {},
        playerList: result && result.playerList ? result.playerList : [],
      });
    } finally {
      this.unbindEvents();
      this.clearJoinWaiter();
      this.joined = false;
      this.publishing = false;
      this.publishRequested = false;
      this.trtc = null;
      this.event = null;
      this.ticket = null;
    }
  }

  handlePusherStateChange(event) {
    if (this.trtc) this.trtc.pusherEventHandler(event);
  }

  handlePusherNetStatus(event) {
    if (this.trtc) this.trtc.pusherNetStatusHandler(event);
  }

  handlePusherError(event) {
    if (this.trtc) this.trtc.pusherErrorHandler(event);
  }

  handlePusherAudioVolumeNotify(event) {
    if (this.trtc) this.trtc.pusherAudioVolumeNotify(event);
  }

  handlePlayerStateChange(event) {
    if (this.trtc) this.trtc.playerEventHandler(event);
  }

  handlePlayerNetStatus(event) {
    if (this.trtc) this.trtc.playerNetStatus(event);
  }

  handlePlayerFullscreenChange(event) {
    if (this.trtc) this.trtc.playerFullscreenChange(event);
  }

  handlePlayerAudioVolumeNotify(event) {
    if (this.trtc) this.trtc.playerAudioVolumeNotify(event);
  }

  getMode() {
    return 'trtc';
  }

  bindEvents() {
    const EVENT = this.event;
    this.handlers = {
      localJoin: () => this.onLocalJoin(),
      localLeave: () => this.onLocalLeave(),
      kickedOut: (event) => this.onKickedOut(event),
      remoteUserLeave: (event) => this.syncRemotePlayers(event),
      remoteAudioAdd: (event) => this.onRemoteAudioAdd(event),
      remoteAudioRemove: (event) => this.syncRemotePlayers(event),
      localNetStateUpdate: (event) => this.onLocalNetStateUpdate(event),
      remoteNetStateUpdate: (event) => this.syncRemotePlayers(event),
      remoteAudioVolumeUpdate: (event) => this.syncRemotePlayers(event),
      localAudioVolumeUpdate: (event) => this.syncLocalPusher(event),
      error: (event) => this.onError(event),
    };

    this.trtc.on(EVENT.LOCAL_JOIN, this.handlers.localJoin, this);
    this.trtc.on(EVENT.LOCAL_LEAVE, this.handlers.localLeave, this);
    this.trtc.on(EVENT.KICKED_OUT, this.handlers.kickedOut, this);
    this.trtc.on(EVENT.REMOTE_USER_LEAVE, this.handlers.remoteUserLeave, this);
    this.trtc.on(EVENT.REMOTE_AUDIO_ADD, this.handlers.remoteAudioAdd, this);
    this.trtc.on(EVENT.REMOTE_AUDIO_REMOVE, this.handlers.remoteAudioRemove, this);
    this.trtc.on(EVENT.LOCAL_NET_STATE_UPDATE, this.handlers.localNetStateUpdate, this);
    this.trtc.on(EVENT.REMOTE_NET_STATE_UPDATE, this.handlers.remoteNetStateUpdate, this);
    this.trtc.on(EVENT.REMOTE_AUDIO_VOLUME_UPDATE, this.handlers.remoteAudioVolumeUpdate, this);
    this.trtc.on(EVENT.LOCAL_AUDIO_VOLUME_UPDATE, this.handlers.localAudioVolumeUpdate, this);
    this.trtc.on(EVENT.ERROR, this.handlers.error, this);
  }

  unbindEvents() {
    if (!this.trtc || !this.event || !this.handlers) return;

    Object.entries({
      LOCAL_JOIN: 'localJoin',
      LOCAL_LEAVE: 'localLeave',
      KICKED_OUT: 'kickedOut',
      REMOTE_USER_LEAVE: 'remoteUserLeave',
      REMOTE_AUDIO_ADD: 'remoteAudioAdd',
      REMOTE_AUDIO_REMOVE: 'remoteAudioRemove',
      LOCAL_NET_STATE_UPDATE: 'localNetStateUpdate',
      REMOTE_NET_STATE_UPDATE: 'remoteNetStateUpdate',
      REMOTE_AUDIO_VOLUME_UPDATE: 'remoteAudioVolumeUpdate',
      LOCAL_AUDIO_VOLUME_UPDATE: 'localAudioVolumeUpdate',
      ERROR: 'error',
    }).forEach(([eventName, handlerName]) => {
      const code = this.event[eventName];
      const handler = this.handlers[handlerName];
      if (code && handler) this.trtc.off(code, handler);
    });
    this.handlers = null;
  }

  onLocalJoin() {
    this.joined = true;
    this.resolveJoin();
    this.notify('onRtcJoined');
  }

  onLocalLeave() {
    this.joined = false;
    this.publishing = false;
    this.publishRequested = false;
    this.notify('onRtcLeft');
  }

  onKickedOut(event) {
    const message = normalizeError(event && event.data, 'TRTC 房间已解散或当前账号被移出');
    this.joined = false;
    this.publishing = false;
    this.publishRequested = false;
    this.rejectJoin(new Error(message));
    this.notify('onRtcError', message);
  }

  onRemoteAudioAdd(event) {
    const player = event && event.data && event.data.player;
    if (!player || !this.trtc) return;

    const playerId = player.streamID || player.id;
    if (!playerId) {
      this.syncRemotePlayers(event);
      return;
    }

    const playerList = this.trtc.setPlayerAttributes(playerId, {
      autoplay: true,
      muteAudio: false,
      muteVideo: true,
      soundMode: 'speaker',
      autoPauseIfNavigate: false,
      autoPauseIfOpenNative: false,
    });
    this.setPageData({ playerList });
  }

  syncRemotePlayers(event) {
    if (!this.trtc) return;
    const eventPlayerList = event && event.data && event.data.playerList;
    const playerList = Array.isArray(eventPlayerList)
      ? eventPlayerList
      : this.trtc.getPlayerList();
    this.setPageData({ playerList });
  }

  syncLocalPusher(event) {
    const pusher = event && event.data && event.data.pusher;
    if (pusher) this.setPageData({ pusher });
  }

  onLocalNetStateUpdate(event) {
    const netStatus = event
      && event.data
      && event.data.pusher
      && event.data.pusher.netStatus;
    const quality = netStatus && netStatus.netQualityLevel;
    this.notify('onRtcNetworkUpdate', {
      quality: Number.isFinite(Number(quality)) ? Number(quality) : null,
      netStatus: netStatus || null,
    });
  }

  onError(event) {
    const data = event && event.data ? event.data : event;
    const message = normalizeError(data, 'TRTC 音频通道发生错误');
    this.rejectJoin(new Error(message));
    this.notify('onRtcError', message, data || null);
  }

  resolveJoin() {
    if (!this.joinResolve) return;
    const resolve = this.joinResolve;
    this.clearJoinWaiter();
    resolve();
  }

  rejectJoin(error) {
    if (!this.joinReject) return;
    const reject = this.joinReject;
    this.clearJoinWaiter();
    reject(error);
  }

  clearJoinWaiter() {
    clearTimeout(this.joinTimer);
    this.joinTimer = null;
    this.joinResolve = null;
    this.joinReject = null;
  }

  setPageData(data) {
    if (!this.pageContext || typeof this.pageContext.setData !== 'function') {
      return Promise.resolve();
    }
    return new Promise((resolve) => this.pageContext.setData(data, resolve));
  }

  notify(method, ...args) {
    const callback = this.pageContext && this.pageContext[method];
    if (typeof callback === 'function') callback.apply(this.pageContext, args);
  }
}

function validateTicket(ticket) {
  if (!ticket || !ticket.configured) {
    const message = ticket && ticket.message
      ? ticket.message
      : 'TRTC 尚未配置，请先配置服务端 SDKAppID 和 SecretKey';
    throw new Error(message);
  }
  if (!ticket.sdkAppId || !ticket.userId || !ticket.userSig || !ticket.roomId) {
    throw new Error('TRTC 进房票据缺少 sdkAppId、userId、userSig 或 roomId');
  }
}

function ensureRecordPermission() {
  return new Promise((resolve, reject) => {
    wx.getSetting({
      success(result) {
        const value = result.authSetting && result.authSetting['scope.record'];
        if (value === true) {
          resolve();
          return;
        }
        if (value === false) {
          reject(new Error('麦克风权限已关闭，请在小程序右上角“设置”中重新开启'));
          return;
        }
        wx.authorize({
          scope: 'scope.record',
          success: resolve,
          fail: () => reject(new Error('需要麦克风权限才能进行车队对讲')),
        });
      },
      fail: () => reject(new Error('无法读取麦克风权限状态')),
    });
  });
}

function normalizeError(error, fallback) {
  if (!error) return fallback;
  if (typeof error === 'string') return error;
  return error.message || error.errMsg || (error.code ? `${fallback}（${error.code}）` : fallback);
}

module.exports = TrtcRtcProvider;
