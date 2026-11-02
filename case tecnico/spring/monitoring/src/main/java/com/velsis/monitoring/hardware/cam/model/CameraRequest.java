package com.velsis.monitoring.hardware.cam.model;

public record CameraRequest(String nome, String descricao, String enderecoip,
                            Integer portaHttp, Integer portaRtsp, String usuario,
                            String senha, Boolean status) {}
