package com.velsis.monitoring.hardware.cam.service;

import com.velsis.monitoring.hardware.camStream.config.StreamProperties;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.service.FfmpegLauncher;
import com.velsis.monitoring.hardware.camStream.service.HlsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class CameraOperabilityProbe {
    private static final Pattern REPLY = Pattern.compile("\\b(?:ttl[= ]\\d+|(?:time|tempo)[=<]\\s*\\d+)", Pattern.CASE_INSENSITIVE);
    private final StreamProperties properties;
    private final FfmpegLauncher launcher;
    private final int pingTimeout;
    private final int frameTimeout;

    public CameraOperabilityProbe(StreamProperties properties, FfmpegLauncher launcher,
            @Value("${monitoring.camera.ping-timeout-ms:3000}") int pingTimeout,
            @Value("${monitoring.camera.frame-timeout-ms:10000}") int frameTimeout) {
        if (pingTimeout < 1 || pingTimeout > 30000 || frameTimeout < 1 || frameTimeout > 60000)
            throw new IllegalArgumentException("Invalid camera probe timeouts");
        this.properties = properties;
        this.launcher = launcher;
        this.pingTimeout = pingTimeout;
        this.frameTimeout = frameTimeout;
    }

    public boolean operable(StreamCamera camera) {
        return ping(camera.enderecoip()) && frame(camera);
    }

    boolean ping(String host) {
        // Only registered IP literals or localhost; never permit command-line options.
        if (!host.equalsIgnoreCase("localhost") && !host.matches("[0-9a-fA-F:.]+")) return false;
        Process process = null;
        try {
            process = startPing(host);
            if (!process.waitFor(pingTimeout + 1000L, TimeUnit.MILLISECONDS)) return false;
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            return process.exitValue() == 0 && REPLY.matcher(output).find();
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    Process startPing(String host) throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        return new ProcessBuilder(windows
                ? List.of("ping", "-n", "1", "-w", Integer.toString(pingTimeout), host)
                : List.of("ping", "-n", "-c", "1", "-W", Integer.toString((pingTimeout + 999) / 1000), host))
                .redirectErrorStream(true).start();
    }

    boolean frame(StreamCamera camera) {
        Process process = null;
        Path image = null;
        try {
            image = Files.createTempFile("camera-probe-", ".jpg");
            process = launcher.launch(List.of(properties.ffmpeg(), "-hide_banner", "-nostdin", "-loglevel", "error",
                    "-rtsp_transport", "tcp", "-timeout", Long.toString(frameTimeout * 1000L),
                    "-i", HlsService.rtspUrl(camera), "-map", "0:v:0", "-frames:v", "1",
                    "-an", "-c:v", "mjpeg", "-f", "image2", "-y", image.toString()));
            if (!process.waitFor(frameTimeout, TimeUnit.MILLISECONDS) || process.exitValue() != 0) return false;
            // Exit code alone can be successful even when no video frames were received.
            return Files.size(image) > 0 && ImageIO.read(image.toFile()) != null;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try { process.waitFor(1, TimeUnit.SECONDS); }
                catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            }
            if (image != null) {
                try { Files.deleteIfExists(image); }
                catch (IOException exception) { image.toFile().deleteOnExit(); }
            }
        }
    }
}
