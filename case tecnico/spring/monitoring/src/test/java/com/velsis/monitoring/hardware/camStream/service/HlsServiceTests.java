package com.velsis.monitoring.hardware.camStream.service;

import com.velsis.monitoring.hardware.camStream.config.StreamProperties;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.repository.StreamCameraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HlsServiceTests {
    @TempDir Path directory;
    StreamCameraRepository repository;
    FfmpegLauncher launcher;
    HlsService service;
    Process first;
    Process second;

    @BeforeEach
    void setup() throws Exception {
        repository = mock(StreamCameraRepository.class);
        launcher = mock(FfmpegLauncher.class);
        service = new HlsService(new StreamProperties("ffmpeg", directory.toString()), repository, launcher);
        when(repository.findByName("Entrada")).thenReturn(Optional.of(
                new StreamCamera(1, "Entrada", "192.168.1.10", 554, "admin@", "p:a/ss #?", true)));
        when(repository.findByName("Garagem")).thenReturn(Optional.of(
                new StreamCamera(2, "Garagem", "192.168.1.11", 8554, "user", "secret", true)));
        first = mock(Process.class);
        second = mock(Process.class);
        when(first.isAlive()).thenReturn(true);
        when(second.isAlive()).thenReturn(true);
        when(first.waitFor(anyLong(), any())).thenReturn(true);
        when(second.waitFor(anyLong(), any())).thenReturn(true);
        when(launcher.launch(anyList())).thenReturn(first, second);
    }

    @Test
    void usesDatabaseCredentialsAndIsolatesCameras() throws Exception {
        service.start("Entrada");
        service.start("Garagem");
        service.start("Entrada");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> commands = ArgumentCaptor.forClass(List.class);
        verify(launcher, times(2)).launch(commands.capture());
        assertTrue(commands.getAllValues().get(0).contains(
                "rtsp://admin%40:p%3Aa%2Fss%20%23%3F@192.168.1.10:554/Entrada"));
        assertTrue(commands.getAllValues().get(1).contains("rtsp://user:secret@192.168.1.11:8554/Garagem"));
        assertNotEquals(service.file("Entrada", "index.m3u8"), service.file("Garagem", "index.m3u8"));
        service.stop("Entrada");
        verify(first).destroy();
        verify(second, never()).destroy();
        assertEquals(directory.resolve("camera-2/index.m3u8"), service.file("Garagem", "index.m3u8"));
        service.shutdown();
        verify(second).destroy();
    }

    @Test
    void concurrentStartsForSameCameraLaunchOnlyOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            var futures = executor.invokeAll(java.util.Collections.nCopies(8,
                    (Callable<Void>) () -> { service.start("Entrada"); return null; }));
            for (var future : futures) future.get();
            verify(launcher).launch(anyList());
        } finally {
            executor.shutdownNow();
            service.shutdown();
        }
    }

    @Test
    void restartsDeadProcessAndRemovesOldSegments() throws Exception {
        service.start("Entrada");
        Files.writeString(directory.resolve("camera-1/index.m3u8"), "stale");
        Files.writeString(directory.resolve("camera-1/seg_00001.ts.tmp"), "stale");
        when(first.isAlive()).thenReturn(false);
        service.start("Entrada");
        verify(launcher, times(2)).launch(anyList());
        assertFalse(Files.exists(directory.resolve("camera-1/index.m3u8")));
        assertFalse(Files.exists(directory.resolve("camera-1/seg_00001.ts.tmp")));
    }

    @Test
    void rejectsUnknownInactiveAndUnsafeFiles() throws Exception {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.start("Unknown"))
                .getStatusCode().value());
        when(repository.findByName("Off")).thenReturn(Optional.of(
                new StreamCamera(3, "Off", "127.0.0.1", 554, "user", "secret", false)));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.start("Off"))
                .getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.file("Entrada", "../secret")).getStatusCode().value());
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> service.file("Entrada", "index.m3u8")).getStatusCode().value());
        verifyNoInteractions(launcher);
    }

    @Test
    void launchFailureDoesNotExposeCredentials() throws Exception {
        when(launcher.launch(anyList())).thenThrow(new IOException("secret connection data"));
        var exception = assertThrows(ResponseStatusException.class, () -> service.start("Entrada"));
        assertEquals(503, exception.getStatusCode().value());
        assertFalse(exception.getMessage().contains("secret"));
        assertNull(exception.getCause());
    }

    @Test
    void formatsIpv6AndDoesNotExposeCredentialsInToString() {
        var camera = new StreamCamera(1, "camera1", "::1", 554, "admin", "secret", true);
        assertEquals("rtsp://admin:secret@[::1]:554/camera1", service.rtspUrl(camera));
        assertFalse(camera.toString().contains("secret"));
    }
    @Test
    void encodesNameAsPathAndPreservesLeadingSlash() {
        var camera = new StreamCamera(1, "/Porta principal", "127.0.0.1", 554, "admin", "secret", true);
        assertEquals("rtsp://admin:secret@127.0.0.1:554/Porta%20principal", service.rtspUrl(camera));
    }
}

