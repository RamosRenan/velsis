package com.example.stream.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableConfigurationProperties(StreamProperties.class)
public class StreamConfiguration {}
