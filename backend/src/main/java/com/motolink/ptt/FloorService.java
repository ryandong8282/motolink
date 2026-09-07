package com.motolink.ptt;

import com.motolink.team.TeamService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FloorService {

    private final TeamService teamService;
    private final Duration leaseDuration;
    private final ConcurrentMap<UUID, FloorLease> leases = new ConcurrentHashMap<>();

    public FloorService(
            TeamService teamService,
            @Value("${motolink.floor-lease-seconds:20}") long leaseSeconds) {
        this.teamService = teamService;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
    }

    public FloorLease claim(UUID teamId, UUID userId) {
        if (!teamService.isMember(teamId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the team before talking");
        }

        Instant now = Instant.now();
        return leases.compute(teamId, (ignored, existing) -> {
            if (existing != null && existing.expiresAt().isAfter(now)
                    && !existing.userId().equals(userId)) {
                throw new FloorBusyException(existing);
            }
            return new FloorLease(
                    teamId,
                    userId,
                    UUID.randomUUID(),
                    now,
                    now.plus(leaseDuration));
        });
    }

    public FloorLease heartbeat(UUID teamId, UUID userId, UUID leaseToken) {
        Instant now = Instant.now();
        return leases.compute(teamId, (ignored, existing) -> {
            validateOwner(existing, userId, leaseToken, now);
            return new FloorLease(
                    teamId,
                    userId,
                    leaseToken,
                    existing.claimedAt(),
                    now.plus(leaseDuration));
        });
    }

    public void release(UUID teamId, UUID userId, UUID leaseToken) {
        Instant now = Instant.now();
        leases.compute(teamId, (ignored, existing) -> {
            validateOwner(existing, userId, leaseToken, now);
            return null;
        });
    }

    public FloorState current(UUID teamId) {
        FloorLease lease = leases.get(teamId);
        if (lease == null || !lease.expiresAt().isAfter(Instant.now())) {
            leases.remove(teamId, lease);
            return new FloorState(teamId, false, null, null);
        }
        return new FloorState(teamId, true, lease.userId(), lease.expiresAt());
    }

    private void validateOwner(
            FloorLease existing,
            UUID userId,
            UUID leaseToken,
            Instant now) {
        if (existing == null || !existing.expiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Floor lease has expired");
        }
        if (!existing.userId().equals(userId) || !existing.leaseToken().equals(leaseToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Floor lease is owned by another member");
        }
    }

    public record FloorLease(
            UUID teamId,
            UUID userId,
            UUID leaseToken,
            Instant claimedAt,
            Instant expiresAt) {
    }

    public record FloorState(
            UUID teamId,
            boolean busy,
            UUID holderUserId,
            Instant expiresAt) {
    }

    public static final class FloorBusyException extends ResponseStatusException {
        public FloorBusyException(FloorLease existing) {
            super(
                    HttpStatus.CONFLICT,
                    "Floor is busy until " + existing.expiresAt());
        }
    }
}
