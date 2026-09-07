class TrtcRtcProvider {
  constructor(pageContext) {
    this.pageContext = pageContext;
  }

  async initialize(ticket) {
    if (!ticket || !ticket.configured) {
      throw new Error('TRTC 尚未配置，请先完成 docs/TRTC_INTEGRATION.md 中的前置条件');
    }
    throw new Error('真实 TRTC Provider 尚未接线，禁止在客户端内置 SecretKey');
  }

  async join() {}
  async startPublishing() {}
  async stopPublishing() {}
  async leave() {}

  getMode() {
    return 'trtc';
  }
}

module.exports = TrtcRtcProvider;
