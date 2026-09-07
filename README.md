# MotoLink

MotoLink 是一个面向摩托车骑行组队场景的 MVP：附近车友、车队房间、半双工 PTT 抢麦、骑行轨迹和基础管理后台。

当前仓库采用 **Flutter 业务层 + iOS/Android 原生 Riding Core 接口 + Spring Boot 模块化单体 + Vue 3 管理端**。首个版本优先验证最难、最有风险的链路，而不是一次性堆满社区和运营功能。

## 当前 MVP 包含

- Flutter 移动端：开发登录、附近车友列表、创建演示车队、按住说话状态机、骑行计时与轨迹演示、个人页。
- 原生桥接模板：iOS `MethodChannel` 与 Android `MethodChannel`，为后续接入 PushToTalk、AVAudioSession、Foreground Service 和 RTC SDK 留出稳定边界。
- Spring Boot API：开发登录、位置更新与附近查询、房间创建/加入、Redis 抢麦租约、骑行开始/轨迹点/结束、管理端查询。
- Vue 3 管理端：总览、用户列表、车队列表。
- 本地基础设施：PostgreSQL/PostGIS、Redis、Docker Compose、Flyway 初始化脚本。
- CI：移动端静态检查与测试、后端单测、管理端构建。

> 当前原生桥接只是可运行的接口壳，尚未绑定具体 RTC 厂商，也没有宣称已完成锁屏后台对讲。RTC、后台定位、蓝牙耳机和实车风噪必须通过单独 POC 后再进入商用实现。

## 仓库结构

```text
apps/mobile        Flutter 移动端与原生桥接模板
apps/admin         Vue 3 + Element Plus 管理端
services/api       Spring Boot 21 API
packages/contracts OpenAPI 契约
docs               架构、范围、运行手册与技术决策
```

## 一键启动后端基础环境

```bash
cp .env.example .env
docker compose up --build
```

启动后：

- API: `http://localhost:8080`
- 健康检查: `http://localhost:8080/actuator/health`
- 管理端: `http://localhost:8081`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

默认会写入少量演示车友。生产环境必须关闭 `MOTOLINK_SEED_DEMO`，并补齐认证、权限、审计和敏感数据保护。

## 启动 Flutter MVP

仓库不固定提交 Flutter 自动生成的 Android/iOS 工程，先运行：

```bash
cd apps/mobile
./tool/bootstrap_platforms.sh
flutter pub get
flutter run --dart-define=DEMO_MODE=true
```

连接本地 API 时：

```bash
# Android 模拟器
flutter run \
  --dart-define=DEMO_MODE=false \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080

# iOS 模拟器
flutter run \
  --dart-define=DEMO_MODE=false \
  --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

## 启动管理端开发环境

```bash
cd apps/admin
npm install
npm run dev
```

## 第一阶段验收目标

第一阶段不是“功能列表全部打勾”，而是证明下面四条链路能够成立：

1. 用户进入车队后保持 RTC 房间连接，按住时申请麦权并实时上行，松手释放。
2. 抢麦冲突由服务端原子仲裁，断线后租约能够自动过期。
3. 骑行状态下持续上报位置，弱网恢复后能够继续同步。
4. iOS/Android 在目标设备、蓝牙耳机、锁屏和网络切换条件下完成实车 POC。

详细边界见 [docs/MVP_SCOPE.md](docs/MVP_SCOPE.md)，架构见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。
