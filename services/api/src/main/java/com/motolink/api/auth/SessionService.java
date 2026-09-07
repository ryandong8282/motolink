package com.motolink.api.auth;

import com.motolink.api.common.ApiException;
import com.motolink.api.config.MotoLinkProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final MotoLinkProperties properties;

    public SessionService(Clock clock, MotoLinkProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public Session create(String nickname) {
        String userId = "u_" + compactUuid().substring(0, 12);
        String token = "mvp_" + compactUuid();
        Instant expiresAt = Instant.now(clock).plus(properties.sessionTtl());
        Session session = new Session(token, userId, nickname.trim(), expiresAt);
        sessions.put(token, session);
        return session;
    }

    public Session require(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized("缺少登录凭证");
        }
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now(clock))) {
            if (session != null) {
                sessions.remove(token);
            }
            throw unauthorized("登录状态已失效，请重新登录");
        }
        return session;
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record Session(String token, String userId, String nickname, Instant expiresAt) {
    }
}
