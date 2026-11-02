package com.velsis.monitoring.hardware.disc.config;

import com.velsis.monitoring.hardware.disc.websocket.DiscWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class DiscWebSocketConfig implements WebSocketConfigurer {
    private final DiscWebSocketHandler handler;
    private final String[] allowedOrigins;

    public DiscWebSocketConfig(DiscWebSocketHandler handler,
            @Value("${monitoring.disc.allowed-origins:http://localhost:5173,http://192.168.15.4:5173}") String[] allowedOrigins) {
        this.handler = handler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/monitor/disc").setAllowedOrigins(allowedOrigins);
    }
}
