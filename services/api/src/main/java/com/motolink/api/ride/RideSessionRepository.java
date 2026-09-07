package com.motolink.api.ride;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideSessionRepository extends JpaRepository<RideSession, UUID> {
    long countByStatus(RideStatus status);
}
