package com.velsis.monitoring.hardware.disc.infra.websocket;

import com.velsis.monitoring.hardware.disc.model.DiscMeasurementRecord;
import com.velsis.monitoring.hardware.disc.service.InfoDiscService;
import com.velsis.monitoring.hardware.disc.websocket.DiscWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DiscWebSocketHandlerTests {
    private final InfoDiscService service = mock(InfoDiscService.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final DiscWebSocketHandler handler = new DiscWebSocketHandler(service, mapper);

    private WebSocketSession session(String id) {
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void sendsRecordOnConnectAndBroadcastAndRemovesClosedSession() throws Exception {
        var record = new DiscMeasurementRecord(BigInteger.valueOf(1000),
                BigInteger.valueOf(500), BigInteger.valueOf(123456));
        when(service.measure()).thenReturn(record);
        var session = session("one");
        handler.afterConnectionEstablished(session);
        handler.broadcast();
        var messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(2)).sendMessage(messages.capture());
        for (var message : messages.getAllValues()) {
            assertEquals(record, mapper.readValue(message.getPayload(), DiscMeasurementRecord.class));
        }
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.broadcast();
        verify(service, times(2)).measure();
    }

    @Test
    void failingClientDoesNotPreventDeliveryToOtherClients() throws Exception {
        when(service.measure()).thenReturn(new DiscMeasurementRecord(
                BigInteger.TEN, BigInteger.ONE, BigInteger.ZERO));
        var failed = session("failed");
        var healthy = session("healthy");
        handler.afterConnectionEstablished(failed);
        handler.afterConnectionEstablished(healthy);
        doThrow(new IOException("Disconnected")).when(failed).sendMessage(any());
        handler.broadcast();
        handler.broadcast();
        verify(failed).close(CloseStatus.SERVER_ERROR);
        verify(healthy, times(3)).sendMessage(any());
        verify(failed, times(2)).sendMessage(any());
    }

    @Test
    void collectionFailureCanRecoverOnNextBroadcast() throws Exception {
        var record = new DiscMeasurementRecord(BigInteger.TEN, BigInteger.ONE, BigInteger.ZERO);
        when(service.measure()).thenReturn(record).thenThrow(new IOException("Unavailable")).thenReturn(record);
        var session = session("one");
        handler.afterConnectionEstablished(session);
        handler.broadcast();
        handler.broadcast();
        verify(session, times(2)).sendMessage(any());
        verify(session, never()).close(any());
    }
}
