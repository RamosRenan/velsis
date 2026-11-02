package com.velsis.monitoring.hardware.cam.controller;

import com.velsis.monitoring.hardware.cam.model.CameraPage;
import com.velsis.monitoring.hardware.cam.model.CameraRequest;
import com.velsis.monitoring.hardware.cam.model.CameraResponse;
import com.velsis.monitoring.hardware.cam.service.CameraService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {
    private final CameraService service;

    public CameraController(CameraService service) {
        this.service = service;
    }

    @GetMapping
    public CameraPage list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CameraResponse> find(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(id));
    }

    @PostMapping
    public ResponseEntity<CameraResponse> create(@RequestBody CameraRequest request) {
        CameraResponse camera = service.create(request);
        return ResponseEntity.created(URI.create("/api/cameras/" + camera.id())).body(camera);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
