package com.motolink.api.room;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class FloorService {
    public static final Duration LEASE = Duration.ofSeconds(15);

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
            Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public FloorService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public FloorResponse request(UUID roomId, UUID userId) {
        String key = key(roomId);
        Boolean granted = redis.opsForValue().setIfAbsent(key, userId.toString(), LEASE);
        String speaker = Boolean.TRUE.equals(granted) ? userId.toString() : redis.opsForValue().get(key);
        return new FloorResponse(Boolean.TRUE.equals(granted), parseUuid(speaker), LEASE.toMillis());
    }

    public FloorResponse heartbeat(UUID roomId, UUID userId) {
        Long renewed = redis.execute(
                RENEW_SCRIPT,
                List.of(key(roomId)),
                userId.toString(),
                String.valueOf(LEASE.toMillis()));
        boolean granted = renewed != null && renewed > 0;
        String speaker = granted ? userId.toString() : redis.opsForValue().get(key(roomId));
        return new FloorResponse(granted, parseUuid(speaker), LEASE.toMillis());
    }

    public FloorResponse release(UUID roomId, UUID userId) {
        redis.execute(RELEASE_SCRIPT, List.of(key(roomId)), userId.toString());
        String speaker = redis.opsForValue().get(key(roomId));
        return new FloorResponse(speaker == null, parseUuid(speaker), LEASE.toMillis());
    }

    private static String key(UUID roomId) {
        return "motolink:floor:" + roomId;
    }

    private static UUID parseUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record FloorResponse(boolean granted, UUID speakerId, long leaseMillis) {}
}
