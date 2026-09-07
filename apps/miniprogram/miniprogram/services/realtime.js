const config = require('../config/index');
const sessionStore = require('./session');

class TeamSocketClient {
  constructor(teamId, handlers = {}) {
    this.teamId = teamId;
    this.handlers = handlers;
    this.socketTask = null;
    this.active = false;
    this.open = false;
    this.reconnectAttempt = 0;
    this.reconnectTimer = null;
    this.pendingLocationMessage = null;
  }

  connect() {
    if (this.active) return;
    this.active = true;
    this.openSocket();
  }

  openSocket() {
    const session = sessionStore.getSession();
    if (!session || !this.active) return;

    this.emitState('connecting');
    const url = `${config.wsBaseUrl}/ws/teams/${encodeURIComponent(this.teamId)}`
      + `?token=${encodeURIComponent(session.token)}`;
    const task = wx.connectSocket({ url });
    this.socketTask = task;

    task.onOpen(() => {
      if (task !== this.socketTask) return;
      this.open = true;
      this.reconnectAttempt = 0;
      this.emitState('connected');
      this.flushQueue();
    });

    task.onMessage((message) => {
      if (task !== this.socketTask) return;
      try {
        const event = JSON.parse(message.data);
        if (this.handlers.onEvent) this.handlers.onEvent(event);
      } catch (error) {
        if (this.handlers.onError) this.handlers.onError(error);
      }
    });

    task.onError((error) => {
      if (task !== this.socketTask) return;
      if (this.handlers.onError) this.handlers.onError(error);
    });

    task.onClose(() => {
      if (task !== this.socketTask) return;
      this.open = false;
      this.socketTask = null;
      this.emitState(this.active ? 'reconnecting' : 'closed');
      if (this.active) this.scheduleReconnect();
    });
  }

  send(type, payload = {}) {
    const message = JSON.stringify({
      type,
      requestId: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      payload,
    });
    if (!this.open || !this.socketTask) {
      // A stale FLOOR_REQUEST must never be replayed after reconnect. The only
      // safe queued event is the latest location sample.
      if (type === 'LOCATION_UPDATE') this.pendingLocationMessage = message;
      return false;
    }
    this.socketTask.send({ data: message });
    return true;
  }

  close() {
    this.active = false;
    this.open = false;
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    if (this.socketTask) {
      this.socketTask.close({ code: 1000, reason: 'page_unload' });
      this.socketTask = null;
    }
    this.emitState('closed');
  }

  flushQueue() {
    if (!this.open || !this.socketTask || !this.pendingLocationMessage) return;
    const message = this.pendingLocationMessage;
    this.pendingLocationMessage = null;
    this.socketTask.send({ data: message });
  }

  scheduleReconnect() {
    clearTimeout(this.reconnectTimer);
    const delay = Math.min(1000 * (2 ** this.reconnectAttempt), 10000);
    this.reconnectAttempt += 1;
    this.reconnectTimer = setTimeout(() => this.openSocket(), delay);
  }

  emitState(state) {
    if (this.handlers.onStateChange) this.handlers.onStateChange(state);
  }
}

module.exports = {
  TeamSocketClient,
};
