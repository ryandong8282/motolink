package com.motolink.team;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeamService {

    private final ConcurrentMap<UUID, TeamState> teams = new ConcurrentHashMap<>();

    public TeamService() {
        UUID ownerId = UUID.nameUUIDFromBytes("demo-captain".getBytes());
        TeamState demo = new TeamState(
                UUID.nameUUIDFromBytes("demo-team".getBytes()),
                "520131",
                "周末环湖小队",
                ownerId,
                "城北集合，沿湖骑行约 45km",
                12,
                false,
                Instant.now());
        demo.memberIds.add(ownerId);
        teams.put(demo.id, demo);
    }

    public List<TeamView> list() {
        return teams.values().stream()
                .sorted(Comparator.comparing(TeamState::createdAt).reversed())
                .map(this::toView)
                .toList();
    }

    public TeamView get(UUID teamId) {
        return toView(requireTeam(teamId));
    }

    public TeamView create(CreateTeam command) {
        String roomCode = nextRoomCode();
        TeamState state = new TeamState(
                UUID.randomUUID(),
                roomCode,
                command.name().trim(),
                command.ownerId(),
                command.routeNote() == null ? "" : command.routeNote().trim(),
                command.capacity(),
                command.privateRoom(),
                Instant.now());
        state.memberIds.add(command.ownerId());
        teams.put(state.id, state);
        return toView(state);
    }

    public TeamView join(String roomCode, UUID userId) {
        TeamState state = teams.values().stream()
                .filter(team -> team.roomCode.equals(roomCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Room code does not exist"));

        synchronized (state) {
            if (!state.memberIds.contains(userId) && state.memberIds.size() >= state.capacity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Team is full");
            }
            state.memberIds.add(userId);
        }
        return toView(state);
    }

    public boolean isMember(UUID teamId, UUID userId) {
        return requireTeam(teamId).memberIds.contains(userId);
    }

    private TeamState requireTeam(UUID teamId) {
        TeamState state = teams.get(teamId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team does not exist");
        }
        return state;
    }

    private String nextRoomCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            boolean exists = teams.values().stream().anyMatch(team -> team.roomCode.equals(code));
            if (!exists) {
                return code;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Unable to allocate room code");
    }

    private TeamView toView(TeamState state) {
        return new TeamView(
                state.id,
                state.roomCode,
                state.name,
                state.ownerId,
                state.routeNote,
                state.capacity,
                state.privateRoom,
                state.memberIds.size(),
                Set.copyOf(state.memberIds),
                state.createdAt);
    }

    public record CreateTeam(
            String name,
            UUID ownerId,
            String routeNote,
            int capacity,
            boolean privateRoom) {
    }

    public record TeamView(
            UUID id,
            String roomCode,
            String name,
            UUID ownerId,
            String routeNote,
            int capacity,
            boolean privateRoom,
            int memberCount,
            Set<UUID> memberIds,
            Instant createdAt) {
    }

    private static final class TeamState {
        private final UUID id;
        private final String roomCode;
        private final String name;
        private final UUID ownerId;
        private final String routeNote;
        private final int capacity;
        private final boolean privateRoom;
        private final Instant createdAt;
        private final Set<UUID> memberIds = new LinkedHashSet<>();

        private TeamState(
                UUID id,
                String roomCode,
                String name,
                UUID ownerId,
                String routeNote,
                int capacity,
                boolean privateRoom,
                Instant createdAt) {
            this.id = id;
            this.roomCode = roomCode;
            this.name = name;
            this.ownerId = ownerId;
            this.routeNote = routeNote;
            this.capacity = capacity;
            this.privateRoom = privateRoom;
            this.createdAt = createdAt;
        }

        private Instant createdAt() {
            return createdAt;
        }
    }
}
