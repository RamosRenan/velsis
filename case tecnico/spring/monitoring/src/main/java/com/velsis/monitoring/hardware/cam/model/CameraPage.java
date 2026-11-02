package com.velsis.monitoring.hardware.cam.model;

import java.util.List;

public record CameraPage(List<CameraResponse> content, long totalElements, int page, int size) {}
