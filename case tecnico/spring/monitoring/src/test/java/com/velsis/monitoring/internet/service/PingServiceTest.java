package com.velsis.monitoring.internet.service;

import com.velsis.monitoring.internet.model.PingRecord;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PingServiceTest {
    @Test
    void returnsTenResultsInOrderIncludingFailures() {
        PingService service = spy(new PingService());
        var success = new PingRecord("8.8.8.8", PingRecord.Status.SUCCESS, 10, 1000);
        var failure = new PingRecord("8.8.8.8", PingRecord.Status.TIMEOUT, 5000, 2000);
        doReturn(success, failure, success).when(service).test();
        var results = service.testBatch();
        assertEquals(10, results.size());
        assertEquals(success, results.get(0));
        assertEquals(failure, results.get(1));
        assertEquals(success, results.get(9));
        verify(service, times(10)).test();
    }

    private Process response(String output, int exit) throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor(5, TimeUnit.SECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(exit);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
        return process;
    }

    @Test
    void detectsEchoReply() throws Exception {
        PingService service = spy(new PingService());
        doReturn(response("Reply from 8.8.8.8: bytes=32 time=10ms TTL=117", 0)).when(service).startPing();
        var result = service.test();
        assertEquals(PingRecord.Status.SUCCESS, result.status());
        assertEquals("8.8.8.8", result.host());
        assertTrue(result.durationMs() >= 0);
        assertTrue(result.timestamp() > 0);
    }

    @Test
    void doesNotTreatWindowsUnreachableExitZeroAsSuccess() throws Exception {
        PingService service = spy(new PingService());
        doReturn(response("Destination host unreachable", 0)).when(service).startPing();
        assertEquals(PingRecord.Status.NO_REPLY, service.test().status());
    }

    @Test
    void killsTimedOutProcess() throws Exception {
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        PingService service = spy(new PingService());
        doReturn(process).when(service).startPing();
        assertEquals(PingRecord.Status.TIMEOUT, service.test().status());
        verify(process).destroyForcibly();
    }

    @Test
    void reportsMissingPingCommand() throws Exception {
        PingService service = spy(new PingService());
        doThrow(new IOException("Missing ping")).when(service).startPing();
        assertEquals(PingRecord.Status.ERROR, service.test().status());
    }

    @Test
    void preservesInterruptionAndKillsProcess() throws Exception {
        Process process = mock(Process.class);
        when(process.waitFor(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        when(process.isAlive()).thenReturn(true);
        PingService service = spy(new PingService());
        doReturn(process).when(service).startPing();
        try {
            assertEquals(PingRecord.Status.ERROR, service.test().status());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(process).destroyForcibly();
        } finally {
            Thread.interrupted();
        }
    }
}
