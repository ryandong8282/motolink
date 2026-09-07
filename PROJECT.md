# MotoLink MVP

MotoLink 是面向摩托车骑行组队场景的移动端 MVP。首阶段只验证最关键的闭环：

1. 开发态手机号登录；
2. 创建或通过房间号加入车队；
3. 进入车队后保持实时连接；
4. 服务端仲裁半双工麦权；
5. 客户端按住说话、松手释放；
6. 队员位置上报与查看；
7. 骑行开始、轨迹点上传、骑行结束；
8. 管理端查看基础运行数据。

## 技术结构

```text
apps/mobile          Flutter 业务界面
packages/riding_core Swift/Kotlin 原生骑行核心桥接层
services/api         Spring Boot 模块化单体 API
apps/admin           Vue 3 管理端
PostgreSQL/PostGIS   用户、房间、轨迹等持久数据
Redis                麦权租约、在线临时状态
WebSocket            房间内临时事件广播
商业 RTC             后续接入；当前使用 mock provider
```

业务页面采用 Flutter，共享 Android 与 iOS 代码；后台音频、后台定位、音频会话、前台服务等能力放在 `riding_core` 原生插件中。真正的语音媒体流不自行实现，后续在该边界内接入 TRTC、Agora 或 ZEGO。

## 本地启动

```bash
cp .env.example .env
docker compose up --build
```

启动后：

- API：`http://localhost:8080`
- Actuator：`http://localhost:8080/actuator/health`
- 管理端：`http://localhost:8088`
- PostgreSQL：`localhost:5432`
- Redis：`localhost:6379`

首次准备 Flutter 工程：

```bash
./scripts/bootstrap-mobile.sh
cd apps/mobile
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

Android 模拟器访问宿主机时，把地址改为：

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

## 当前有意保留的缺口

这是可继续开发的 MVP 骨架，不是已经可以商用上架的成品。下列能力目前只预留接口或使用开发态实现：

- 真实短信验证码、微信登录、JWT 与风控；
- 商业 RTC SDK、RTC Token 正式签发与音频质量统计；
- 高德地图及地图商业授权；
- iOS PushToTalk、Android 厂商后台策略的实机 POC；
- 头盔蓝牙按键、来电打断、音频路由切换；
- 内容社区、私信、审核、推送；
- 生产级限流、审计、监控、备份与灾备。

在真实 RTC、后台定位和实车风噪 POC 通过之前，不应对外承诺锁屏保活率、300ms 延迟或高速风噪指标。
