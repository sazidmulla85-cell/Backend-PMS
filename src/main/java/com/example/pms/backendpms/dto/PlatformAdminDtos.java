package com.example.pms.backendpms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PlatformAdminDtos {

  private PlatformAdminDtos() {}

  public record MetricBreakdown(String label, long value) {}

  public record PlatformPlanResponse(
      Long id,
      String code,
      String name,
      Integer includedRooms,
      Double baseMonthlyAmount,
      Double perRoomAmount,
      String description,
      List<String> modules,
      Boolean active
  ) {}

  public record UpdatePlatformPlanRequest(
      @NotBlank String name,
      @NotNull @Positive Integer includedRooms,
      @NotNull @Positive Double baseMonthlyAmount,
      @NotNull @Positive Double perRoomAmount,
      String description,
      @NotEmpty List<@NotBlank String> modules,
      @NotNull Boolean active
  ) {}

  public record PlatformInvoicePaymentResponse(
      Long id,
      Double amount,
      String paymentMethod,
      String referenceNumber,
      LocalDateTime receivedAt
  ) {}

  public record PlatformInvoiceResponse(
      Long id,
      Long propertyId,
      String propertyName,
      String invoiceNumber,
      String planName,
      LocalDate billingMonth,
      LocalDate dueDate,
      Double totalAmount,
      Double paidAmount,
      Double outstandingAmount,
      String status,
      List<PlatformInvoicePaymentResponse> payments
  ) {}

  public record CreatePlatformInvoiceRequest(
      @NotNull LocalDate billingMonth,
      @NotNull LocalDate dueDate,
      @NotNull @Positive Double totalAmount,
      String notes
  ) {}

  public record RecordPlatformInvoicePaymentRequest(
      @NotNull @Positive Double amount,
      @NotBlank String paymentMethod,
      String referenceNumber
  ) {}

  public record PropertyCommunicationResponse(
      Long id,
      Long propertyId,
      String propertyName,
      String channel,
      String status,
      String subject,
      String message,
      String actorName,
      LocalDateTime createdAt
  ) {}

  public record CreatePropertyCommunicationRequest(
      @NotNull Long propertyId,
      @NotBlank String channel,
      @NotBlank String subject,
      @NotBlank String message,
      String actorName
  ) {}

  public record SupportPropertySummary(
      Long propertyId,
      String propertyName,
      String propertyCode,
      String ownerName,
      String ownerPhone,
      String ownerEmail,
      LocalDateTime lastLoginAt,
      String crmStage,
      String accountManager,
      Integer onboardingCompletionPercent,
      Integer checklistCompleted,
      Integer checklistTotal,
      List<String> enabledModules,
      String supportNotes,
      String commercialNotes,
      String legalName,
      String gstNumber,
      String billingEmail,
      String billingPhone,
      String billingAddress,
      Long roomCount,
      Long reservationCount,
      Long companyCount
  ) {}

  public record AuditTimelineItem(
      Long id,
      String description,
      String module,
      String action,
      LocalDateTime createdAt
  ) {}

  public record SupportPropertyDetailResponse(
      SupportPropertySummary summary,
      List<String> onboardingChecklist,
      List<AuditTimelineItem> recentAudits,
      List<PropertyCommunicationResponse> communications,
      List<PlatformInvoiceResponse> invoices
  ) {}

  public record UpdateSupportPropertyRequest(
      @NotBlank String crmStage,
      String accountManager,
      String supportNotes,
      String commercialNotes,
      String legalName,
      String gstNumber,
      String billingEmail,
      String billingPhone,
      String billingAddress,
      @NotEmpty List<@NotBlank String> enabledModules
  ) {}

  public record SupportOpenResponse(
      Long propertyId,
      String propertyName,
      String ownerName,
      String message
  ) {}

  public record PlatformReportResponse(
      long totalProperties,
      long activeProperties,
      long dueSoonProperties,
      long overdueProperties,
      long suspendedProperties,
      long churnedProperties,
      long noRecentLoginCount,
      double platformMrr,
      double collectedThisMonth,
      double outstandingInvoiced,
      long pendingInvoices,
      long paidInvoices,
      long partialInvoices,
      List<MetricBreakdown> crmBreakdown,
      List<MetricBreakdown> planBreakdown
  ) {}
}
