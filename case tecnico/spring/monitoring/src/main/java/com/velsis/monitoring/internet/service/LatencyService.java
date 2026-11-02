package com.velsis.monitoring.internet.service;

import com.velsis.monitoring.internet.model.LatencyRecord;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class LatencyService {
    private final short PORT = 443;
    private final String ip = "8.8.8.8";

    public Object measure() {
        try (Socket socket = new Socket()) {
            long start = System.nanoTime();
            socket.connect(new java.net.InetSocketAddress(ip, PORT), 10_000);
            long end = System.nanoTime();
            return new LatencyRecord((end - start) / 1_000_000); // converte para ms
        } catch (UnknownHostException e) {
            System.err.println("Host inválido: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
        }
        return -1;
    }
}
