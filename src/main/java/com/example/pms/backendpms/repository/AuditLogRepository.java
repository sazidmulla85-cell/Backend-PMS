package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

  List<AuditLog> findAllByOrderByCreatedAtDesc();
}
