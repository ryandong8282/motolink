package com.motolink.api.config;

import com.motolink.api.realtime.TeamHandshakeInterceptor;
import com.motolink.api.realtime.TeamSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TeamSocketHandler teamSocketHandler;
    private final TeamHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(
            TeamSocketHandler teamSocketHandler,
            TeamHandshakeInterceptor handshakeInterceptor) {
        this.teamSocketHandler = teamSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(teamSocketHandler, "/ws/teams/{teamId}")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
