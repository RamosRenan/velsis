package com.example.stream.web;

import com.example.stream.service.HlsService;
import com.example.stream.service.StreamUnavailableException;
import com.example.stream.service.WebRtcGatewayService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:4173", "http://127.0.0.1:4173"}) // Somente demonstração local
public class StreamController {
    private final HlsService hls;
    private final WebRtcGatewayService webrtc;

    public StreamController(HlsService hls, WebRtcGatewayService webrtc) {
        this.hls = hls; this.webrtc = webrtc;
    }

    @PostMapping("/{id}/hls/start")
    public Map<String, String> startHls(@PathVariable long id) throws IOException {
        hls.start(id);
        return Map.of("playlist", "/api/cameras/" + id + "/hls/index.m3u8");
    }

    @PostMapping("/{id}/hls/stop")
    public ResponseEntity<Void> stopHls(@PathVariable long id) {
        hls.stop(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/hls/{filename:.+}")
    public ResponseEntity<Resource> segment(@PathVariable long id, @PathVariable String filename) {
        Path file = hls.file(id, filename);
        if (!Files.isRegularFile(file)) return ResponseEntity.notFound().build();
        MediaType type = filename.endsWith(".m3u8")
                ? MediaType.parseMediaType("application/vnd.apple.mpegurl")
                : MediaType.parseMediaType("video/mp2t");
        return ResponseEntity.ok().contentType(type)
                .cacheControl(CacheControl.noStore()).body(new FileSystemResource(file));
    }

    @PostMapping("/{id}/webrtc/provision")
    public Map<String, String> provisionWebRtc(@PathVariable long id) {
        String url = webrtc.provision(id);
        return Map.of("playerUrl", url, "whepUrl", url + "/whep");
    }

    @ExceptionHandler(StreamUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(StreamUnavailableException ex) {
        org.slf4j.LoggerFactory.getLogger(StreamController.class).error("Falha ao iniciar HLS", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));
    }
}
