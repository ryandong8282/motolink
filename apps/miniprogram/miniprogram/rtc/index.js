const config = require('../config/index');
const MockRtcProvider = require('./mock-rtc-provider');
const TrtcRtcProvider = require('./trtc-rtc-provider');

function createRtcProvider(pageContext) {
  if (config.rtcMode === 'trtc') {
    return new TrtcRtcProvider(pageContext);
  }
  return new MockRtcProvider();
}

module.exports = {
  createRtcProvider,
};
