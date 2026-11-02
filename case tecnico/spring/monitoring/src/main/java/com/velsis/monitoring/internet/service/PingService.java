package com.velsis.monitoring.internet.service;

import com.velsis.monitoring.internet.model.PingRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class PingService {
    private static final String HOST = "8.8.8.8";
    private static final Pattern REPLY = Pattern.compile("\\bttl[= ]\\d+", Pattern.CASE_INSENSITIVE);

    public java.util.List<PingRecord> testBatch() {
        var results = new java.util.ArrayList<PingRecord>(10);
        for (int attempt = 0; attempt < 10; attempt++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Ping batch interrupted");
            }
            results.add(test());
        }
        return java.util.List.copyOf(results);
    }

    public PingRecord test() {
        long start = System.nanoTime();
        Process process = null;
        PingRecord.Status status;
        try {
            process = startPing();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                status = PingRecord.Status.TIMEOUT;
            } else {
                String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
                status = process.exitValue() == 0 && REPLY.matcher(output).find()
                        ? PingRecord.Status.SUCCESS : PingRecord.Status.NO_REPLY;
            }
        } catch (IOException exception) {
            status = PingRecord.Status.ERROR;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            status = PingRecord.Status.ERROR;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return new PingRecord(HOST, status,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), System.currentTimeMillis());
    }

    Process startPing() throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        // One packet, fixed destination, no shell or user-supplied command.
        return new ProcessBuilder(windows
                ? new String[]{"ping", "-n", "1", "-w", "3000", HOST}
                : new String[]{"ping", "-n", "-c", "1", "-W", "3", HOST})
                .redirectErrorStream(true).start();
    }
}
