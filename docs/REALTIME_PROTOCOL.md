# 队内 WebSocket 协议（MVP）

连接地址：

```text
ws://localhost:8080/ws/teams/{teamId}?token={sessionToken}
```

## 客户端消息

```json
{
  "type": "FLOOR_REQUEST",
  "requestId": "optional-client-id",
  "payload": {}
}
```

支持的类型：

- `PING`
- `LOCATION_UPDATE`
- `FLOOR_REQUEST`
- `FLOOR_RELEASE`

位置示例：

```json
{
  "type": "LOCATION_UPDATE",
  "payload": {
    "latitude": 39.9042,
    "longitude": 116.4074,
    "accuracy": 12.4,
    "speed": 8.1,
    "bearing": 90
  }
}
```

## 服务端消息

- `SNAPSHOT`
- `MEMBER_JOINED`
- `MEMBER_LEFT`
- `LOCATION_UPDATED`
- `FLOOR_GRANTED`
- `FLOOR_DENIED`
- `FLOOR_RELEASED`
- `PONG`
- `ERROR`

服务端所有事件均包含：

```json
{
  "type": "FLOOR_GRANTED",
  "timestamp": "2026-09-07T08:00:00Z",
  "payload": {}
}
```

## 麦权语义

- 每个车队同时最多一个持麦者；
- 默认租约 45 秒；
- 同一用户重复申请会刷新自己的租约；
- 松手后客户端必须发送 `FLOOR_RELEASE`；
- 连接断开时服务端尝试立即释放；
- 即使释放消息丢失，租约到期也会回收；
- 后续应增加 5～10 秒心跳续租，而不是允许无限占麦。
