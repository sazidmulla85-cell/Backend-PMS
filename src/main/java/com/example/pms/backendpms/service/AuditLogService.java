package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.AuditDtos.AuditLogResponse;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditLog;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  public void log(
      UserAccount actor,
      Property property,
      AuditModule module,
      AuditAction action,
      String entityType,
      String entityId,
      String description
  ) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActor(actor);
    auditLog.setProperty(property);
    auditLog.setModule(module);
    auditLog.setAction(action);
    auditLog.setEntityType(entityType);
    auditLog.setEntityId(entityId);
    auditLog.setDescription(description);
    auditLogRepository.save(auditLog);
  }

  public List<AuditLogResponse> getByProperty(Long propertyId) {
    return auditLogRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
        .map(log -> new AuditLogResponse(
            log.getId(),
            log.getModule().name(),
            log.getAction().name(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDescription(),
            log.getActor() != null ? log.getActor().getFullName() : "System",
            log.getCreatedAt()
        ))
        .toList();
  }
}
