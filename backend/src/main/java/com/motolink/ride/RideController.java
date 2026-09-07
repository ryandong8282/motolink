package com.motolink.ride;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private final ConcurrentMap<UUID, RideState> rides = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideView start(@Valid @RequestBody StartRideRequest request) {
        Instant startedAt = request.startedAt() == null ? Instant.now() : request.startedAt();
        RideState state = new RideState(UUID.randomUUID(), request.userId(), request.teamId(), startedAt);
        rides.put(state.id, state);
        return toView(state);
    }

    @PostMapping("/{rideId}/points")
    public RideView appendPoints(
            @PathVariable UUID rideId,
            @Valid @RequestBody PointBatch request) {
        RideState state = requireRide(rideId);
        synchronized (state) {
            if (state.endedAt != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride has already finished");
            }
            request.points().stream()
                    .sorted(Comparator.comparingLong(TrackPoint::sequence))
                    .forEach(point -> {
                        boolean duplicate = state.points.stream()
                                .anyMatch(existing -> existing.sequence() == point.sequence());
                        if (!duplicate) {
                            state.points.add(point);
                        }
                    });
            state.points.sort(Comparator.comparingLong(TrackPoint::sequence));
            recalculate(state);
        }
        return toView(state);
    }

    @PostMapping("/{rideId}/finish")
    public RideView finish(@PathVariable UUID rideId) {
        RideState state = requireRide(rideId);
        synchronized (state) {
            if (state.endedAt == null) {
                state.endedAt = Instant.now();
                recalculate(state);
            }
        }
        return toView(state);
    }

    @GetMapping("/{rideId}")
    public RideView get(@PathVariable UUID rideId) {
        return toView(requireRide(rideId));
    }

    private RideState requireRide(UUID rideId) {
        RideState state = rides.get(rideId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride does not exist");
        }
        return state;
    }

    private void recalculate(RideState state) {
        double totalMeters = 0;
        double maxSpeedKmh = 0;
        for (int index = 0; index < state.points.size(); index++) {
            TrackPoint point = state.points.get(index);
            maxSpeedKmh = Math.max(maxSpeedKmh, point.speedKmh());
            if (index > 0) {
                TrackPoint previous = state.points.get(index - 1);
                totalMeters += distanceMeters(
                        previous.latitude(), previous.longitude(),
                        point.latitude(), point.longitude());
            }
        }
        state.distanceMeters = totalMeters;
        state.maxSpeedKmh = maxSpeedKmh;
    }

    private RideView toView(RideState state) {
        Instant effectiveEnd = state.endedAt == null ? Instant.now() : state.endedAt;
        long durationSeconds = Math.max(0, Duration.between(state.startedAt, effectiveEnd).toSeconds());
        double averageSpeedKmh = durationSeconds == 0
                ? 0
                : (state.distanceMeters / 1000d) / (durationSeconds / 3600d);
        return new RideView(
                state.id,
                state.userId,
                state.teamId,
                state.startedAt,
                state.endedAt,
                state.points.size(),
                state.distanceMeters,
                durationSeconds,
                averageSpeedKmh,
                state.maxSpeedKmh);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record StartRideRequest(
            @NotNull UUID userId,
            UUID teamId,
            Instant startedAt) {
    }

    public record PointBatch(@NotEmpty List<@Valid TrackPoint> points) {
    }

    public record TrackPoint(
            @Min(0) long sequence,
            @NotNull Instant capturedAt,
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @Min(0) @Max(500) double speedKmh,
            @Min(0) @Max(5000) double accuracyMeters) {
    }

    public record RideView(
            UUID id,
            UUID userId,
            UUID teamId,
            Instant startedAt,
            Instant endedAt,
            int pointCount,
            double distanceMeters,
            long durationSeconds,
            double averageSpeedKmh,
            double maxSpeedKmh) {
    }

    private static final class RideState {
        private final UUID id;
        private final UUID userId;
        private final UUID teamId;
        private final Instant startedAt;
        private final List<TrackPoint> points = new ArrayList<>();
        private Instant endedAt;
        private double distanceMeters;
        private double maxSpeedKmh;

        private RideState(UUID id, UUID userId, UUID teamId, Instant startedAt) {
            this.id = id;
            this.userId = userId;
            this.teamId = teamId;
            this.startedAt = startedAt;
        }
    }
}
