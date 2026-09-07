package com.motolink.api.realtime;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.motolink.api.common.ApiException;
import com.motolink.api.location.LocationService;
import com.motolink.api.team.TeamService;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TeamSocketHandler extends TextWebSocketHandler {

    private final ConcurrentMap<String, ConcurrentMap<String, WebSocketSession>> sessionsByTeam =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final TeamService teamService;
    private final LocationService locationService;
    private final FloorService floorService;
    private final Clock clock;

    public TeamSocketHandler(
            ObjectMapper objectMapper,
            TeamService teamService,
            LocationService locationService,
            FloorService floorService,
            Clock clock) {
        this.objectMapper = objectMapper;
        this.teamService = teamService;
        this.locationService = locationService;
        this.floorService = floorService;
        this.clock = clock;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String teamId = attribute(session, TeamHandshakeInterceptor.TEAM_ID);
        sessionsByTeam.computeIfAbsent(teamId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        send(session, event("SNAPSHOT", Map.of(
                "team", teamService.getForMember(teamId, userId(session)),
                "floor", nullable(floorService.current(teamId)),
                "locations", locationService.teamLocations(teamId))));
        broadcast(teamId, event("MEMBER_JOINED", Map.of(
                "userId", userId(session),
                "nickname", nickname(session))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String teamId = teamId(session);
            teamService.requireMember(teamId, userId(session));

            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            JsonNode payload = root.path("payload");
            switch (type) {
                case "PING" -> send(session, event("PONG", Map.of()));
                case "LOCATION_UPDATE" -> handleLocation(session, payload);
                case "FLOOR_REQUEST" -> handleFloorRequest(session);
                case "FLOOR_RELEASE" -> handleFloorRelease(session);
                default -> sendError(session, "UNSUPPORTED_EVENT", "不支持的事件类型: " + type);
            }
        } catch (ApiException exception) {
            sendError(session, exception.code(), exception.getMessage());
            close(session, CloseStatus.POLICY_VIOLATION);
        } catch (Exception exception) {
            sendError(session, "INVALID_MESSAGE", "消息格式不正确");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String teamId = attribute(session, TeamHandshakeInterceptor.TEAM_ID);
        ConcurrentMap<String, WebSocketSession> teamSessions = sessionsByTeam.get(teamId);
        if (teamSessions != null) {
            teamSessions.remove(session.getId());
            if (teamSessions.isEmpty()) {
                sessionsByTeam.remove(teamId, teamSessions);
            }
        }

        FloorService.FloorLease released = floorService.release(teamId, userId(session));
        if (released != null) {
            broadcast(teamId, event("FLOOR_RELEASED", Map.of(
                    "userId", released.userId(),
                    "reason", "socket_closed")));
        }
        broadcast(teamId, event("MEMBER_LEFT", Map.of(
                "userId", userId(session),
                "nickname", nickname(session))));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleLocation(WebSocketSession session, JsonNode payload) {
        double latitude = payload.path("latitude").asDouble(Double.NaN);
        double longitude = payload.path("longitude").asDouble(Double.NaN);
        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {
            sendError(session, "INVALID_LOCATION", "经纬度不合法");
            return;
        }

        LocationService.LocationUpdate update = new LocationService.LocationUpdate(
                latitude,
                longitude,
                nullableDouble(payload.get("accuracy")),
                nullableDouble(payload.get("speed")),
                nullableDouble(payload.get("bearing")));
        String teamId = teamId(session);
        LocationService.MemberLocation location = locationService.update(
                userId(session),
                nickname(session),
                update,
                teamId);
        broadcast(teamId, event("LOCATION_UPDATED", location));
    }

    private void handleFloorRequest(WebSocketSession session) {
        String teamId = teamId(session);
        FloorService.AcquireResult result = floorService.request(
                teamId,
                userId(session),
                nickname(session));
        if (result.granted()) {
            broadcast(teamId, event("FLOOR_GRANTED", result.lease()));
        } else {
            send(session, event("FLOOR_DENIED", Map.of(
                    "reason", result.reason(),
                    "holder", result.lease())));
        }
    }

    private void handleFloorRelease(WebSocketSession session) {
        String teamId = teamId(session);
        FloorService.FloorLease released = floorService.release(teamId, userId(session));
        if (released != null) {
            broadcast(teamId, event("FLOOR_RELEASED", Map.of(
                    "userId", released.userId(),
                    "reason", "client_release")));
        }
    }

    public void broadcastExpiredFloor(FloorService.FloorLease lease) {
        broadcast(lease.teamId(), event("FLOOR_RELEASED", Map.of(
                "userId", lease.userId(),
                "reason", "lease_expired")));
    }

    private ServerEvent event(String type, Object payload) {
        return new ServerEvent(type, Instant.now(clock), payload);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        send(session, event("ERROR", Map.of("code", code, "message", message)));
    }

    private void broadcast(String teamId, ServerEvent event) {
        ConcurrentMap<String, WebSocketSession> sessions = sessionsByTeam.get(teamId);
        if (sessions == null) {
            return;
        }
        sessions.values().forEach(session -> send(session, event));
    }

    private void send(WebSocketSession session, ServerEvent event) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException exception) {
            close(session, CloseStatus.SERVER_ERROR);
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
            // The connection is already broken.
        }
    }

    private Double nullableDouble(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private Object nullable(Object value) {
        return value == null ? Map.of() : value;
    }

    private String teamId(WebSocketSession session) {
        return attribute(session, TeamHandshakeInterceptor.TEAM_ID);
    }

    private String userId(WebSocketSession session) {
        return attribute(session, TeamHandshakeInterceptor.USER_ID);
    }

    private String nickname(WebSocketSession session) {
        return attribute(session, TeamHandshakeInterceptor.NICKNAME);
    }

    private String attribute(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        return value == null ? "" : value.toString();
    }

    private record ServerEvent(String type, Instant timestamp, Object payload) {
    }
}
