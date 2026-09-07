// 模拟器可使用 127.0.0.1；真机调试请改成电脑局域网 IP 或已备案 HTTPS 域名。
module.exports = {
  apiBaseUrl: 'http://127.0.0.1:8080',
  wsBaseUrl: 'ws://127.0.0.1:8080',
  rtcMode: 'trtc',
  locationIntervalMs: 2000,
  heartbeatIntervalMs: 15000,
};
