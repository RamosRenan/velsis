package com.velsis.monitoring.hardware.camStream.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FfmpegLauncherTests {
    @Test
    void removesCredentialsAndQuerySecretsFromDiagnosticUrls() {
        String message = FfmpegLauncher.sanitize(
                "Error opening 'rtsp://admin:p%40ss@127.0.0.1:8554/camera?token=secret': 401 Unauthorized");
        assertFalse(message.contains("admin"));
        assertFalse(message.contains("p%40ss"));
        assertFalse(message.contains("secret"));
        assertTrue(message.contains("401 Unauthorized"));
    }

    @Test
    void preservesUsefulErrorsAndRemovesMultipleUrls() {
        assertEquals("Connection refused", FfmpegLauncher.sanitize("Connection refused"));
        assertEquals("[RTSP URL ocultada] [RTSP URL ocultada]",
                FfmpegLauncher.sanitize("RTSP://user:pass@host/a rtsps://user:pass@host/b"));
    }
}
