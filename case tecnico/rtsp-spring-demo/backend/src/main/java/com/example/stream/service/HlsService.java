package com.example.stream.service;

import com.example.stream.config.StreamProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HlsService {
    private final StreamProperties properties;
    private final Map<Long, Process> active = new ConcurrentHashMap<>();
    private final Path root;

    public HlsService(StreamProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.directory()).toAbsolutePath().normalize();
    }

    public synchronized void start(long cameraId) throws IOException {
        requireDemoCamera(cameraId);
        Process old = active.get(cameraId);
        if (old != null && old.isAlive()) return;
        Path dir = directory(cameraId);
        Files.createDirectories(dir);
        try (var files = Files.list(dir)) {
            for (Path f : files.filter(x -> x.getFileName().toString().matches("(?:index\\.m3u8|seg_\\d+\\.ts)" )).toList()) Files.deleteIfExists(f);
        }
        List<String> cmd = List.of(
            properties.ffmpeg(), "-hide_banner", "-nostdin", "-loglevel", "warning",
            "-rtsp_transport", "tcp", "-i", properties.rtspUrl(),
            "-map", "0:v:0", "-map", "0:a:0?",
            "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
            "-pix_fmt", "yuv420p", "-g", "50", "-keyint_min", "50", "-sc_threshold", "0",
            "-c:a", "aac", "-b:a", "96k", "-ar", "48000",
            "-f", "hls", "-hls_time", "2", "-hls_list_size", "5",
            "-hls_flags", "delete_segments+independent_segments+temp_file",
            "-hls_segment_filename", dir.resolve("seg_%05d.ts").toString(),
            dir.resolve("index.m3u8").toString()
        );
        Process process;
        try {
            process = new ProcessBuilder(cmd)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.appendTo(dir.resolve("ffmpeg.log").toFile()))
                .start();
        } catch (IOException ex) {
            throw new StreamUnavailableException(
                "Nao foi possivel iniciar o FFmpeg. Instale o FFmpeg e configure FFMPEG_PATH "
                + "com o caminho completo do ffmpeg.exe no ambiente do Spring. "
                + "Reinicie o backend apos configurar. Verifique tambem a permissao de escrita em STREAM_DIRECTORY.", ex);
        }
        active.put(cameraId, process);
    }

    public synchronized void stop(long cameraId) {
        Process process = active.remove(cameraId);
        if (process != null) {
            process.destroy();
            try { if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly(); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); process.destroyForcibly(); }
        }
    }

    public Path file(long cameraId, String filename) {
        requireDemoCamera(cameraId);
        if (!filename.matches("index\\.m3u8|seg_\\d+\\.ts")) throw new IllegalArgumentException("Nome de arquivo inválido");
        return directory(cameraId).resolve(filename).normalize();
    }

    private Path directory(long cameraId) { return root.resolve("camera-" + cameraId); }
    public static void requireDemoCamera(long id) { if (id != 1) throw new IllegalArgumentException("Demo suporta apenas camera 1"); }
    @PreDestroy public void shutdown() { new ArrayList<>(active.keySet()).forEach(this::stop); }
}
