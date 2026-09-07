package com.motolink.api.realtime;

import com.motolink.api.config.MotoLinkProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FloorService {

    private final Map<String, FloorLease> leasesByTeam = new HashMap<>();
    private final Clock clock;
    private final MotoLinkProperties properties;

    public FloorService(Clock clock, MotoLinkProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public synchronized AcquireResult request(
            String teamId,
            String userId,
            String nickname) {
        Instant now = Instant.now(clock);
        FloorLease current = activeLease(teamId, now);
        if (current != null && !current.userId().equals(userId)) {
            return new AcquireResult(false, current, "当前有人正在讲话");
        }

        FloorLease lease = new FloorLease(
                teamId,
                userId,
                nickname,
                "lease_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                now,
                now.plus(properties.floorLease()));
        leasesByTeam.put(teamId, lease);
        return new AcquireResult(true, lease, null);
    }

    public synchronized FloorLease current(String teamId) {
        return activeLease(teamId, Instant.now(clock));
    }

    public synchronized FloorLease release(String teamId, String userId) {
        FloorLease current = activeLease(teamId, Instant.now(clock));
        if (current == null || !current.userId().equals(userId)) {
            return null;
        }
        leasesByTeam.remove(teamId);
        return current;
    }

    /**
     * Removes expired leases so clients can be notified even when no new floor
     * request arrives. In production this state moves to Redis with an atomic
     * compare-and-delete script.
     */
    public synchronized List<FloorLease> expireStale() {
        Instant now = Instant.now(clock);
        List<FloorLease> expired = new ArrayList<>();
        leasesByTeam.entrySet().removeIf(entry -> {
            FloorLease lease = entry.getValue();
            if (!lease.expiresAt().isAfter(now)) {
                expired.add(lease);
                return true;
            }
            return false;
        });
        return List.copyOf(expired);
    }

    private FloorLease activeLease(String teamId, Instant now) {
        FloorLease current = leasesByTeam.get(teamId);
        if (current != null && !current.expiresAt().isAfter(now)) {
            leasesByTeam.remove(teamId);
            return null;
        }
        return current;
    }

    public record AcquireResult(boolean granted, FloorLease lease, String reason) {
    }

    public record FloorLease(
            String teamId,
            String userId,
            String nickname,
            String leaseId,
            Instant acquiredAt,
            Instant expiresAt) {
    }
}
