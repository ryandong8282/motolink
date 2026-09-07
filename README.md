# MotoLink MVP

摩托车友组队与半双工对讲的 **微信小程序 MVP**。当前仓库已经把腾讯云 TRTC 纯音频接入到现有抢麦流程：

- 开发态快捷登录；
- 创建 / 加入骑行车队；
- 小程序地图定位与附近车友查询；
- WebSocket 队内实时位置广播；
- 服务端抢麦租约（半双工 floor control）；
- 腾讯 TRTC 音频房间与远端音频播放；
- 获得麦权后开麦，松手、断线或退后台立即关麦；
- 服务端生成短期 UserSig，不向小程序暴露 SecretKey；
- Spring Boot API 与基础 CI。

> 代码对接已经完成，但真实声音必须使用具备 `live-pusher` / `live-player` 权限的企业小程序、真实 TRTC 应用参数和两台真机验收。微信开发者工具不能完整模拟原生音频组件。

## 仓库结构

```text
motolink/
├── apps/miniprogram/       微信原生小程序与 trtc-wx-sdk
├── services/api/           Spring Boot 4 / Java 21 API
├── docs/                   架构、范围、协议与 TRTC 接入说明
├── infra/                  PostgreSQL/PostGIS、Redis 开发环境
└── .github/workflows/      CI
```

## 5 分钟启动

### 1. 准备腾讯云 TRTC

在腾讯云实时音视频控制台创建应用，获取：

- `SDKAppID`；
- `SecretKey`。

SecretKey 只配置在后端环境变量中：

```bash
export RTC_PROVIDER=trtc
export TRTC_SDK_APP_ID=1400xxxxxxxx
export TRTC_SECRET_KEY='replace-with-real-secret'
export TRTC_USER_SIG_TTL=24h
```

### 2. 启动后端

需要 Java 21 与 Maven 3.9+：

```bash
cd services/api
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

### 3. 安装小程序 TRTC SDK

```bash
cd apps/miniprogram
npm install
```

打开微信开发者工具后，执行 **工具 → 构建 npm**。

### 4. 导入微信小程序

1. 使用企业主体小程序，并确认所属服务类目可开通实时音视频；
2. 在微信公众平台开通 `live-pusher` 与 `live-player` 权限；
3. 打开微信开发者工具，导入 `apps/miniprogram`；
4. 本地联调时可关闭合法域名校验；
5. 真机调试时，将 `miniprogram/config/index.js` 改为电脑局域网地址，或使用已备案 HTTPS/WSS 域名；
6. 使用两台真机登录不同测试用户，加入同一车队测试讲话。

模拟器默认访问 `http://127.0.0.1:8080`。进入小程序后先做“开发态登录”，再创建或加入车队。

## 半双工音频链路

```text
按住讲话
  -> WebSocket 发送 FLOOR_REQUEST
  -> 服务端授予唯一麦权
  -> 客户端收到 FLOOR_GRANTED
  -> TRTC enableMic=true，开始实时上行
  -> 松手 / 断线 / 页面退后台
  -> TRTC enableMic=false，并释放麦权
```

所有成员进入车队后会保持 TRTC 房间连接和远端音频订阅，但本地麦克风默认关闭。因此按键时不需要重新进房，同时服务端仍是唯一的麦权裁决方。

## 当前 MVP 的边界

本次先做手机本身的音频闭环，明确不包含：

- 头盔蓝牙物理按键；
- 特定头盔设备兼容和专用音频路由；
- 高速骑行风噪的效果承诺；
- 锁屏状态下继续按键讲话；
- 小程序被系统或用户结束后的后台保活保证；
- 真实手机号验证码和微信登录；
- PostgreSQL / Redis 持久化；
- 社区、私信、支付、审核后台；
- 生产级风控、内容审核和运维体系。

页面进入后台时，当前实现会主动关麦并释放麦权，避免手势结束事件丢失后继续采集。后台持续收听的表现作为后续真机测试项，不在当前阶段做绝对承诺。

详见 [MVP 范围](docs/MVP_SCOPE.md)、[TRTC 接入说明](docs/TRTC_INTEGRATION.md) 与 [后续路线图](docs/ROADMAP.md)。

## 安全说明

- 不要把 TRTC SecretKey、短信密钥或云服务密钥写进小程序或提交到 GitHub；
- TRTC UserSig 由后端按当前登录用户和车队成员身份签发；
- MVP 的开发态登录仅用于本地联调，不能用于生产；
- 附近陌生人的坐标接口默认做约百米级模糊化，精确位置只应在明确加入同一车队后共享；
- 正式环境需要 HTTPS/WSS、接口限流、审计、密钥托管和 UserSig 签发监控。
