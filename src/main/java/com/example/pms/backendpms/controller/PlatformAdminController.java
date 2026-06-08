package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.PlatformAdminDtos.CreatePlatformInvoiceRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.CreatePropertyCommunicationRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.PlatformInvoiceResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.PlatformPlanResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.PlatformReportResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.PropertyCommunicationResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.RecordPlatformInvoicePaymentRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.SupportOpenResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.SupportPropertyDetailResponse;
import com.example.pms.backendpms.dto.PlatformAdminDtos.SupportPropertySummary;
import com.example.pms.backendpms.dto.PlatformAdminDtos.UpdatePlatformPlanRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.UpdateSupportPropertyRequest;
import com.example.pms.backendpms.service.PlatformAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/platform")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformAdminController {

  private final PlatformAdminService platformAdminService;

  public PlatformAdminController(PlatformAdminService platformAdminService) {
    this.platformAdminService = platformAdminService;
  }

  @GetMapping("/plans")
  public List<PlatformPlanResponse> getPlans() {
    return platformAdminService.getPlans();
  }

  @PutMapping("/plans/{planId}")
  public PlatformPlanResponse updatePlan(
      @PathVariable Long planId,
      @Valid @RequestBody UpdatePlatformPlanRequest request
  ) {
    return platformAdminService.updatePlan(planId, request);
  }

  @GetMapping("/reports")
  public PlatformReportResponse getReports() {
    return platformAdminService.getReports();
  }

  @GetMapping("/support")
  public List<SupportPropertySummary> getSupportPortfolio() {
    return platformAdminService.getSupportPortfolio();
  }

  @GetMapping("/support/{propertyId}")
  public SupportPropertyDetailResponse getSupportPropertyDetail(@PathVariable Long propertyId) {
    return platformAdminService.getSupportPropertyDetail(propertyId);
  }

  @PutMapping("/support/{propertyId}")
  public SupportPropertySummary updateSupportProperty(
      @PathVariable Long propertyId,
      @Valid @RequestBody UpdateSupportPropertyRequest request
  ) {
    return platformAdminService.updateSupportProperty(propertyId, request);
  }

  @PostMapping("/support/{propertyId}/open")
  public SupportOpenResponse openSupportView(@PathVariable Long propertyId) {
    return platformAdminService.openSupportView(propertyId);
  }

  @GetMapping("/communications")
  public List<PropertyCommunicationResponse> getCommunications() {
    return platformAdminService.getCommunications();
  }

  @PostMapping("/communications")
  public PropertyCommunicationResponse createCommunication(
      @Valid @RequestBody CreatePropertyCommunicationRequest request
  ) {
    return platformAdminService.createCommunication(request);
  }

  @GetMapping("/ledger")
  public List<PlatformInvoiceResponse> getLedger() {
    return platformAdminService.getLedger();
  }

  @PostMapping("/ledger/properties/{propertyId}/invoice")
  public PlatformInvoiceResponse createInvoice(
      @PathVariable Long propertyId,
      @Valid @RequestBody CreatePlatformInvoiceRequest request
  ) {
    return platformAdminService.createInvoice(propertyId, request);
  }

  @PostMapping("/ledger/invoices/{invoiceId}/payments")
  public PlatformInvoiceResponse recordInvoicePayment(
      @PathVariable Long invoiceId,
      @Valid @RequestBody RecordPlatformInvoicePaymentRequest request
  ) {
    return platformAdminService.recordInvoicePayment(invoiceId, request);
  }
}
