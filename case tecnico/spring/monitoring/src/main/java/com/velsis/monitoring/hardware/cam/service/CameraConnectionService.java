package com.velsis.monitoring.hardware.cam.service;

import com.velsis.monitoring.hardware.cam.model.CameraConnection;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.repository.StreamCameraRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static com.velsis.monitoring.hardware.cam.model.CameraConnection.State.*;

@Service
public class CameraConnectionService {
    private final StreamCameraRepository repository;
    private final CameraOperabilityProbe probe;
    private final long interval;
    private final Map<Long, Sample> samples = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "camera-connection-monitor");
        thread.setDaemon(true);
        return thread;
    });
    private record Sample(String host, int port, CameraConnection result) {}

    public CameraConnectionService(StreamCameraRepository repository, CameraOperabilityProbe probe,
            @Value("${monitoring.camera.interval-ms:30000}") long interval) {
        if (interval < 1000) throw new IllegalArgumentException("Camera interval must be at least 1000 ms");
        this.repository = repository;
        this.probe = probe;
        this.interval = interval;
    }

    @PostConstruct
    public void start() {
        scheduler.scheduleWithFixedDelay(() -> {
            try { refresh(); }
            catch (Exception exception) {
                LoggerFactory.getLogger(getClass()).warn("Camera connection cycle failed", exception);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() { scheduler.shutdownNow(); }

    public void refresh() {
        List<StreamCamera> cameras = repository.findAll();
        samples.keySet().retainAll(cameras.stream().map(StreamCamera::id).collect(Collectors.toSet()));
        for (StreamCamera camera : cameras) {
            if (Thread.currentThread().isInterrupted()) return;
            check(camera);
        }
    }

    public List<CameraConnection> list() {
        return repository.findAll().stream().map(this::cached).toList();
    }

    public CameraConnection get(long id) { return cached(find(id)); }

    public CameraConnection check(long id) { return check(find(id)); }

    private StreamCamera find(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera nao encontrada"));
    }

    private CameraConnection cached(StreamCamera camera) {
        if (!camera.status()) return new CameraConnection(camera.id(), camera.nome(), DISABLED, null);
        Sample sample = samples.get(camera.id());
        if (sample == null || !sample.host().equals(camera.enderecoip()) || sample.port() != camera.portaRtsp())
            return new CameraConnection(camera.id(), camera.nome(), UNKNOWN, null);
        return new CameraConnection(camera.id(), camera.nome(), sample.result().state(), sample.result().checkedAt());
    }

    private CameraConnection check(StreamCamera camera) {
        if (!camera.status()) {
            samples.remove(camera.id());
            return new CameraConnection(camera.id(), camera.nome(), DISABLED, null);
        }
        // Serialize checks for the same camera; probes for different cameras remain independent.
        return samples.compute(camera.id(), (id, previous) -> {
            boolean online = probe.operable(camera);
            var result = new CameraConnection(camera.id(), camera.nome(), online ? ONLINE : OFFLINE, Instant.now());
            return new Sample(camera.enderecoip(), camera.portaRtsp(), result);
        }).result();
    }
}
