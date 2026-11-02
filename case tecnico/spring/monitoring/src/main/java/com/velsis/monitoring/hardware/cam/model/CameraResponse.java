package com.velsis.monitoring.hardware.cam.model;

public record CameraResponse(long id, String nome, String descricao, String enderecoip,
                             int portaHttp, int portaRtsp, String usuario,
                             boolean status, boolean senhaCadastrada) {}
