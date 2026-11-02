package com.velsis.monitoring.hardware.disc.service;

import com.velsis.monitoring.hardware.disc.model.DiscMeasurementRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class InfoDiscService {
    private final Path path;

    public InfoDiscService(@Value("${monitoring.disc.path:.}") String path) {
        this.path = Path.of(path).toAbsolutePath();
    }

    public DiscMeasurementRecord measure() throws IOException {
        var store = Files.getFileStore(path);
        return new DiscMeasurementRecord(
                BigInteger.valueOf(store.getTotalSpace()),
                BigInteger.valueOf(store.getUnallocatedSpace()),
                BigInteger.valueOf(System.currentTimeMillis()));
    }
}
