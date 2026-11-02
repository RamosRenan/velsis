package com.example.stream.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "stream")
public record StreamProperties(
        String ffmpeg,
        String rtspUrl,

        // no use
        String gatewayRtspUrl,
        String mediaApi,
        String mediaBrowserUrl,
        String directory
) {}
