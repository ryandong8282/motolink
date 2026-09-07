package com.motolink.api.room;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ride_room")
public class RideRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_code", nullable = false, unique = true, length = 6)
    private String roomCode;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "public_room", nullable = false)
    private boolean publicRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoomStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RoomMember> members = new ArrayList<>();

    protected RideRoom() {}

    public RideRoom(String roomCode, String name, int maxMembers, boolean publicRoom) {
        this.roomCode = roomCode;
        this.name = name;
        this.maxMembers = maxMembers;
        this.publicRoom = publicRoom;
        this.status = RoomStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addMember(RoomMember member) {
        members.add(member);
        member.attachTo(this);
    }

    public UUID getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public String getName() { return name; }
    public int getMaxMembers() { return maxMembers; }
    public boolean isPublicRoom() { return publicRoom; }
    public RoomStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<RoomMember> getMembers() { return members; }
}
