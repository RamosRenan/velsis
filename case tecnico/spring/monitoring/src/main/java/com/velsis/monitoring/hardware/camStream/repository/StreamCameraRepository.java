package com.velsis.monitoring.hardware.camStream.repository;

import com.velsis.monitoring.hardware.camStream.model.StreamCamera;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class StreamCameraRepository {
    private final JdbcTemplate jdbc;

    public StreamCameraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StreamCamera> findByName(String name) {
        return jdbc.query("""
                SELECT id, nome, enderecoip, porta_rtsp, usuario, senha, status
                FROM "CAMERA" WHERE nome = ?
                """, (row, index) -> new StreamCamera(row.getLong("id"), row.getString("nome"), row.getString("enderecoip"),
                row.getInt("porta_rtsp"), row.getString("usuario"), row.getString("senha"),
                row.getBoolean("status")), name).stream().findFirst();
    }

    public Optional<StreamCamera> findById(long id) {
        return jdbc.query("""
                SELECT id, nome, enderecoip, porta_rtsp, usuario, senha, status
                FROM "CAMERA" WHERE id = ?
                """, (row, index) -> new StreamCamera(row.getLong("id"), row.getString("nome"),
                row.getString("enderecoip"), row.getInt("porta_rtsp"), row.getString("usuario"),
                row.getString("senha"), row.getBoolean("status")), id).stream().findFirst();
    }
    public List<StreamCamera> findAll() {
        return jdbc.query("""
                SELECT id, nome, enderecoip, porta_rtsp, usuario, senha, status
                FROM "CAMERA" ORDER BY id
                """, (row, index) -> new StreamCamera(row.getLong("id"), row.getString("nome"),
                row.getString("enderecoip"), row.getInt("porta_rtsp"), row.getString("usuario"),
                row.getString("senha"), row.getBoolean("status")));
    }
}

