package com.velsis.monitoring.hardware.camStream.service;

import com.velsis.monitoring.hardware.camStream.config.StreamProperties;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.repository.StreamCameraRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class HlsService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HlsService.class);
    private final StreamProperties properties;
    private final StreamCameraRepository cameras;
    private final FfmpegLauncher launcher;
    private final Map<Long, Process> active = new HashMap<>();
    private final Path root;

    public HlsService(StreamProperties properties, StreamCameraRepository cameras, FfmpegLauncher launcher) {
        this.properties = properties;
        this.cameras = cameras;
        this.launcher = launcher;
        this.root = Path.of(properties.directory()).toAbsolutePath().normalize();
    }

    public synchronized void start(String name) {
        StreamCamera camera = find(name);
        if (!camera.status()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Camera inativa");
        Process old = active.get(camera.id());
        if (old != null && old.isAlive()) return;
        active.remove(camera.id());
        Path dir = directory(camera.id());
        try {
            Files.createDirectories(dir);
            try (var files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.getFileName().toString()
                        .matches("(?:index\\.m3u8|seg_\\d+\\.ts)(?:\\.tmp)?")).toList()) {
                    Files.deleteIfExists(file);
                }
            }
            Process process = launcher.launch(List.of(
                    properties.ffmpeg(), "-hide_banner", "-nostdin", "-loglevel", "error",
                    "-rtsp_transport", "tcp", "-timeout", "15000000", "-i", rtspUrl(camera),
                    "-map", "0:v:0", "-map", "0:a:0?",
                    "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
                    "-pix_fmt", "yuv420p", "-g", "50", "-keyint_min", "50", "-sc_threshold", "0",
                    "-c:a", "aac", "-b:a", "96k", "-ar", "48000",
                    "-f", "hls", "-hls_time", "2", "-hls_list_size", "5",
                    "-hls_flags", "delete_segments+independent_segments+temp_file",
                    "-hls_segment_filename", dir.resolve("seg_%05d.ts").toString(),
                    dir.resolve("index.m3u8").toString()));
            active.put(camera.id(), process);
            log.info("FFmpeg iniciado: cameraId={}, pid={}, diretorio={}", camera.id(), process.pid(), dir);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nao foi possivel iniciar HLS. Verifique FFMPEG_PATH e STREAM_DIRECTORY.");
        }
    }

    public synchronized void stop(String name) {
        stopProcess(find(name).id());
    }

    public synchronized Path file(String name, String filename) {
        if (!filename.matches("index\\.m3u8|seg_\\d+\\.ts")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo invalido");
        }
        StreamCamera camera = find(name);
        if (!camera.status()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Camera inativa");
        Process process = active.get(camera.id());
        if (process != null && !process.isAlive()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "FFmpeg encerrou com codigo " + process.exitValue() + ". Consulte o log do backend.");
        }
        if (process == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stream indisponivel");
        }
        return directory(camera.id()).resolve(filename);
    }

    private StreamCamera find(String name) {
        return cameras.findByName(name).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera nao encontrada"));
    }

    private Path directory(long id) {
        return root.resolve("camera-" + id);
    }

    public static String rtspUrl(StreamCamera camera) {
        String host = camera.enderecoip();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        String path = org.springframework.web.util.UriUtils.encodePath(camera.nome(), StandardCharsets.UTF_8);
        if (!path.startsWith("/")) path = "/" + path;
        return "rtsp://" + encode(camera.usuario()) + ":" + encode(camera.senha())
                + "@" + host + ":" + camera.portaRtsp() + path;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void stopProcess(long id) {
        Process process = active.remove(id);
        if (process == null) return;
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        new ArrayList<>(active.keySet()).forEach(this::stopProcess);
    }
}

