# MotoLink MVP API

Base URL：`http://localhost:8080/api/v1`

当前 API 仅用于纵切片和联调。`dev-login`、内存房间、内存位置和内存麦权不能直接用于生产。

## 1. 演示登录

```http
POST /auth/dev-login
Content-Type: application/json

{
  "nickname": "测试骑手"
}
```

返回稳定的演示 `userId` 和占位 Token。生产版本改为短信/微信/Apple 登录和服务端签名 Token。

## 2. 车队房间

### 查询房间

```http
GET /teams
GET /teams/{teamId}
```

### 创建房间

```http
POST /teams
Content-Type: application/json

{
  "name": "周末环湖小队",
  "ownerId": "00000000-0000-0000-0000-000000000001",
  "routeNote": "城北集合，沿湖骑行约 45km",
  "capacity": 12,
  "privateRoom": false
}
```

### 通过房间号加入

```http
POST /teams/join
Content-Type: application/json

{
  "roomCode": "520131",
  "userId": "00000000-0000-0000-0000-000000000002"
}
```

## 3. PTT 麦权

### 申请麦权

```http
POST /teams/{teamId}/floor/claim
Content-Type: application/json

{
  "userId": "00000000-0000-0000-0000-000000000001"
}
```

成功返回 `leaseToken` 和 `expiresAt`。如果其他成员持有未过期租约，返回 `409 Conflict`。

### 心跳续租

```http
POST /teams/{teamId}/floor/heartbeat
Content-Type: application/json

{
  "userId": "00000000-0000-0000-0000-000000000001",
  "leaseToken": "00000000-0000-0000-0000-000000000099"
}
```

### 释放麦权

```http
DELETE /teams/{teamId}/floor
Content-Type: application/json

{
  "userId": "00000000-0000-0000-0000-000000000001",
  "leaseToken": "00000000-0000-0000-0000-000000000099"
}
```

客户端只有在申请成功后才能让原生 RTC 开始上行；松开按钮时应先本地停止上行，再尽力释放服务端租约。

## 4. 在线位置

### 上报位置

```http
PUT /locations/{userId}
Content-Type: application/json

{
  "latitude": 39.9042,
  "longitude": 116.4074,
  "speedKmh": 42.5,
  "accuracyMeters": 8.0,
  "capturedAt": "2026-09-07T08:00:00Z"
}
```

### 查询附近车友

```http
GET /locations/nearby?latitude=39.9042&longitude=116.4074&radiusMeters=3000&excludeUserId={userId}
```

演示实现将在 30 秒未更新后把位置视为离线。生产实现需要 Redis GEO、查询限频和陌生人位置模糊化。

## 5. 骑行轨迹

### 开始骑行

```http
POST /rides
Content-Type: application/json

{
  "userId": "00000000-0000-0000-0000-000000000001",
  "teamId": null,
  "startedAt": "2026-09-07T08:00:00Z"
}
```

### 批量补传轨迹点

```http
POST /rides/{rideId}/points
Content-Type: application/json

{
  "points": [
    {
      "sequence": 1,
      "capturedAt": "2026-09-07T08:00:01Z",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "speedKmh": 38.2,
      "accuracyMeters": 8.0
    }
  ]
}
```

同一骑行内按 `sequence` 去重。正式数据库约束为 `(ride_id, sequence_no)` 唯一。

### 结束与查询

```http
POST /rides/{rideId}/finish
GET  /rides/{rideId}
```

## 6. 待补控制事件

进入 RTC POC 后增加 WebSocket `/ws/v1/team`，至少包含：

- `member.joined` / `member.left`；
- `floor.granted` / `floor.released` / `floor.revoked`；
- `member.location`；
- `member.too_far`；
- `network.reconnecting`；
- `team.closed`；
- `emergency.requested`。

每条事件包含 `eventId`、`teamId`、`sequence`、`occurredAt` 和 `payload`，客户端按序号去重并忽略过期事件。
