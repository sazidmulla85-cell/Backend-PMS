package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.DashboardDtos.DashboardResponse;
import com.example.pms.backendpms.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public DashboardResponse dashboard(@PathVariable Long propertyId) {
    return dashboardService.getDashboard(propertyId);
  }
}
