package com.velsis.monitoring.internet.websocket;

import com.velsis.monitoring.internet.service.LatencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class InternetWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(InternetWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = new HashSet<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.sessions.add(session);
    }

    @Scheduled(fixedDelayString = "${monitoring.disc.interval-ms:1000}")
    private void measure() throws IOException {
        for (WebSocketSession activeSession : sessions) {
            if (activeSession.isOpen()) {
                activeSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(new LatencyService().measure())));
            }else {
                disconnect(activeSession);
            }
        }
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
