package com.velsis.monitoring.internet.config;

import com.velsis.monitoring.internet.websocket.InternetWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class InternetWebSocketConfig implements WebSocketConfigurer {
    private InternetWebSocketHandler internetWebSocketHandler;

    public InternetWebSocketConfig(InternetWebSocketHandler internetWebSocketHandler){
        this.internetWebSocketHandler = internetWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(internetWebSocketHandler, "/ws/monitor/internet").setAllowedOrigins("*");
    }
}
