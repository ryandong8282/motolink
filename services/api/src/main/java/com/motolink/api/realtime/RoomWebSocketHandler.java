package com.motolink.api.realtime;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = roomId(session);
        rooms.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        broadcast(roomId, new TextMessage("{\"type\":\"member_connected\",\"sessionId\":\""
                + session.getId() + "\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        broadcast(roomId(session), message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = roomId(session);
        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) rooms.remove(roomId);
        }
    }

    private void broadcast(String roomId, TextMessage message) {
        Set<WebSocketSession> sessions = rooms.getOrDefault(roomId, Set.of());
        for (WebSocketSession peer : sessions) {
            if (!peer.isOpen()) continue;
            try {
                peer.sendMessage(message);
            } catch (Exception ignored) {
                // 下一次连接状态变化时清理。
            }
        }
    }

    private static String roomId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return "unknown";
        String value = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("roomId");
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
