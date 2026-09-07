package com.motolink.api.ride;

import com.motolink.api.common.DomainException;
import com.motolink.api.nearby.LocationService;
import com.motolink.api.user.AppUser;
import com.motolink.api.user.UserService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private final RideSessionRepository rides;
    private final RidePointRepository points;
    private final UserService users;

    public RideService(
            RideSessionRepository rides,
            RidePointRepository points,
            UserService users) {
        this.rides = rides;
        this.points = points;
        this.users = users;
    }

    @Transactional
    public RideResponse start(UUID userId) {
        AppUser user = users.require(userId);
        return toResponse(rides.save(new RideSession(user)));
    }

    @Transactional
    public RideResponse append(UUID rideId, UUID userId, AddPointRequest request) {
        RideSession ride = requireOwnedRide(rideId, userId);
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new DomainException(HttpStatus.CONFLICT, "骑行记录已经结束");
        }

        points.findTopByRide_IdOrderByRecordedAtDesc(rideId).ifPresent(previous -> {
            double distance = LocationService.haversineMeters(
                    previous.getLatitude(),
                    previous.getLongitude(),
                    request.latitude(),
                    request.longitude());
            ride.addDistance(distance);
        });
        ride.observeSpeed(request.speedKmh());
        points.save(new RidePoint(
                ride,
                request.latitude(),
                request.longitude(),
                request.speedKmh(),
                request.recordedAt() == null ? Instant.now() : request.recordedAt()));
        return toResponse(ride);
    }

    @Transactional
    public RideResponse finish(UUID rideId, UUID userId) {
        RideSession ride = requireOwnedRide(rideId, userId);
        ride.finish();
        return toResponse(ride);
    }

    private RideSession requireOwnedRide(UUID rideId, UUID userId) {
        RideSession ride = rides.findById(rideId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "骑行记录不存在"));
        if (!ride.getUser().getId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "无权访问该骑行记录");
        }
        return ride;
    }

    private RideResponse toResponse(RideSession ride) {
        return new RideResponse(
                ride.getId(),
                ride.getStatus().name(),
                ride.getStartedAt(),
                ride.getEndedAt(),
                ride.getDistanceMeters(),
                ride.getMaxSpeedKmh(),
                points.countByRide_Id(ride.getId()));
    }

    public record AddPointRequest(
            @DecimalMin("-90") @DecimalMax("90") double latitude,
            @DecimalMin("-180") @DecimalMax("180") double longitude,
            @PositiveOrZero double speedKmh,
            Instant recordedAt) {}

    public record RideResponse(
            UUID id,
            String status,
            Instant startedAt,
            Instant endedAt,
            double distanceMeters,
            double maxSpeedKmh,
            long pointCount) {}
}
