package com.velsis.monitoring.hardware.camStream.controller;

import com.velsis.monitoring.hardware.cam.model.CameraConnection;
import com.velsis.monitoring.hardware.camStream.service.HlsService;
import com.velsis.monitoring.hardware.camStream.model.CameraStatusAlert;
import com.velsis.monitoring.hardware.cam.service.CameraConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cameras")
public class StreamController {
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    private final HlsService hls;
    private final CameraConnectionService cameras;

    public StreamController(HlsService hls, CameraConnectionService cameras) {
        this.hls = hls;
        this.cameras = cameras;
    }

    @GetMapping("/inoperative")
    public List<CameraStatusAlert> inoperative() {
        return cameras.list().stream()
                .filter(camera -> camera.state() == CameraConnection.State.OFFLINE)
                .map(camera -> new CameraStatusAlert(
                        camera.id(), camera.nome(), "A câmera está inoperante."))
                .collect(Collectors.toList());
    }

    @PostMapping("/by-name/{name}/hls/start")
    public Map<String, String> start(@PathVariable String name) {
        log.info("STARTING STREAM");
        hls.start(name);
        return Map.of("playlist", "/api/cameras/by-name/"
                + UriUtils.encodePathSegment(name, StandardCharsets.UTF_8) + "/hls/index.m3u8");
    }

    @PostMapping("/by-name/{name}/hls/stop")
    public ResponseEntity<Void> stop(@PathVariable String name) {
        log.info("STOPING STREAM");
        hls.stop(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-name/{name}/hls/{filename:.+}")
    public ResponseEntity<Resource> file(@PathVariable String name, @PathVariable String filename) {
        log.info("FILE STREAM");
        var path = hls.file(name, filename);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        var type = MediaType.parseMediaType(filename.endsWith(".m3u8")
                ? "application/vnd.apple.mpegurl" : "video/mp2t");
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noStore())
                .body(new FileSystemResource(path));
    }
}

