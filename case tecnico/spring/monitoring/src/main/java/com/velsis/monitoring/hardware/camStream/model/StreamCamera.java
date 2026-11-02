package com.velsis.monitoring.hardware.camStream.model;

// Internal connection data: never return credentials from a controller.
public record StreamCamera(long id, String nome, String enderecoip, int portaRtsp,
                           String usuario, String senha, boolean status) {
    @Override
    public String toString() {
        return "StreamCamera[id=" + id + ", status=" + status + "]";
    }
}

