# MotoLink MVP

摩托车友组队与半双工对讲的 **微信小程序 MVP**。当前仓库先把最难的业务骨架跑通：

- 开发态快捷登录；
- 创建 / 加入骑行车队；
- 小程序地图定位与附近车友查询；
- WebSocket 队内实时位置广播；
- 服务端抢麦租约（半双工 floor control）；
- 按住说话 / 松手释放的对讲交互；
- 可替换的 RTC Provider，默认使用 Mock，便于先验证产品流程；
- Spring Boot API 与基础 CI。

> 当前版本是可运行的产品骨架，不把“页面按钮能动”伪装成已经完成商用语音。真实 TRTC 接入必须在企业小程序取得 `live-pusher` / `live-player` 权限、准备 SDKAppID 和服务端签名后完成，并进行真机和骑行路测。

## 仓库结构

```text
motolink/
├── apps/miniprogram/       微信原生小程序（无跨端框架）
├── services/api/           Spring Boot 4 / Java 21 API
├── docs/                   架构、范围、协议与 TRTC 接入说明
├── infra/                  PostgreSQL/PostGIS、Redis 开发环境
└── .github/workflows/      CI
```

## 5 分钟启动

### 1. 启动后端

需要 Java 21 与 Maven 3.9+：

```bash
cd services/api
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

### 2. 导入微信小程序

1. 打开微信开发者工具；
2. 导入 `apps/miniprogram`；
3. MVP 阶段可使用测试 AppID；
4. 本地联调时，在开发者工具中关闭“校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”；
5. 模拟器默认访问 `http://127.0.0.1:8080`。真机调试时，将 `miniprogram/config/index.js` 改为电脑局域网地址，并保证手机与电脑在同一网络。

进入小程序后，先做“开发态登录”，再创建车队。用两台手机或两个开发者工具实例进入同一车队，即可验证抢麦和实时位置事件。

## 当前 MVP 的边界

当前后端使用内存存储，重启后会清空用户、车队和位置数据；RTC 默认为 Mock，只验证抢麦与 UI 状态机。以下内容刻意不在首个骨架提交中假装完成：

- 真实手机号验证码和微信登录；
- 真实 TRTC 音频上行 / 下行；
- 锁屏后台对讲与后台定位保证；
- PostgreSQL / Redis 持久化；
- 社区、私信、支付、审核后台；
- 生产级风控、内容审核和运维体系。

详见 [MVP 范围](docs/MVP_SCOPE.md) 与 [后续路线图](docs/ROADMAP.md)。

## 核心设计

语音媒体流与“谁可以讲话”是两回事：

- RTC 厂商负责低延迟音频媒体流；
- MotoLink 服务端负责车队、成员、抢麦、租约超时、队长优先级与审计；
- 小程序只在获得 `FLOOR_GRANTED` 后打开音频上行，松手立即停止并释放麦权。

这样后续可以替换 RTC 厂商，而不会把核心业务规则锁死在第三方 SDK 中。

## 安全说明

- 不要把 TRTC SecretKey、短信密钥、云服务密钥写进小程序或提交到 GitHub；
- TRTC UserSig 必须由服务端生成；
- MVP 的开发态登录仅用于本地联调，不能用于生产；
- 附近陌生人的坐标接口默认做了约百米级模糊化，精确位置只应在明确加入同一车队后共享。
