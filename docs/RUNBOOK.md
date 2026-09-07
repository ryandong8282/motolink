# 本地运行手册

## 启动

```bash
cp .env.example .env
docker compose up --build
```

## 常用检查

```bash
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/v1/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13800000000","nickname":"测试骑士"}'
```

## 清空本地数据

```bash
docker compose down -v
```

## Redis 抢麦排查

```bash
redis-cli KEYS 'motolink:floor:*'
redis-cli TTL 'motolink:floor:<room-id>'
```

## 生产前必须补齐

- TLS、WAF、限流与密钥托管。
- PostgreSQL 自动备份和恢复演练。
- Redis 高可用或可接受的降级策略。
- 日志脱敏、审计与告警。
- RTC 质量指标、设备型号和网络切换埋点。
