package com.velsis.monitoring.hardware.disc.websocket;

import com.velsis.monitoring.hardware.disc.service.InfoDiscService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(DiscWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final InfoDiscService service;
    private final ObjectMapper mapper;

    public DiscWebSocketHandler(InfoDiscService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        var safeSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 65536);
        try {
            safeSession.sendMessage(measurement());
            sessions.put(session.getId(), safeSession);
        } catch (Exception exception) {
            disconnect(safeSession);
            log.warn("Unable to send initial disc measurement", exception);
        }
    }

    @Scheduled(fixedDelayString = "${monitoring.disc.interval-ms:1000}")
    public void broadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        final TextMessage message;
        try {
            message = measurement();
        } catch (Exception exception) {
            log.warn("Unable to read disc measurement", exception);
            return;
        }
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                } else {
                    sessions.remove(session.getId());
                }
            } catch (Exception exception) {
                disconnect(session);
                log.debug("Unable to send disc measurement", exception);
            }
        });
    }

    private TextMessage measurement() throws IOException {
        return new TextMessage(mapper.writeValueAsString(service.measure()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        disconnect(session);
    }

    private void disconnect(WebSocketSession session) {
        sessions.remove(session.getId());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException exception) {
            log.debug("Unable to close disc WebSocket", exception);
        }
    }
}
