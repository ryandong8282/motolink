package com.motolink.api.room;

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
@Table(name = "room_member")
public class RoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RideRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoomRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected RoomMember() {}

    public RoomMember(AppUser user, RoomRole role) {
        this.user = user;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }

    void attachTo(RideRoom room) {
        this.room = room;
    }

    public UUID getId() { return id; }
    public RideRoom getRoom() { return room; }
    public AppUser getUser() { return user; }
    public RoomRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
