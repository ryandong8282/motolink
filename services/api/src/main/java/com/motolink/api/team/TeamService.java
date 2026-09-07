package com.motolink.api.team;

import com.motolink.api.common.ApiException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private static final int ROOM_CODE_BOUND = 1_000_000;

    private final ConcurrentMap<String, MutableTeam> teams = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> teamIdByRoomCode = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TeamService(Clock clock) {
        this.clock = clock;
    }

    public TeamView create(
            String leaderId,
            String leaderNickname,
            String name,
            String routeNote,
            int maxMembers,
            boolean publicRoom) {
        String teamId = "team_" + compactUuid().substring(0, 12);
        String roomCode = reserveRoomCode(teamId);
        Instant now = Instant.now(clock);
        MutableTeam team = new MutableTeam(
                teamId,
                roomCode,
                name.trim(),
                routeNote == null ? "" : routeNote.trim(),
                maxMembers,
                publicRoom,
                leaderId,
                now);
        team.members.put(leaderId, new Member(leaderId, leaderNickname, now));
        teams.put(teamId, team);
        return toView(team);
    }

    public TeamView joinByRoomCode(String userId, String nickname, String roomCode) {
        String normalizedCode = roomCode == null ? "" : roomCode.trim();
        String teamId = teamIdByRoomCode.get(normalizedCode);
        if (teamId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "没有找到该房间码");
        }
        MutableTeam team = requireTeam(teamId);
        synchronized (team) {
            if (team.members.containsKey(userId)) {
                return toView(team);
            }
            if (team.members.size() >= team.maxMembers) {
                throw new ApiException(HttpStatus.CONFLICT, "TEAM_FULL", "车队人数已满");
            }
            team.members.put(userId, new Member(userId, nickname, Instant.now(clock)));
            return toView(team);
        }
    }

    public TeamView getForMember(String teamId, String userId) {
        MutableTeam team = requireTeam(teamId);
        synchronized (team) {
            requireMembership(team, userId);
            return toView(team);
        }
    }

    public void requireMember(String teamId, String userId) {
        MutableTeam team = requireTeam(teamId);
        synchronized (team) {
            requireMembership(team, userId);
        }
    }

    public List<TeamView> listForUser(String userId) {
        List<TeamView> result = new ArrayList<>();
        for (MutableTeam team : teams.values()) {
            synchronized (team) {
                if (team.members.containsKey(userId)) {
                    result.add(toView(team));
                }
            }
        }
        result.sort(Comparator.comparing(TeamView::createdAt).reversed());
        return List.copyOf(result);
    }

    public LeaveResult leave(String teamId, String userId) {
        MutableTeam team = requireTeam(teamId);
        synchronized (team) {
            requireMembership(team, userId);
            if (team.leaderId.equals(userId)) {
                teams.remove(team.id);
                teamIdByRoomCode.remove(team.roomCode);
                return new LeaveResult(true);
            }
            team.members.remove(userId);
            return new LeaveResult(false);
        }
    }

    private MutableTeam requireTeam(String teamId) {
        MutableTeam team = teams.get(teamId);
        if (team == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "车队不存在或已解散");
        }
        return team;
    }

    private void requireMembership(MutableTeam team, String userId) {
        if (!team.members.containsKey(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_TEAM_MEMBER", "你还不是该车队成员");
        }
    }

    private TeamView toView(MutableTeam team) {
        List<MemberView> members = new ArrayList<>();
        for (Member member : team.members.values()) {
            members.add(new MemberView(
                    member.userId,
                    member.nickname,
                    member.joinedAt,
                    member.userId.equals(team.leaderId)));
        }
        members.sort(Comparator.comparing(MemberView::joinedAt));
        return new TeamView(
                team.id,
                team.roomCode,
                team.name,
                team.routeNote,
                team.maxMembers,
                team.publicRoom,
                team.leaderId,
                team.createdAt,
                List.copyOf(members));
    }

    private String reserveRoomCode(String teamId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("%06d", random.nextInt(ROOM_CODE_BOUND));
            if (teamIdByRoomCode.putIfAbsent(code, teamId) == null) {
                return code;
            }
        }
        throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ROOM_CODE_EXHAUSTED",
                "暂时无法分配房间码，请稍后重试");
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final class MutableTeam {
        private final String id;
        private final String roomCode;
        private final String name;
        private final String routeNote;
        private final int maxMembers;
        private final boolean publicRoom;
        private final Instant createdAt;
        private final Map<String, Member> members = new LinkedHashMap<>();
        private String leaderId;

        private MutableTeam(
                String id,
                String roomCode,
                String name,
                String routeNote,
                int maxMembers,
                boolean publicRoom,
                String leaderId,
                Instant createdAt) {
            this.id = id;
            this.roomCode = roomCode;
            this.name = name;
            this.routeNote = routeNote;
            this.maxMembers = maxMembers;
            this.publicRoom = publicRoom;
            this.leaderId = leaderId;
            this.createdAt = createdAt;
        }
    }

    private record Member(String userId, String nickname, Instant joinedAt) {
    }

    public record TeamView(
            String id,
            String roomCode,
            String name,
            String routeNote,
            int maxMembers,
            boolean publicRoom,
            String leaderId,
            Instant createdAt,
            List<MemberView> members) {
    }

    public record MemberView(
            String userId,
            String nickname,
            Instant joinedAt,
            boolean leader) {
    }

    public record LeaveResult(boolean teamDissolved) {
    }
}
