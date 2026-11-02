package com.example.stream.service;

import com.example.stream.config.StreamProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
public class WebRtcGatewayService {
    private final StreamProperties config;
    private final RestClient http;
    public WebRtcGatewayService(StreamProperties config, RestClient.Builder builder) {
        this.config = config;
        this.http = builder.baseUrl(config.mediaApi()).build();
    }
    public String provision(long cameraId) {
        HlsService.requireDemoCamera(cameraId);
        String path = "camera-" + cameraId;
        Map<String, Object> body = Map.of("source", config.gatewayRtspUrl(),
                "rtspTransport", "tcp", "sourceOnDemand", true);
        // Upsert: add; if path already exists, patch (API responds 4xx for existing paths).
        try {
            http.post().uri("/v3/config/paths/add/{name}", path)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
            http.patch().uri("/v3/config/paths/patch/{name}", path)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        }
        return config.mediaBrowserUrl() + "/" + path;
    }
}
