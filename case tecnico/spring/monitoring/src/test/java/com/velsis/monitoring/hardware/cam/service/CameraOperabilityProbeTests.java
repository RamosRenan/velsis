package com.velsis.monitoring.hardware.cam.service;

import com.velsis.monitoring.hardware.camStream.config.StreamProperties;
import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import com.velsis.monitoring.hardware.camStream.service.FfmpegLauncher;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CameraOperabilityProbeTests {
    final StreamCamera camera = new StreamCamera(1, "Entrada", "127.0.0.1", 554, "user", "secret", true);
    final FfmpegLauncher launcher = mock(FfmpegLauncher.class);
    final CameraOperabilityProbe probe = spy(new CameraOperabilityProbe(new StreamProperties("ffmpeg", "unused"), launcher, 3000, 10000));

    @Test
    void requiresBothPingAndFrame() {
        doReturn(false).when(probe).ping(anyString());
        assertFalse(probe.operable(camera));
        verify(probe, never()).frame(any());
        doReturn(true).when(probe).ping(anyString());
        doReturn(false, true).when(probe).frame(camera);
        assertFalse(probe.operable(camera));
        assertTrue(probe.operable(camera));
    }

    @Test
    void rejectsWindowsUnreachableEvenWithZeroExitCode() throws Exception {
        var process = mock(Process.class);
        doReturn(process).when(probe).startPing("127.0.0.1");
        when(process.waitFor(anyLong(), any())).thenReturn(true);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("Destination host unreachable".getBytes()));
        assertFalse(probe.ping("127.0.0.1"));
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("Reply from 127.0.0.1: bytes=32 time<1ms TTL=128".getBytes()));
        assertTrue(probe.ping("127.0.0.1"));
    }

    @Test
    void killsTimedOutPing() throws Exception {
        var process = mock(Process.class);
        doReturn(process).when(probe).startPing("127.0.0.1");
        when(process.isAlive()).thenReturn(true);
        assertFalse(probe.ping("127.0.0.1"));
        verify(process).destroyForcibly();
    }

    @Test
    void requiresActualDecodedImageAndCleansTemporaryFile() throws Exception {
        var process = mock(Process.class);
        when(process.waitFor(anyLong(), any())).thenReturn(true);
        final Path[] output = new Path[1];
        when(launcher.launch(anyList())).thenAnswer(invocation -> {
            List<String> command = invocation.getArgument(0);
            output[0] = Path.of(command.get(command.size() - 1));
            return process;
        });
        assertFalse(probe.frame(camera));
        assertFalse(Files.exists(output[0]));
        reset(launcher);
        when(launcher.launch(anyList())).thenAnswer(invocation -> {
            List<String> command = invocation.getArgument(0);
            output[0] = Path.of(command.get(command.size() - 1));
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpg", output[0].toFile());
            return process;
        });
        assertTrue(probe.frame(camera));
        assertFalse(Files.exists(output[0]));
        reset(launcher);
    }

    @Test
    void killsFfmpegOnTimeout() throws Exception {
        var process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        when(launcher.launch(anyList())).thenReturn(process);
        assertFalse(probe.frame(camera));
        verify(process).destroyForcibly();
    }
}
