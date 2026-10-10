package com.testpilot.repository;

import com.testpilot.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByEntityTypeAndEntityNoOrderByIdDesc(String entityType, String entityNo);
}
