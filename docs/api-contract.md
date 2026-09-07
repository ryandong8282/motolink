# MVP API Contract

Base URL: `/api/v1`

This scaffold intentionally uses a development login and passes `userId` in request bodies. Replace it with JWT-derived identity before production.

## Authentication

### `POST /auth/dev-login`

```json
{
  "phone": "13800000000",
  "nickname": "RoadRider"
}
```

## Teams

### `POST /teams`

```json
{
  "leaderId": "uuid",
  "name": "Sunday Ride",
  "maxMembers": 12
}
```

### `POST /teams/join`

```json
{
  "userId": "uuid",
  "roomCode": "123456"
}
```

### `GET /teams/{roomId}`

Returns the room and current members.

## PTT floor control

### `POST /teams/{roomId}/ptt/request`

```json
{ "userId": "uuid" }
```

### `POST /teams/{roomId}/ptt/heartbeat`

```json
{ "userId": "uuid" }
```

### `POST /teams/{roomId}/ptt/release`

```json
{ "userId": "uuid" }
```

A response contains `granted`, `holderUserId`, and `leaseExpiresAt`. The client starts RTC publishing only after `granted=true`.

## Locations

### `POST /teams/{roomId}/locations`

```json
{
  "userId": "uuid",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "speedMps": 12.4,
  "heading": 88.0,
  "recordedAt": "2026-09-07T08:00:00Z"
}
```

### `GET /teams/{roomId}/locations`

Returns only the latest point for every room member.

## Rides

### `POST /rides/start`

```json
{
  "userId": "uuid",
  "roomId": "uuid"
}
```

### `POST /rides/{rideId}/points`

```json
{
  "points": [
    {
      "sequenceNo": 1,
      "latitude": 39.9042,
      "longitude": 116.4074,
      "speedMps": 12.4,
      "recordedAt": "2026-09-07T08:00:00Z"
    }
  ]
}
```

### `POST /rides/{rideId}/finish`

```json
{
  "distanceMeters": 15320.5,
  "maxSpeedMps": 27.1
}
```

## RTC adapter

### `POST /rtc/token`

The current implementation returns a mock token and provider marker. Production code must sign a short-lived vendor token on the server and never put the RTC app secret in Flutter or the native plugin.

## WebSocket

Connect to:

```text
/ws/teams?roomId={uuid}&userId={uuid}
```

The MVP relay broadcasts opaque JSON messages within the selected room. Production must authenticate the connection, validate membership, impose size/rate limits, and use versioned event schemas.
