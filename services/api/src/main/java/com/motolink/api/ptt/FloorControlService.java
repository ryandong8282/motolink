package com.motolink.api.ptt;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class FloorControlService {

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('get', KEYS[1]) == ARGV[1] then
          redis.call('pexpire', KEYS[1], ARGV[2])
          return 1
        end
        return 0
        """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('get', KEYS[1]) == ARGV[1] then
          return redis.call('del', KEYS[1])
        end
        return 0
        """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration floorTtl;

    public FloorControlService(
        StringRedisTemplate redis,
        @Value("${motolink.ptt.floor-ttl}") Duration floorTtl
    ) {
        this.redis = redis;
        this.floorTtl = floorTtl;
    }

    public LeaseResult request(UUID roomId, UUID userId) {
        String key = key(roomId);
        String value = userId.toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, value, floorTtl);
        if (Boolean.TRUE.equals(acquired)) {
            return result(true, false, userId, floorTtl);
        }

        String holder = redis.opsForValue().get(key);
        if (value.equals(holder)) {
            renew(key, value);
            return result(true, false, userId, floorTtl);
        }
        return result(false, false, parseUuid(holder), remainingTtl(key));
    }

    public LeaseResult heartbeat(UUID roomId, UUID userId) {
        String key = key(roomId);
        boolean renewed = renew(key, userId.toString());
        if (renewed) {
            return result(true, false, userId, floorTtl);
        }
        String holder = redis.opsForValue().get(key);
        return result(false, false, parseUuid(holder), remainingTtl(key));
    }

    public LeaseResult release(UUID roomId, UUID userId) {
        String key = key(roomId);
        Long released = redis.execute(RELEASE_SCRIPT, List.of(key), userId.toString());
        if (released != null && released == 1L) {
            return new LeaseResult(false, true, null, null);
        }
        String holder = redis.opsForValue().get(key);
        return result(false, false, parseUuid(holder), remainingTtl(key));
    }

    public LeaseResult current(UUID roomId) {
        String key = key(roomId);
        String holder = redis.opsForValue().get(key);
        return result(false, false, parseUuid(holder), remainingTtl(key));
    }

    private boolean renew(String key, String userId) {
        Long renewed = redis.execute(
            RENEW_SCRIPT,
            List.of(key),
            userId,
            Long.toString(floorTtl.toMillis())
        );
        return renewed != null && renewed == 1L;
    }

    private LeaseResult result(
        boolean granted,
        boolean released,
        UUID holder,
        Duration remaining
    ) {
        OffsetDateTime expiresAt = remaining == null || remaining.isNegative()
            ? null
            : OffsetDateTime.now().plus(remaining);
        return new LeaseResult(granted, released, holder, expiresAt);
    }

    private Duration remainingTtl(String key) {
        Long millis = redis.getExpire(key, TimeUnit.MILLISECONDS);
        if (millis == null || millis < 0) {
            return null;
        }
        return Duration.ofMillis(millis);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String key(UUID roomId) {
        return "motolink:ptt:floor:" + roomId;
    }

    public record LeaseResult(
        boolean granted,
        boolean released,
        UUID holderUserId,
        OffsetDateTime leaseExpiresAt
    ) {
    }
}
