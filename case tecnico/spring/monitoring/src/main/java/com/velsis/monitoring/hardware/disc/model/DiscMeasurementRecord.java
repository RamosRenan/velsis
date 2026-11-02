package com.velsis.monitoring.hardware.disc.model;

import java.math.BigInteger;

public record DiscMeasurementRecord(
        BigInteger totalBytes,
        BigInteger freeByte,
        BigInteger timestamp
) {}
