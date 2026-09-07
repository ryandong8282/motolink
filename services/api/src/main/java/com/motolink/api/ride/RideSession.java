package com.motolink.api.ride;

import com.motolink.api.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_session")
public class RideSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RideStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "distance_meters", nullable = false)
    private double distanceMeters;

    @Column(name = "max_speed_kmh", nullable = false)
    private double maxSpeedKmh;

    protected RideSession() {}

    public RideSession(AppUser user) {
        this.user = user;
        this.status = RideStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        startedAt = Instant.now();
    }

    public void addDistance(double meters) {
        if (meters >= 0 && meters < 10_000) distanceMeters += meters;
    }

    public void observeSpeed(double speedKmh) {
        if (speedKmh >= 0 && speedKmh < 400) maxSpeedKmh = Math.max(maxSpeedKmh, speedKmh);
    }

    public void finish() {
        if (status == RideStatus.ACTIVE) {
            status = RideStatus.FINISHED;
            endedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public RideStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getMaxSpeedKmh() { return maxSpeedKmh; }
}
