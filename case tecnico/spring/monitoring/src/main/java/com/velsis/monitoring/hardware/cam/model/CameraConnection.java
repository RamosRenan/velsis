package com.velsis.monitoring.hardware.cam.model;

import java.time.Instant;

public record CameraConnection(long id, String nome, State state, Instant checkedAt) {
    public enum State { UNKNOWN, DISABLED, ONLINE, OFFLINE }
}
