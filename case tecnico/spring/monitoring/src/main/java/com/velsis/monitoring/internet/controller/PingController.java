package com.velsis.monitoring.internet.controller;

import com.velsis.monitoring.internet.model.PingRecord;
import com.velsis.monitoring.internet.service.PingService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class PingController {
    private final PingService pingService;

    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    @GetMapping("/ping")
    public ResponseEntity<java.util.List<PingRecord>> test() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pingService.testBatch());
    }
}
