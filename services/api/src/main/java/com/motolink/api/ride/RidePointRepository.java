package com.motolink.api.ride;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RidePointRepository extends JpaRepository<RidePoint, UUID> {
    Optional<RidePoint> findTopByRide_IdOrderByRecordedAtDesc(UUID rideId);
    long countByRide_Id(UUID rideId);
}
