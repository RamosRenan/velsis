package com.velsis.monitoring.security.persistence.repository;

import com.velsis.monitoring.security.persistence.entities.MonitoringUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<MonitoringUser, String> {
}
