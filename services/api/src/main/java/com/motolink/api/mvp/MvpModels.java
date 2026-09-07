package com.motolink.api.mvp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MvpModels {

    private MvpModels() {
    }

    public record UserView(
        UUID id,
        String phone,
        String nickname,
        String avatarUrl,
        OffsetDateTime createdAt
    ) {
    }

    public record MemberView(
        UUID userId,
        String nickname,
        String role,
        OffsetDateTime joinedAt
    ) {
    }

    public record TeamView(
        UUID id,
        String roomCode,
        String name,
        UUID leaderId,
        int maxMembers,
        String status,
        OffsetDateTime createdAt,
        List<MemberView> members
    ) {
    }

    public record LocationView(
        UUID roomId,
        UUID userId,
        String nickname,
        double latitude,
        double longitude,
        Double speedMps,
        Double heading,
        OffsetDateTime recordedAt,
        OffsetDateTime receivedAt
    ) {
    }

    public record RideView(
        UUID id,
        UUID userId,
        UUID roomId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        double distanceMeters,
        double maxSpeedMps
    ) {
    }

    public record AdminOverview(
        long users,
        long openTeams,
        long totalTeams,
        long activeRides,
        long totalRides,
        long latestLocationRows
    ) {
    }
}
