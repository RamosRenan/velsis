package com.velsis.monitoring.security.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
public class MonitoringUser {
    @Id
    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = false, length = 60)
    private String senha;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MonitoringUser() {
    }

    public String getUsuario() {
        return usuario;
    }

    public String getSenha() {
        return senha;
    }
}
