package com.motolink.api.realtime;

import com.motolink.api.auth.SessionService;
import com.motolink.api.common.ApiException;
import com.motolink.api.team.TeamService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TeamHandshakeInterceptor implements HandshakeInterceptor {

    static final String TEAM_ID = "teamId";
    static final String USER_ID = "userId";
    static final String NICKNAME = "nickname";

    private final SessionService sessionService;
    private final TeamService teamService;

    public TeamHandshakeInterceptor(SessionService sessionService, TeamService teamService) {
        this.sessionService = sessionService;
        this.teamService = teamService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        try {
            String teamId = extractTeamId(request.getURI().getPath());
            String token = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("token");
            SessionService.Session session = sessionService.require(token);
            teamService.requireMember(teamId, session.userId());
            attributes.put(TEAM_ID, teamId);
            attributes.put(USER_ID, session.userId());
            attributes.put(NICKNAME, session.nickname());
            return true;
        } catch (ApiException exception) {
            response.setStatusCode(exception.status());
            return false;
        } catch (RuntimeException exception) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op.
    }

    private String extractTeamId(String path) {
        String[] segments = path.split("/");
        if (segments.length == 0 || segments[segments.length - 1].isBlank()) {
            throw new IllegalArgumentException("missing teamId");
        }
        return segments[segments.length - 1];
    }
}
