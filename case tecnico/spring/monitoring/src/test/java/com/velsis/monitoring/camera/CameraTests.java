package com.velsis.monitoring.camera;

import com.velsis.monitoring.hardware.cam.controller.CameraController;
import com.velsis.monitoring.hardware.cam.model.CameraResponse;
import com.velsis.monitoring.hardware.cam.repository.CameraRepository;
import com.velsis.monitoring.hardware.cam.service.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CameraTests {
    CameraRepository repository;
    MockMvc mvc;
    CameraResponse camera = new CameraResponse(1, "Entrada", "Porta principal", "192.168.1.10", 80, 554, "admin", true, true);

    @BeforeEach
    void setup() {
        repository = mock(CameraRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new CameraController(new CameraService(repository))).build();
    }

    private String payload(String ip, int port) {
        return """
                {"nome":"Entrada","descricao":"Porta principal","enderecoip":"%s",
                 "portaHttp":%d,"portaRtsp":554,"usuario":"admin","senha":"secret","status":true}
                """.formatted(ip, port);
    }

    @Test
    void createsCameraWithoutReturningPassword() throws Exception {
        when(repository.insert(any())).thenReturn(1L);
        when(repository.findById(1)).thenReturn(Optional.of(camera));
        mvc.perform(post("/api/cameras").contentType(MediaType.APPLICATION_JSON).content(payload("192.168.1.10", 80)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/cameras/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaCadastrada").value(true));
    }

    @Test
    void rejectsDuplicateCameraName() throws Exception {
        when(repository.insert(any())).thenThrow(new org.springframework.dao.DuplicateKeyException("uk_camera_nome"));
        mvc.perform(post("/api/cameras").contentType(MediaType.APPLICATION_JSON).content(payload("192.168.1.10", 80)))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptsLocalhostAndIpAddresses() throws Exception {
        when(repository.insert(any())).thenReturn(1L);
        when(repository.findById(1)).thenReturn(Optional.of(camera));
        for (String host : List.of("localhost", "LOCALHOST", " localhost ", "127.0.0.1", "::1")) {
            mvc.perform(post("/api/cameras").contentType(MediaType.APPLICATION_JSON).content(payload(host, 80)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void validatesPortsIpAndRequiredFields() throws Exception {
        for (String body : List.of(payload("192.168.1.10", 0), payload("999.1.1.1", 80),
                payload("example.com", 80), payload("localhost.example.com", 80),
                payload("http://localhost", 80), payload("localhost:8554", 80), "{}")) {
            mvc.perform(post("/api/cameras").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(repository, never()).insert(any());
    }

    @Test
    void paginatesAndValidatesPageBounds() throws Exception {
        when(repository.findPage(1, 5)).thenReturn(List.of(camera));
        when(repository.count()).thenReturn(6L);
        mvc.perform(get("/api/cameras?page=1&size=5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].senha").doesNotExist());
        mvc.perform(get("/api/cameras?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/cameras?size=101")).andExpect(status().isBadRequest());
    }

    @Test
    void retrievesDetailsAndHandlesMissingCamera() throws Exception {
        when(repository.findById(1)).thenReturn(Optional.of(camera));
        mvc.perform(get("/api/cameras/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Entrada"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(get("/api/cameras/2")).andExpect(status().isNotFound());
    }

    @Test
    void deletesAndHandlesMissingCamera() throws Exception {
        when(repository.delete(1)).thenReturn(true);
        mvc.perform(delete("/api/cameras/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/cameras/2")).andExpect(status().isNotFound());
    }

    @Test
    void parsesBothCameraMigrations() throws Exception {
        try (var resources = new liquibase.resource.ClassLoaderResourceAccessor()) {
            var changelog = liquibase.parser.ChangeLogParserFactory.getInstance()
                    .getParser("db/changelog/db.changelog-master.xml", resources)
                    .parse("db/changelog/db.changelog-master.xml", new liquibase.changelog.ChangeLogParameters(), resources);
            assertTrue(changelog.getChangeSets().stream().anyMatch(change -> change.getId().equals("002-create-camera")));
            assertTrue(changelog.getChangeSets().stream().anyMatch(change -> change.getId().equals("003-create-monitoramento-camera")));
            assertTrue(changelog.getChangeSets().stream().anyMatch(change -> change.getId().equals("004-camera-unique-name")));
        }
    }
}
