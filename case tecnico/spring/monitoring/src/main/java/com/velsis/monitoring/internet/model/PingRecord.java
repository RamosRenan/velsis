package com.velsis.monitoring.internet.model;

public record PingRecord(String host, Status status, long durationMs, long timestamp) {
    public enum Status { SUCCESS, NO_REPLY, TIMEOUT, ERROR }
}
