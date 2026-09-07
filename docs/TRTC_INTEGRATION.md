# TRTC 接入计划

当前代码将 RTC 隔离在 `apps/miniprogram/miniprogram/rtc/`，默认使用 Mock Provider。完成以下前置条件后再切换真实实现。

## 前置条件

1. 企业主体微信小程序；
2. 小程序服务类目允许使用 `live-pusher` 与 `live-player`；
3. 微信公众平台中开通对应组件权限；
4. 腾讯云创建 TRTC 应用，获得 SDKAppID；
5. SecretKey 只放服务端安全配置；
6. 配置合法 request / socket / upload / download 域名；
7. 使用真机测试，微信开发者工具不能完整模拟原生推拉流组件。

腾讯云官方文档（核对日期：2026-09）：

- `trtc-wx` API：<https://cloud.tencent.com/document/product/647/17018>
- 微信小程序快速跑通：<https://cloud.tencent.com/document/product/647/32399>
- 小程序集成准备：<https://cloud.tencent.com/document/product/647/45532>

## 服务端

将 `RtcTicketController` 的 Mock 返回替换为真实签名服务：

```json
{
  "provider": "trtc",
  "sdkAppId": 1234567890,
  "userId": "u_xxx",
  "roomId": "team_xxx",
  "userSig": "server-generated-short-lived-sig",
  "expiresAt": "..."
}
```

要求：

- UserSig 必须短时有效；
- 只能给当前车队成员签发；
- SecretKey 不能出现在小程序包、日志和仓库；
- 应记录签发审计，但不要记录 SecretKey 或完整 token；
- 生产环境应限频。

## 小程序

真实 Provider 应完成：

1. 根据 Ticket 创建 `trtc-wx` 实例；
2. 页面中渲染 `live-pusher` 和远端 `live-player`；
3. 进入房间后默认保持本地音频上行暂停；
4. 收到服务端 `FLOOR_GRANTED` 才恢复上行；
5. 松手、失焦、断线或租约失效时立即暂停上行；
6. 远端音频一直保持订阅；
7. 采集并上报本地 / 远端网络质量事件。

## 不可跳过的 POC

- 两台 iPhone、两台主流 Android 真机；
- 手机麦克风、普通蓝牙耳机、头盔耳机；
- 前台、息屏、切微信页面、系统来电；
- Wi-Fi / 4G / 5G 切换与隧道断网；
- 5、10、20 人进房；
- 抢麦冲突、断网占麦、异常退出；
- 高速风噪实车录音对比。

在 POC 完成前，不应在合同中承诺锁屏后台保活率、固定 300ms 延迟或特定降噪效果。
