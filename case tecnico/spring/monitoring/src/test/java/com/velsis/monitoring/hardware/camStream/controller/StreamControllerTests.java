package com.velsis.monitoring.hardware.camStream.controller;

import com.velsis.monitoring.hardware.cam.service.CameraConnectionService;
import com.velsis.monitoring.hardware.camStream.service.HlsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StreamControllerTests {
    @TempDir Path directory;

    @Test
    void startsByNameAndReturnsEncodedPlaylist() throws Exception {
        var service = mock(HlsService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new StreamController(service, mock(CameraConnectionService.class))).build();
        mvc.perform(post("/api/cameras/by-name/{name}/hls/start", "Porta principal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlist").value(
                        "/api/cameras/by-name/Porta%20principal/hls/index.m3u8"));
        verify(service).start("Porta principal");
        mvc.perform(post("/api/cameras/by-name/Entrada/hls/stop")).andExpect(status().isNoContent());
        verify(service).stop("Entrada");
    }

    @Test
    void servesHlsWithCorrectContentTypeAndNoCache() throws Exception {
        var service = mock(HlsService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new StreamController(service, mock(CameraConnectionService.class))).build();
        var playlist = Files.writeString(directory.resolve("index.m3u8"), "#EXTM3U");
        var segment = Files.write(directory.resolve("seg_00001.ts"), new byte[]{1, 2, 3});
        when(service.file("Entrada", "index.m3u8")).thenReturn(playlist);
        when(service.file("Entrada", "seg_00001.ts")).thenReturn(segment);
        when(service.file("Entrada", "seg_00002.ts")).thenReturn(directory.resolve("missing.ts"));
        mvc.perform(get("/api/cameras/by-name/Entrada/hls/index.m3u8"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/vnd.apple.mpegurl"))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(content().string("#EXTM3U"));
        mvc.perform(get("/api/cameras/by-name/Entrada/hls/seg_00001.ts"))
                .andExpect(status().isOk()).andExpect(content().contentType("video/mp2t"));
        mvc.perform(get("/api/cameras/by-name/Entrada/hls/seg_00002.ts")).andExpect(status().isNotFound());
    }
}

