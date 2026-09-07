package com.motolink.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String phone;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 120)
    private String motorcycle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {}

    public AppUser(String phone, String nickname) {
        this.phone = phone;
        this.nickname = nickname;
        this.motorcycle = "未填写座驾";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getNickname() { return nickname; }
    public String getMotorcycle() { return motorcycle; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateProfile(String nickname, String motorcycle) {
        this.nickname = nickname;
        if (motorcycle != null && !motorcycle.isBlank()) {
            this.motorcycle = motorcycle;
        }
    }
}
