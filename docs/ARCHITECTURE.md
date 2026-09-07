# 架构说明

## 总体结构

```text
Flutter UI / 业务状态
        │ MethodChannel（后续可迁移 Pigeon）
        ▼
iOS Riding Core / Android Riding Core
        │
        ├── RTC Native SDK（媒体流）
        ├── 后台音频、蓝牙、音频焦点
        └── 后台定位、网络状态

Flutter / Admin ── REST + WebSocket ── Spring Boot API
                                      ├── PostgreSQL/PostGIS
                                      ├── Redis 抢麦租约
                                      └── 对象存储/审核/推送（后续）
```

## 为什么核心不全部放 Dart

锁屏唤醒、系统音频会话、Android 前台服务和原生 RTC 生命周期不能依赖 Flutter 页面仍然存活。Flutter 只调用高层命令：加入房间、申请说话、开始发送、停止发送、离开房间；音频帧和系统生命周期留在原生层。

## PTT 状态机

```text
IDLE -> REQUESTING -> TRANSMITTING -> IDLE
                  └-> DENIED/ERROR -> IDLE
```

1. 用户进入房间时预连接 RTC。
2. 按下按钮请求服务端麦权。
3. Redis 使用 `SET NX + TTL` 保证同一房间只有一个讲话者。
4. 获得麦权后才允许本地音频上行。
5. 讲话期间周期续租；断网或崩溃后 TTL 自动释放。
6. 松手先停止上行，再释放服务端租约。

## 数据策略

- 附近发现：Redis GEO 保存索引，独立 presence key 使用短 TTL；陌生人坐标按约百米粒度模糊化。商用前仍需补限频、隐身和反跟踪策略。
- 骑行轨迹：手机本地先写入，服务端按批次上传；当前 API 已保留逐点写入契约。
- 房间和骑行记录：PostgreSQL 持久化。
- 抢麦：Redis 短租约，不写数据库。

## 安全欠账

当前开发登录使用 `X-User-Id`，只适合本地开发。进入外测前必须增加正式认证、Token 轮换、接口权限、审计、限流、设备风控、数据删除和生产密钥管理。
