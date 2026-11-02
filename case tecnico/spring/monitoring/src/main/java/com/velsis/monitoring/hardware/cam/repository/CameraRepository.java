package com.velsis.monitoring.hardware.cam.repository;

import com.velsis.monitoring.hardware.cam.model.CameraRequest;
import com.velsis.monitoring.hardware.cam.model.CameraResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CameraRepository {
    private final JdbcTemplate jdbc;
    private static final String COLUMNS = "id, nome, descricao, enderecoip, porta_http, porta_rtsp, usuario, status";
    private static final RowMapper<CameraResponse> MAPPER = (row, index) -> new CameraResponse(
            row.getLong("id"), row.getString("nome"), row.getString("descricao"),
            row.getString("enderecoip"), row.getInt("porta_http"), row.getInt("porta_rtsp"),
            row.getString("usuario"), row.getBoolean("status"), true);

    public CameraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CameraResponse> findPage(int page, int size) {
        return jdbc.query("SELECT " + COLUMNS + " FROM \"CAMERA\" ORDER BY id DESC LIMIT ? OFFSET ?",
                MAPPER, size, (long) page * size);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM \"CAMERA\"", Long.class);
    }

    public Optional<CameraResponse> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM \"CAMERA\" WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public long insert(CameraRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO "CAMERA" (nome, descricao, enderecoip, porta_http, porta_rtsp, usuario, senha, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, request.nome().trim(), request.descricao(), request.enderecoip().trim(),
                request.portaHttp(), request.portaRtsp(), request.usuario().trim(), request.senha(), request.status());
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM \"CAMERA\" WHERE id = ?", id) > 0;
    }
}
