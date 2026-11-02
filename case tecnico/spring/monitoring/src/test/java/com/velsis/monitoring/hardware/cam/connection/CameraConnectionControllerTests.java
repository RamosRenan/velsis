package com.velsis.monitoring.hardware.cam.connection;

import com.velsis.monitoring.hardware.cam.controller.CameraConnectionController;
import com.velsis.monitoring.hardware.cam.model.CameraConnection;
import com.velsis.monitoring.hardware.cam.service.CameraConnectionService;
import com.velsis.monitoring.hardware.camStream.controller.StreamController;
import com.velsis.monitoring.hardware.camStream.service.HlsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.velsis.monitoring.hardware.cam.model.CameraConnection.State.*;

class CameraConnectionControllerTests {
    @Test
    void exposesChecksAndOnlyAlertsOnVerifiedOfflineCameras() throws Exception {
        var service = mock(CameraConnectionService.class);
        var offline = new CameraConnection(1, "Entrada", OFFLINE, Instant.now());
        when(service.list()).thenReturn(List.of(offline,
                new CameraConnection(2, "Outra", ONLINE, Instant.now()),
                new CameraConnection(3, "Nova", UNKNOWN, null),
                new CameraConnection(4, "Desativada", DISABLED, null)));
        when(service.get(1)).thenReturn(offline);
        when(service.check(1)).thenReturn(offline);
        var mvc = MockMvcBuilders.standaloneSetup(new CameraConnectionController(service),
                new StreamController(mock(HlsService.class), service)).build();
        mvc.perform(get("/api/cameras/connections")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4)).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/cameras/1/connection")).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OFFLINE"));
        mvc.perform(post("/api/cameras/1/connection/check")).andExpect(status().isOk());
        verify(service).check(1);
        mvc.perform(get("/api/cameras/inoperative")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(1));
    }
}
