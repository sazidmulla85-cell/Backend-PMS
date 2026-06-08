package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.AdminDtos.AdminOverviewResponse;
import com.example.pms.backendpms.dto.AdminDtos.CreatePropertyAccountRequest;
import com.example.pms.backendpms.dto.AdminDtos.PropertyAccountSummary;
import com.example.pms.backendpms.dto.AdminDtos.UpdatePropertyAccountRequest;
import com.example.pms.backendpms.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/overview")
  public AdminOverviewResponse overview() {
    return adminService.getOverview();
  }

  @PostMapping("/properties")
  public PropertyAccountSummary createPropertyAccount(@Valid @RequestBody CreatePropertyAccountRequest request) {
    return adminService.createPropertyAccount(request);
  }

  @PutMapping("/properties/{propertyId}")
  public PropertyAccountSummary updatePropertyAccount(
      @PathVariable Long propertyId,
      @Valid @RequestBody UpdatePropertyAccountRequest request
  ) {
    return adminService.updatePropertyAccount(propertyId, request);
  }

  @PostMapping("/properties/{propertyId}/renew")
  public PropertyAccountSummary renewPropertySubscription(@PathVariable Long propertyId) {
    return adminService.renewPropertySubscription(propertyId);
  }

  @PostMapping("/properties/{propertyId}/suspend")
  public PropertyAccountSummary suspendPropertySubscription(@PathVariable Long propertyId) {
    return adminService.suspendPropertySubscription(propertyId);
  }

  @PostMapping("/properties/{propertyId}/reactivate")
  public PropertyAccountSummary reactivatePropertySubscription(@PathVariable Long propertyId) {
    return adminService.reactivatePropertySubscription(propertyId);
  }
}
