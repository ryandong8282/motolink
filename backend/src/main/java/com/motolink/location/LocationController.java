package com.motolink.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(30);
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final ConcurrentMap<UUID, Position> positions = new ConcurrentHashMap<>();

    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateLocationRequest request) {
        positions.put(userId, new Position(
                userId,
                request.latitude(),
                request.longitude(),
                request.speedKmh(),
                request.accuracyMeters(),
                request.capturedAt() == null ? Instant.now() : request.capturedAt(),
                Instant.now()));
    }

    @GetMapping("/nearby")
    public List<NearbyRider> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "3000") @Min(100) @Max(10000) int radiusMeters,
            @RequestParam(required = false) UUID excludeUserId) {
        Instant cutoff = Instant.now().minus(ONLINE_TTL);
        return positions.values().stream()
                .filter(position -> position.observedAt().isAfter(cutoff))
                .filter(position -> excludeUserId == null || !position.userId().equals(excludeUserId))
                .map(position -> new NearbyRider(
                        position.userId(),
                        distanceMeters(latitude, longitude, position.latitude(), position.longitude()),
                        position.speedKmh(),
                        position.observedAt()))
                .filter(rider -> rider.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(NearbyRider::distanceMeters))
                .toList();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record UpdateLocationRequest(
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @Min(0) @Max(500) double speedKmh,
            @Min(0) @Max(5000) double accuracyMeters,
            Instant capturedAt) {
    }

    private record Position(
            UUID userId,
            double latitude,
            double longitude,
            double speedKmh,
            double accuracyMeters,
            Instant capturedAt,
            Instant observedAt) {
    }

    public record NearbyRider(
            UUID userId,
            double distanceMeters,
            double speedKmh,
            Instant observedAt) {
    }
}
