package com.motolink.api.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TeamSocketHandler teamSocketHandler;

    public WebSocketConfig(TeamSocketHandler teamSocketHandler) {
        this.teamSocketHandler = teamSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(teamSocketHandler, "/ws/teams")
            .setAllowedOriginPatterns("*");
    }
}
