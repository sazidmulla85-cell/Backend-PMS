package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.PlatformAdminDtos.AuditTimelineItem;
import com.example.pms.backendpms.dto.PlatformAdminDtos.CreatePlatformInvoiceRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.CreatePropertyCommunicationRequest;
import com.example.pms.backendpms.dto.PlatformAdminDtos.MetricBreakdown;
import com.example.pms.backendpms.dto.PlatformAdminDtos.PlatformInvoicePaymentResponse;
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
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditLog;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.CommunicationChannel;
import com.example.pms.backendpms.model.CommunicationStatus;
import com.example.pms.backendpms.model.CrmStage;
import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.PlatformInvoice;
import com.example.pms.backendpms.model.PlatformInvoicePayment;
import com.example.pms.backendpms.model.PlatformInvoiceStatus;
import com.example.pms.backendpms.model.PlatformPlan;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.PropertyCommunicationLog;
import com.example.pms.backendpms.model.SubscriptionStatus;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.AuditLogRepository;
import com.example.pms.backendpms.repository.CompanyRepository;
import com.example.pms.backendpms.repository.PlatformInvoicePaymentRepository;
import com.example.pms.backendpms.repository.PlatformInvoiceRepository;
import com.example.pms.backendpms.repository.PlatformPlanRepository;
import com.example.pms.backendpms.repository.PropertyCommunicationLogRepository;
import com.example.pms.backendpms.repository.PropertyRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlatformAdminService {

  private static final List<String> DEFAULT_MODULES = List.of(
      "Dashboard",
      "Stay View",
      "Rooms",
      "Reservations",
      "Companies",
      "Audit Logs"
  );

  private final PlatformPlanRepository platformPlanRepository;
  private final PlatformInvoiceRepository platformInvoiceRepository;
  private final PlatformInvoicePaymentRepository platformInvoicePaymentRepository;
  private final PropertyCommunicationLogRepository propertyCommunicationLogRepository;
  private final PropertyRepository propertyRepository;
  private final UserAccountRepository userAccountRepository;
  private final RoomRepository roomRepository;
  private final ReservationRepository reservationRepository;
  private final CompanyRepository companyRepository;
  private final AuditLogRepository auditLogRepository;
  private final AuditLogService auditLogService;
  private final SubscriptionLifecycleService subscriptionLifecycleService;

  public PlatformAdminService(
      PlatformPlanRepository platformPlanRepository,
      PlatformInvoiceRepository platformInvoiceRepository,
      PlatformInvoicePaymentRepository platformInvoicePaymentRepository,
      PropertyCommunicationLogRepository propertyCommunicationLogRepository,
      PropertyRepository propertyRepository,
      UserAccountRepository userAccountRepository,
      RoomRepository roomRepository,
      ReservationRepository reservationRepository,
      CompanyRepository companyRepository,
      AuditLogRepository auditLogRepository,
      AuditLogService auditLogService,
      SubscriptionLifecycleService subscriptionLifecycleService
  ) {
    this.platformPlanRepository = platformPlanRepository;
    this.platformInvoiceRepository = platformInvoiceRepository;
    this.platformInvoicePaymentRepository = platformInvoicePaymentRepository;
    this.propertyCommunicationLogRepository = propertyCommunicationLogRepository;
    this.propertyRepository = propertyRepository;
    this.userAccountRepository = userAccountRepository;
    this.roomRepository = roomRepository;
    this.reservationRepository = reservationRepository;
    this.companyRepository = companyRepository;
    this.auditLogRepository = auditLogRepository;
    this.auditLogService = auditLogService;
    this.subscriptionLifecycleService = subscriptionLifecycleService;
  }

  @Transactional
  public List<PlatformPlanResponse> getPlans() {
    return platformPlanRepository.findAll().stream()
        .sorted(Comparator.comparing(PlatformPlan::getIncludedRooms))
        .map(this::toPlanResponse)
        .toList();
  }

  @Transactional
  public PlatformPlanResponse updatePlan(Long planId, UpdatePlatformPlanRequest request) {
    PlatformPlan plan = platformPlanRepository.findById(planId)
        .orElseThrow(() -> new NotFoundException("Platform plan not found for id " + planId));

    plan.setName(request.name().trim());
    plan.setIncludedRooms(request.includedRooms());
    plan.setBaseMonthlyAmount(request.baseMonthlyAmount());
    plan.setPerRoomAmount(request.perRoomAmount());
    plan.setDescription(blankToNull(request.description()));
    plan.setModuleCodesCsv(String.join(",", normalizeModules(request.modules())));
    plan.setActive(Boolean.TRUE.equals(request.active()));
    plan = platformPlanRepository.save(plan);

    logPlatformAction(
        null,
        AuditAction.UPDATE,
        "PlatformPlan",
        String.valueOf(plan.getId()),
        "Updated platform plan " + plan.getName()
    );

    return toPlanResponse(plan);
  }

  @Transactional
  public PlatformReportResponse getReports() {
    List<Property> properties = propertyRepository.findAllByOrderByNameAsc();
    properties.forEach(this::refreshSubscriptionState);

    List<PlatformInvoice> invoices = platformInvoiceRepository.findAllByOrderByBillingMonthDesc();
    Map<String, Long> crmBreakdown = new LinkedHashMap<>();
    Map<String, Long> planBreakdown = new LinkedHashMap<>();
    LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
    long noRecentLoginCount = 0;
    double platformMrr = 0D;
    double collectedThisMonth = 0D;
    double outstandingInvoiced = 0D;
    long pendingInvoices = 0;
    long partialInvoices = 0;
    long paidInvoices = 0;

    for (Property property : properties) {
      crmBreakdown.merge(property.getCrmStage().name(), 1L, Long::sum);
      planBreakdown.merge(resolvePlanLabel(property), 1L, Long::sum);
      platformMrr += valueOrZero(property.getMonthlySubscriptionAmount());

      LocalDateTime lastLoginAt = property.getOwner() != null ? property.getOwner().getLastLoginAt() : null;
      if (lastLoginAt == null || lastLoginAt.toLocalDate().isBefore(thirtyDaysAgo)) {
        noRecentLoginCount++;
      }
    }

    for (PlatformInvoice invoice : invoices) {
      outstandingInvoiced += Math.max(invoice.getTotalAmount() - valueOrZero(invoice.getPaidAmount()), 0D);
      if (invoice.getStatus() == PlatformInvoiceStatus.PAID) {
        paidInvoices++;
      } else if (invoice.getStatus() == PlatformInvoiceStatus.PARTIAL) {
        partialInvoices++;
      } else if (invoice.getStatus() == PlatformInvoiceStatus.PENDING || invoice.getStatus() == PlatformInvoiceStatus.OVERDUE) {
        pendingInvoices++;
      }
    }

    for (PlatformInvoicePayment payment : platformInvoicePaymentRepository.findAllByOrderByReceivedAtDesc()) {
      if (payment.getReceivedAt() != null
          && payment.getReceivedAt().getYear() == LocalDate.now().getYear()
          && payment.getReceivedAt().getMonth() == LocalDate.now().getMonth()) {
        collectedThisMonth += valueOrZero(payment.getAmount());
      }
    }

    return new PlatformReportResponse(
        properties.size(),
        properties.stream().filter(Property::isActive).count(),
        properties.stream().filter(property -> property.getSubscriptionStatus() == SubscriptionStatus.DUE_SOON).count(),
        properties.stream().filter(property -> property.getSubscriptionStatus() == SubscriptionStatus.OVERDUE).count(),
        properties.stream().filter(property -> property.getSubscriptionStatus() == SubscriptionStatus.SUSPENDED).count(),
        properties.stream().filter(property -> property.getCrmStage() == CrmStage.CHURNED).count(),
        noRecentLoginCount,
        platformMrr,
        collectedThisMonth,
        outstandingInvoiced,
        pendingInvoices,
        paidInvoices,
        partialInvoices,
        crmBreakdown.entrySet().stream().map(entry -> new MetricBreakdown(humanize(entry.getKey()), entry.getValue())).toList(),
        planBreakdown.entrySet().stream().map(entry -> new MetricBreakdown(entry.getKey(), entry.getValue())).toList()
    );
  }

  @Transactional
  public List<SupportPropertySummary> getSupportPortfolio() {
    return propertyRepository.findAllByOrderByNameAsc().stream()
        .map(this::toSupportSummary)
        .toList();
  }

  @Transactional
  public SupportPropertyDetailResponse getSupportPropertyDetail(Long propertyId) {
    Property property = getProperty(propertyId);
    SupportPropertySummary summary = toSupportSummary(property);

    List<AuditTimelineItem> recentAudits = auditLogRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
        .limit(12)
        .map(this::toAuditTimelineItem)
        .toList();

    List<PropertyCommunicationResponse> communications = propertyCommunicationLogRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
        .map(this::toCommunicationResponse)
        .toList();

    List<PlatformInvoiceResponse> invoices = platformInvoiceRepository.findByPropertyIdOrderByBillingMonthDesc(propertyId).stream()
        .map(this::toInvoiceResponse)
        .toList();

    List<String> checklist = buildChecklist(property);

    return new SupportPropertyDetailResponse(
        summary,
        checklist,
        recentAudits,
        communications,
        invoices
    );
  }

  @Transactional
  public SupportPropertySummary updateSupportProperty(Long propertyId, UpdateSupportPropertyRequest request) {
    Property property = getProperty(propertyId);
    Organization organization = property.getOrganization();

    property.setCrmStage(CrmStage.valueOf(request.crmStage().trim().toUpperCase(Locale.ROOT)));
    property.setAccountManager(blankToNull(request.accountManager()));
    property.setSupportNotes(blankToNull(request.supportNotes()));
    property.setCommercialNotes(blankToNull(request.commercialNotes()));
    property.setModuleEntitlementsCsv(String.join(",", normalizeModules(request.enabledModules())));
    propertyRepository.save(property);

    organization.setLegalName(blankToNull(request.legalName()));
    organization.setGstNumber(blankToNull(request.gstNumber()));
    organization.setBillingEmail(blankToNull(request.billingEmail()));
    organization.setBillingPhone(blankToNull(request.billingPhone()));
    organization.setBillingAddress(blankToNull(request.billingAddress()));

    logPlatformAction(
        property,
        AuditAction.UPDATE,
        "PropertySupportProfile",
        String.valueOf(property.getId()),
        "Updated support, CRM, commercial, and entitlement settings for " + property.getName()
    );

    return toSupportSummary(property);
  }

  @Transactional
  public SupportOpenResponse openSupportView(Long propertyId) {
    Property property = getProperty(propertyId);
    logPlatformAction(
        property,
        AuditAction.VIEW,
        "SupportWorkspace",
        String.valueOf(propertyId),
        "Opened support workspace for " + property.getName()
    );

    return new SupportOpenResponse(
        property.getId(),
        property.getName(),
        property.getOwner().getFullName(),
        "Support context opened in read-only mode for " + property.getName()
    );
  }

  @Transactional
  public List<PropertyCommunicationResponse> getCommunications() {
    return propertyCommunicationLogRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toCommunicationResponse)
        .toList();
  }

  @Transactional
  public PropertyCommunicationResponse createCommunication(CreatePropertyCommunicationRequest request) {
    Property property = getProperty(request.propertyId());

    PropertyCommunicationLog communicationLog = new PropertyCommunicationLog();
    communicationLog.setProperty(property);
    communicationLog.setChannel(CommunicationChannel.valueOf(request.channel().trim().toUpperCase(Locale.ROOT)));
    communicationLog.setStatus(CommunicationStatus.SENT);
    communicationLog.setSubject(request.subject().trim());
    communicationLog.setMessage(request.message().trim());
    communicationLog.setActorName(blankToNull(request.actorName()) != null ? request.actorName().trim() : "Super Admin");
    communicationLog = propertyCommunicationLogRepository.save(communicationLog);

    logPlatformAction(
        property,
        AuditAction.CREATE,
        "PropertyCommunication",
        String.valueOf(communicationLog.getId()),
        "Logged " + communicationLog.getChannel().name() + " communication for " + property.getName()
    );

    return toCommunicationResponse(communicationLog);
  }

  @Transactional
  public List<PlatformInvoiceResponse> getLedger() {
    return platformInvoiceRepository.findAllByOrderByBillingMonthDesc().stream()
        .map(this::toInvoiceResponse)
        .toList();
  }

  @Transactional
  public PlatformInvoiceResponse createInvoice(Long propertyId, CreatePlatformInvoiceRequest request) {
    Property property = getProperty(propertyId);

    PlatformInvoice invoice = new PlatformInvoice();
    invoice.setProperty(property);
    invoice.setInvoiceNumber(nextInvoiceNumber(property));
    invoice.setPlanName(resolvePlanLabel(property));
    invoice.setBillingMonth(request.billingMonth());
    invoice.setDueDate(request.dueDate());
    invoice.setTotalAmount(request.totalAmount());
    invoice.setPaidAmount(0D);
    invoice.setStatus(resolveInvoiceStatus(0D, request.totalAmount(), request.dueDate()));
    invoice.setNotes(blankToNull(request.notes()));
    invoice = platformInvoiceRepository.save(invoice);

    logPlatformAction(
        property,
        AuditAction.CREATE,
        "PlatformInvoice",
        String.valueOf(invoice.getId()),
        "Created billing invoice " + invoice.getInvoiceNumber() + " for " + property.getName()
    );

    return toInvoiceResponse(invoice);
  }

  @Transactional
  public PlatformInvoiceResponse recordInvoicePayment(Long invoiceId, RecordPlatformInvoicePaymentRequest request) {
    PlatformInvoice invoice = platformInvoiceRepository.findById(invoiceId)
        .orElseThrow(() -> new NotFoundException("Platform invoice not found for id " + invoiceId));

    PlatformInvoicePayment payment = new PlatformInvoicePayment();
    payment.setInvoice(invoice);
    payment.setAmount(request.amount());
    payment.setPaymentMethod(request.paymentMethod().trim());
    payment.setReferenceNumber(blankToNull(request.referenceNumber()));
    payment.setReceivedAt(LocalDateTime.now());
    platformInvoicePaymentRepository.save(payment);

    double updatedPaidAmount = valueOrZero(invoice.getPaidAmount()) + request.amount();
    invoice.setPaidAmount(updatedPaidAmount);
    invoice.setStatus(resolveInvoiceStatus(updatedPaidAmount, invoice.getTotalAmount(), invoice.getDueDate()));
    invoice = platformInvoiceRepository.save(invoice);

    logPlatformAction(
        invoice.getProperty(),
        AuditAction.UPDATE,
        "PlatformInvoice",
        String.valueOf(invoice.getId()),
        "Recorded platform billing payment against " + invoice.getInvoiceNumber()
    );

    return toInvoiceResponse(invoice);
  }

  private SupportPropertySummary toSupportSummary(Property property) {
    refreshSubscriptionState(property);
    Organization organization = property.getOrganization();
    List<String> checklist = buildChecklist(property);
    int checklistTotal = checklist.size();
    int checklistCompleted = (int) checklist.stream().filter(item -> item.startsWith("[Done]")).count();
    int completion = checklistTotal == 0 ? 0 : Math.round((checklistCompleted * 100F) / checklistTotal);

    return new SupportPropertySummary(
        property.getId(),
        property.getName(),
        property.getCode(),
        property.getOwner().getFullName(),
        property.getOwner().getPhone(),
        property.getOwner().getEmail(),
        property.getOwner().getLastLoginAt(),
        property.getCrmStage().name(),
        property.getAccountManager(),
        completion,
        checklistCompleted,
        checklistTotal,
        parseModules(property.getModuleEntitlementsCsv()),
        property.getSupportNotes(),
        property.getCommercialNotes(),
        organization.getLegalName(),
        organization.getGstNumber(),
        organization.getBillingEmail(),
        organization.getBillingPhone(),
        organization.getBillingAddress(),
        roomRepository.countByPropertyId(property.getId()),
        reservationRepository.countByPropertyId(property.getId()),
        companyRepository.countByPropertyId(property.getId())
    );
  }

  private List<String> buildChecklist(Property property) {
    List<String> checklist = new ArrayList<>();
    checklist.add(markChecklist(property.isActive(), "Property is active"));
    checklist.add(markChecklist(property.getOwner() != null && property.getOwner().isActive(), "Owner login is active"));
    checklist.add(markChecklist(roomRepository.countByPropertyId(property.getId()) > 0, "Rooms are configured"));
    checklist.add(markChecklist(reservationRepository.countByPropertyId(property.getId()) > 0, "Reservations exist"));
    checklist.add(markChecklist(companyRepository.countByPropertyId(property.getId()) > 0, "Company ledger exists"));
    checklist.add(markChecklist(!auditLogRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId()).isEmpty(), "Audit activity exists"));
    checklist.add(markChecklist(property.getSubscriptionPlan() != null, "Subscription plan is set"));
    checklist.add(markChecklist(property.getOrganization().getLegalName() != null, "Commercial/legal profile is filled"));
    checklist.add(markChecklist(property.getOrganization().getBillingEmail() != null, "Billing contact is configured"));
    checklist.add(markChecklist(!platformInvoiceRepository.findByPropertyIdOrderByBillingMonthDesc(property.getId()).isEmpty(), "Platform billing ledger has started"));
    return checklist;
  }

  private AuditTimelineItem toAuditTimelineItem(AuditLog auditLog) {
    return new AuditTimelineItem(
        auditLog.getId(),
        auditLog.getDescription(),
        auditLog.getModule().name(),
        auditLog.getAction().name(),
        auditLog.getCreatedAt()
    );
  }

  private PlatformInvoiceResponse toInvoiceResponse(PlatformInvoice invoice) {
    double totalAmount = valueOrZero(invoice.getTotalAmount());
    double paidAmount = valueOrZero(invoice.getPaidAmount());
    return new PlatformInvoiceResponse(
        invoice.getId(),
        invoice.getProperty().getId(),
        invoice.getProperty().getName(),
        invoice.getInvoiceNumber(),
        invoice.getPlanName(),
        invoice.getBillingMonth(),
        invoice.getDueDate(),
        totalAmount,
        paidAmount,
        Math.max(totalAmount - paidAmount, 0D),
        invoice.getStatus().name(),
        platformInvoicePaymentRepository.findByInvoiceIdOrderByReceivedAtDesc(invoice.getId()).stream()
            .map(payment -> new PlatformInvoicePaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getReceivedAt()
            ))
            .toList()
    );
  }

  private PropertyCommunicationResponse toCommunicationResponse(PropertyCommunicationLog communicationLog) {
    return new PropertyCommunicationResponse(
        communicationLog.getId(),
        communicationLog.getProperty().getId(),
        communicationLog.getProperty().getName(),
        communicationLog.getChannel().name(),
        communicationLog.getStatus().name(),
        communicationLog.getSubject(),
        communicationLog.getMessage(),
        communicationLog.getActorName(),
        communicationLog.getCreatedAt()
    );
  }

  private PlatformPlanResponse toPlanResponse(PlatformPlan plan) {
    return new PlatformPlanResponse(
        plan.getId(),
        plan.getCode(),
        plan.getName(),
        plan.getIncludedRooms(),
        plan.getBaseMonthlyAmount(),
        plan.getPerRoomAmount(),
        plan.getDescription(),
        parseModules(plan.getModuleCodesCsv()),
        plan.isActive()
    );
  }

  private Property getProperty(Long propertyId) {
    return propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));
  }

  private void refreshSubscriptionState(Property property) {
    if (subscriptionLifecycleService.refreshStatus(property)) {
      propertyRepository.save(property);
    }
  }

  private void logPlatformAction(
      Property property,
      AuditAction action,
      String entityType,
      String entityId,
      String description
  ) {
    UserAccount actor = userAccountRepository.findByRole(UserRole.SUPER_ADMIN).stream().findFirst().orElse(null);
    auditLogService.log(actor, property, AuditModule.ADMIN, action, entityType, entityId, description);
  }

  private List<String> parseModules(String csv) {
    if (csv == null || csv.isBlank()) {
      return DEFAULT_MODULES;
    }

    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }

  private List<String> normalizeModules(List<String> modules) {
    return modules.stream()
        .map(value -> value == null ? "" : value.trim())
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }

  private String resolvePlanLabel(Property property) {
    return property.getSubscriptionPlan() != null ? property.getSubscriptionPlan() : "Starter";
  }

  private double valueOrZero(Double value) {
    return value != null ? value : 0D;
  }

  private PlatformInvoiceStatus resolveInvoiceStatus(double paidAmount, double totalAmount, LocalDate dueDate) {
    if (paidAmount >= totalAmount) {
      return PlatformInvoiceStatus.PAID;
    }

    if (paidAmount > 0D) {
      return PlatformInvoiceStatus.PARTIAL;
    }

    if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
      return PlatformInvoiceStatus.OVERDUE;
    }

    return PlatformInvoiceStatus.PENDING;
  }

  private String nextInvoiceNumber(Property property) {
    return "PLT-" + property.getCode() + "-" + (platformInvoiceRepository.count() + 1);
  }

  private String markChecklist(boolean done, String label) {
    return (done ? "[Done] " : "[Pending] ") + label;
  }

  private String humanize(String value) {
    StringBuilder builder = new StringBuilder();
    for (String word : value.replace('_', ' ').toLowerCase(Locale.ROOT).split("\\s+")) {
      if (word.isBlank()) {
        continue;
      }

      if (builder.length() > 0) {
        builder.append(' ');
      }

      builder.append(Character.toUpperCase(word.charAt(0)));
      if (word.length() > 1) {
        builder.append(word.substring(1));
      }
    }
    return builder.toString();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
