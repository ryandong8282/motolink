package com.motolink.api.mvp;

import static com.motolink.api.mvp.MvpModels.AdminOverview;
import static com.motolink.api.mvp.MvpModels.LocationView;
import static com.motolink.api.mvp.MvpModels.MemberView;
import static com.motolink.api.mvp.MvpModels.RideView;
import static com.motolink.api.mvp.MvpModels.TeamView;
import static com.motolink.api.mvp.MvpModels.UserView;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class MvpRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public MvpRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserView upsertUser(String phone, String nickname) {
        String sql = """
            INSERT INTO user_account (id, phone, nickname)
            VALUES (:id, :phone, :nickname)
            ON CONFLICT (phone) DO UPDATE
            SET nickname = EXCLUDED.nickname, updated_at = NOW()
            RETURNING id, phone, nickname, avatar_url, created_at
            """;
        return jdbc.queryForObject(sql, Map.of(
            "id", UUID.randomUUID(),
            "phone", phone,
            "nickname", nickname
        ), USER_MAPPER);
    }

    @Transactional
    public TeamView createTeam(UUID leaderId, String name, int maxMembers) {
        requireUser(leaderId);
        UUID roomId = UUID.randomUUID();
        String roomCode = "%06d".formatted(random.nextInt(1_000_000));

        jdbc.update("""
            INSERT INTO team_room (id, room_code, name, leader_id, max_members)
            VALUES (:id, :roomCode, :name, :leaderId, :maxMembers)
            """, Map.of(
            "id", roomId,
            "roomCode", roomCode,
            "name", name,
            "leaderId", leaderId,
            "maxMembers", maxMembers
        ));
        jdbc.update("""
            INSERT INTO team_member (room_id, user_id, role)
            VALUES (:roomId, :userId, 'LEADER')
            """, Map.of("roomId", roomId, "userId", leaderId));
        return getTeam(roomId);
    }

    @Transactional
    public TeamView joinTeam(UUID userId, String roomCode) {
        requireUser(userId);
        TeamBase team = findTeamBaseByCode(roomCode);
        if (!"OPEN".equals(team.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team is closed");
        }

        Long currentMembers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM team_member WHERE room_id = :roomId",
            Map.of("roomId", team.id()),
            Long.class
        );
        if (currentMembers != null && currentMembers >= team.maxMembers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team is full");
        }

        jdbc.update("""
            INSERT INTO team_member (room_id, user_id, role)
            VALUES (:roomId, :userId, 'MEMBER')
            ON CONFLICT (room_id, user_id) DO NOTHING
            """, Map.of("roomId", team.id(), "userId", userId));
        return getTeam(team.id());
    }

    public TeamView getTeam(UUID roomId) {
        TeamBase team = findTeamBase(roomId);
        List<MemberView> members = jdbc.query("""
            SELECT m.user_id, u.nickname, m.role, m.joined_at
            FROM team_member m
            JOIN user_account u ON u.id = m.user_id
            WHERE m.room_id = :roomId
            ORDER BY CASE WHEN m.role = 'LEADER' THEN 0 ELSE 1 END, m.joined_at
            """, Map.of("roomId", roomId), MEMBER_MAPPER);
        return new TeamView(
            team.id(),
            team.roomCode(),
            team.name(),
            team.leaderId(),
            team.maxMembers(),
            team.status(),
            team.createdAt(),
            members
        );
    }

    public void requireMembership(UUID roomId, UUID userId) {
        Boolean member = jdbc.queryForObject("""
            SELECT EXISTS(
                SELECT 1 FROM team_member WHERE room_id = :roomId AND user_id = :userId
            )
            """, Map.of("roomId", roomId, "userId", userId), Boolean.class);
        if (!Boolean.TRUE.equals(member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this team");
        }
    }

    public void upsertLocation(
        UUID roomId,
        UUID userId,
        double latitude,
        double longitude,
        Double speedMps,
        Double heading,
        OffsetDateTime recordedAt
    ) {
        requireMembership(roomId, userId);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("roomId", roomId)
            .addValue("userId", userId)
            .addValue("latitude", latitude)
            .addValue("longitude", longitude)
            .addValue("speedMps", speedMps)
            .addValue("heading", heading)
            .addValue("recordedAt", recordedAt);
        jdbc.update("""
            INSERT INTO latest_location (
                room_id, user_id, latitude, longitude, speed_mps, heading, recorded_at
            ) VALUES (
                :roomId, :userId, :latitude, :longitude, :speedMps, :heading, :recordedAt
            )
            ON CONFLICT (room_id, user_id) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                speed_mps = EXCLUDED.speed_mps,
                heading = EXCLUDED.heading,
                recorded_at = EXCLUDED.recorded_at,
                received_at = NOW()
            WHERE EXCLUDED.recorded_at >= latest_location.recorded_at
            """, params);
    }

    public List<LocationView> listLocations(UUID roomId, UUID requesterId) {
        requireMembership(roomId, requesterId);
        return jdbc.query("""
            SELECT l.room_id, l.user_id, u.nickname, l.latitude, l.longitude,
                   l.speed_mps, l.heading, l.recorded_at, l.received_at
            FROM latest_location l
            JOIN user_account u ON u.id = l.user_id
            WHERE l.room_id = :roomId
              AND l.received_at >= NOW() - INTERVAL '90 seconds'
            ORDER BY u.nickname
            """, Map.of("roomId", roomId), LOCATION_MAPPER);
    }

    public RideView startRide(UUID userId, UUID roomId) {
        requireUser(userId);
        if (roomId != null) {
            requireMembership(roomId, userId);
        }
        UUID rideId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO ride_session (id, user_id, room_id)
            VALUES (:id, :userId, :roomId)
            """, new MapSqlParameterSource()
            .addValue("id", rideId)
            .addValue("userId", userId)
            .addValue("roomId", roomId));
        return getRide(rideId);
    }

    public int appendTrackPoints(UUID rideId, List<TrackPointInput> points) {
        RideView ride = getRide(rideId);
        if (!"ACTIVE".equals(ride.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride is not active");
        }
        if (points.isEmpty()) {
            return 0;
        }

        SqlParameterSource[] batch = points.stream()
            .map(point -> new MapSqlParameterSource()
                .addValue("rideId", rideId)
                .addValue("sequenceNo", point.sequenceNo())
                .addValue("latitude", point.latitude())
                .addValue("longitude", point.longitude())
                .addValue("speedMps", point.speedMps())
                .addValue("recordedAt", point.recordedAt()))
            .toArray(SqlParameterSource[]::new);

        int[] results = jdbc.batchUpdate("""
            INSERT INTO ride_track_point (
                ride_id, sequence_no, latitude, longitude, speed_mps, recorded_at
            ) VALUES (
                :rideId, :sequenceNo, :latitude, :longitude, :speedMps, :recordedAt
            )
            ON CONFLICT (ride_id, sequence_no) DO UPDATE SET
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                speed_mps = EXCLUDED.speed_mps,
                recorded_at = EXCLUDED.recorded_at
            """, batch);
        return results.length;
    }

    public RideView finishRide(UUID rideId, double distanceMeters, double maxSpeedMps) {
        int changed = jdbc.update("""
            UPDATE ride_session
            SET status = 'FINISHED', finished_at = NOW(),
                distance_meters = :distanceMeters, max_speed_mps = :maxSpeedMps
            WHERE id = :rideId AND status = 'ACTIVE'
            """, Map.of(
            "rideId", rideId,
            "distanceMeters", distanceMeters,
            "maxSpeedMps", maxSpeedMps
        ));
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride is already finished or missing");
        }
        return getRide(rideId);
    }

    public RideView getRide(UUID rideId) {
        try {
            return jdbc.queryForObject("""
                SELECT id, user_id, room_id, status, started_at, finished_at,
                       distance_meters, max_speed_mps
                FROM ride_session
                WHERE id = :rideId
                """, Map.of("rideId", rideId), RIDE_MAPPER);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
        }
    }

    public AdminOverview overview() {
        return new AdminOverview(
            count("SELECT COUNT(*) FROM user_account"),
            count("SELECT COUNT(*) FROM team_room WHERE status = 'OPEN'"),
            count("SELECT COUNT(*) FROM team_room"),
            count("SELECT COUNT(*) FROM ride_session WHERE status = 'ACTIVE'"),
            count("SELECT COUNT(*) FROM ride_session"),
            count("SELECT COUNT(*) FROM latest_location WHERE received_at >= NOW() - INTERVAL '90 seconds'")
        );
    }

    private long count(String sql) {
        Long value = jdbc.getJdbcTemplate().queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private void requireUser(UUID userId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM user_account WHERE id = :userId)",
            Map.of("userId", userId),
            Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private TeamBase findTeamBase(UUID roomId) {
        try {
            return jdbc.queryForObject("""
                SELECT id, room_code, name, leader_id, max_members, status, created_at
                FROM team_room WHERE id = :roomId
                """, Map.of("roomId", roomId), TEAM_BASE_MAPPER);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found");
        }
    }

    private TeamBase findTeamBaseByCode(String roomCode) {
        try {
            return jdbc.queryForObject("""
                SELECT id, room_code, name, leader_id, max_members, status, created_at
                FROM team_room WHERE room_code = :roomCode
                """, Map.of("roomCode", roomCode), TEAM_BASE_MAPPER);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team room code not found");
        }
    }

    private record TeamBase(
        UUID id,
        String roomCode,
        String name,
        UUID leaderId,
        int maxMembers,
        String status,
        OffsetDateTime createdAt
    ) {
    }

    public record TrackPointInput(
        long sequenceNo,
        double latitude,
        double longitude,
        Double speedMps,
        OffsetDateTime recordedAt
    ) {
    }

    private static final RowMapper<UserView> USER_MAPPER = (rs, rowNum) -> new UserView(
        rs.getObject("id", UUID.class),
        rs.getString("phone"),
        rs.getString("nickname"),
        rs.getString("avatar_url"),
        rs.getObject("created_at", OffsetDateTime.class)
    );

    private static final RowMapper<TeamBase> TEAM_BASE_MAPPER = (rs, rowNum) -> new TeamBase(
        rs.getObject("id", UUID.class),
        rs.getString("room_code"),
        rs.getString("name"),
        rs.getObject("leader_id", UUID.class),
        rs.getInt("max_members"),
        rs.getString("status"),
        rs.getObject("created_at", OffsetDateTime.class)
    );

    private static final RowMapper<MemberView> MEMBER_MAPPER = (rs, rowNum) -> new MemberView(
        rs.getObject("user_id", UUID.class),
        rs.getString("nickname"),
        rs.getString("role"),
        rs.getObject("joined_at", OffsetDateTime.class)
    );

    private static final RowMapper<LocationView> LOCATION_MAPPER = (rs, rowNum) -> new LocationView(
        rs.getObject("room_id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getString("nickname"),
        rs.getDouble("latitude"),
        rs.getDouble("longitude"),
        nullableDouble(rs, "speed_mps"),
        nullableDouble(rs, "heading"),
        rs.getObject("recorded_at", OffsetDateTime.class),
        rs.getObject("received_at", OffsetDateTime.class)
    );

    private static final RowMapper<RideView> RIDE_MAPPER = (rs, rowNum) -> new RideView(
        rs.getObject("id", UUID.class),
        rs.getObject("user_id", UUID.class),
        rs.getObject("room_id", UUID.class),
        rs.getString("status"),
        rs.getObject("started_at", OffsetDateTime.class),
        rs.getObject("finished_at", OffsetDateTime.class),
        rs.getDouble("distance_meters"),
        rs.getDouble("max_speed_mps")
    );

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
