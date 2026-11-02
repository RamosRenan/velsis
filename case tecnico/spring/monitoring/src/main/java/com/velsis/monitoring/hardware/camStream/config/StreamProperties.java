package com.velsis.monitoring.hardware.camStream.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "stream")
public record StreamProperties(
        @DefaultValue("ffmpeg") String ffmpeg,
        @DefaultValue("./runtime-hls") String directory) {}

