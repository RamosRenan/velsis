package com.velsis.monitoring.hardware.cam.connection;

import com.velsis.monitoring.hardware.cam.service.CameraConnectionService;
import com.velsis.monitoring.hardware.cam.service.CameraOperabilityProbe;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.repository.StreamCameraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.velsis.monitoring.hardware.cam.model.CameraConnection.State.*;

class CameraConnectionTests {
    @Test
    void tracksFailureRecoveryDisabledAndMissingCameras() {
        var repository = mock(StreamCameraRepository.class);
        var probe = mock(CameraOperabilityProbe.class);
        var service = new CameraConnectionService(repository, probe, 30000);
        var camera = new StreamCamera(1, "Entrada", "127.0.0.1", 554, "user", "secret", true);
        var disabled = new StreamCamera(2, "Desativada", "127.0.0.1", 8554, "user", "secret", false);
        when(repository.findAll()).thenReturn(List.of(camera, disabled));
        when(repository.findById(1)).thenReturn(Optional.of(camera));
        when(repository.findById(2)).thenReturn(Optional.of(disabled));
        assertEquals(UNKNOWN, service.get(1).state());
        when(probe.operable(camera)).thenReturn(false, true);
        service.refresh();
        assertEquals(OFFLINE, service.get(1).state());
        assertNotNull(service.get(1).checkedAt());
        assertEquals(DISABLED, service.check(2).state());
        verify(probe, never()).operable(disabled);
        assertEquals(ONLINE, service.check(1).state());
        assertThrows(ResponseStatusException.class, () -> service.get(99));
        when(repository.findAll()).thenReturn(List.of());
        service.refresh();
        assertEquals(UNKNOWN, service.get(1).state());
        service.stop();
    }
}
