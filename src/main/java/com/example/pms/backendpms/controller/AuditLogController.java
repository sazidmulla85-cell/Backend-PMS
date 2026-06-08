package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.AuditDtos.AuditLogResponse;
import com.example.pms.backendpms.service.AuditLogService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/audit-logs")
public class AuditLogController {

  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public List<AuditLogResponse> auditLogs(@PathVariable Long propertyId) {
    return auditLogService.getByProperty(propertyId);
  }
}
