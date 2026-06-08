package com.example.pms.backendpms.dto;

import java.time.LocalDateTime;

public final class AuditDtos {

  private AuditDtos() {}

  public record AuditLogResponse(
      Long auditLogId,
      String module,
      String action,
      String entityType,
      String entityId,
      String description,
      String actorName,
      LocalDateTime createdAt
  ) {}
}
