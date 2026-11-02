package com.velsis.monitoring.hardware.camStream.service;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FfmpegLauncher {
    private static final Logger log = LoggerFactory.getLogger(FfmpegLauncher.class);

    public Process launch(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        Thread diagnostics = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.warn("FFmpeg pid={}: {}", process.pid(), sanitize(line));
                }
            } catch (IOException exception) {
                log.debug("Leitura de diagnostico encerrada para FFmpeg pid={}", process.pid());
            }
        }, "ffmpeg-stderr-" + process.pid());
        diagnostics.setDaemon(true);
        diagnostics.start();
        process.onExit().thenAccept(exited ->
                log.info("FFmpeg pid={} encerrado com codigo {}", exited.pid(), exited.exitValue()));
        return process;
    }

    static String sanitize(String line) {
        // Remove the entire RTSP URL, including user info and query parameters.
        return line.replaceAll("(?i)rtsps?://[^\\s\\\"'<>]+", "[RTSP URL ocultada]");
    }
}

