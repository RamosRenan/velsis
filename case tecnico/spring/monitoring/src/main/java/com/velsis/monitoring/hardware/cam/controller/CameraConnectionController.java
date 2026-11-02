package com.velsis.monitoring.hardware.cam.controller;

import com.velsis.monitoring.hardware.cam.model.CameraConnection;
import com.velsis.monitoring.hardware.cam.service.CameraConnectionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CameraConnectionController {
    private final CameraConnectionService service;
    public CameraConnectionController(CameraConnectionService service) { this.service = service; }

    @GetMapping("/connections")
    public ResponseEntity<List<CameraConnection>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list());
    }

    @GetMapping("/{id}/connection")
    public ResponseEntity<CameraConnection> get(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(id));
    }

    @PostMapping("/{id}/connection/check")
    public ResponseEntity<CameraConnection> check(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.check(id));
    }
}
