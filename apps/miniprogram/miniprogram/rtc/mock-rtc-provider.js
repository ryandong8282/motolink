class MockRtcProvider {
  constructor() {
    this.joined = false;
    this.publishing = false;
  }

  async initialize(ticket) {
    this.ticket = ticket;
  }

  async join() {
    this.joined = true;
  }

  async startPublishing() {
    if (!this.joined) throw new Error('RTC 尚未进入房间');
    this.publishing = true;
    console.info('[MockRTC] microphone uplink started');
  }

  async stopPublishing() {
    this.publishing = false;
    console.info('[MockRTC] microphone uplink stopped');
  }

  async leave() {
    this.publishing = false;
    this.joined = false;
  }

  getMode() {
    return 'mock';
  }
}

module.exports = MockRtcProvider;
