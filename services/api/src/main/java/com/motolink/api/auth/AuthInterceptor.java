package com.motolink.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "motolink.userId";
    public static final String NICKNAME_ATTRIBUTE = "motolink.nickname";

    private final SessionService sessionService;

    public AuthInterceptor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        String token = extractBearer(request.getHeader("Authorization"));
        SessionService.Session session = sessionService.require(token);
        request.setAttribute(USER_ID_ATTRIBUTE, session.userId());
        request.setAttribute(NICKNAME_ATTRIBUTE, session.nickname());
        return true;
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
