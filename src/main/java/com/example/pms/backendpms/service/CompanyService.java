package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.CompanyDtos.CompanySummaryResponse;
import com.example.pms.backendpms.dto.CompanyDtos.CreateCompanyRequest;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.Company;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.Reservation;
import com.example.pms.backendpms.repository.CompanyRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

  private final PropertyService propertyService;
  private final CompanyRepository companyRepository;
  private final ReservationRepository reservationRepository;
  private final AuditLogService auditLogService;

  public CompanyService(
      PropertyService propertyService,
      CompanyRepository companyRepository,
      ReservationRepository reservationRepository,
      AuditLogService auditLogService
  ) {
    this.propertyService = propertyService;
    this.companyRepository = companyRepository;
    this.reservationRepository = reservationRepository;
    this.auditLogService = auditLogService;
  }

  public List<CompanySummaryResponse> getCompanies(Long propertyId) {
    List<Reservation> reservations = reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId);

    return companyRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
        .map(company -> toSummary(company, reservations))
        .toList();
  }

  public Company getCompanyEntity(Long propertyId, Long companyId) {
    return companyRepository.findByIdAndPropertyId(companyId, propertyId)
        .orElseThrow(() -> new NotFoundException("Company not found for id " + companyId));
  }

  @Transactional
  public CompanySummaryResponse createCompany(Long propertyId, CreateCompanyRequest request) {
    Property property = propertyService.getPropertyEntity(propertyId);

    Company company = new Company();
    company.setProperty(property);
    company.setName(request.name().trim());
    company.setGstVat(blankToNull(request.gstVat()));
    company.setContactPerson(blankToNull(request.contactPerson()));
    company.setPhone(blankToNull(request.phone()));
    company.setEmail(blankToNull(request.email()));
    company.setCity(blankToNull(request.city()));
    company.setBillingAddress(blankToNull(request.billingAddress()));
    company = companyRepository.save(company);

    auditLogService.log(
        null,
        property,
        AuditModule.ADMIN,
        AuditAction.CREATE,
        "Company",
        String.valueOf(company.getId()),
        "Created company ledger " + company.getName()
    );

    return toSummary(company, List.of());
  }

  private CompanySummaryResponse toSummary(Company company, List<Reservation> reservations) {
    BigDecimal totalBilled = BigDecimal.ZERO;
    BigDecimal totalOutstanding = BigDecimal.ZERO;
    long reservationCount = 0;

    for (Reservation reservation : reservations) {
      if (reservation.getCompany() == null || !reservation.getCompany().getId().equals(company.getId())) {
        continue;
      }

      reservationCount++;
      totalBilled = totalBilled.add(safeAmount(reservation.getTotalAmount()));
      totalOutstanding = totalOutstanding.add(safeAmount(reservation.getBalanceAmount()));
    }

    return new CompanySummaryResponse(
        company.getId(),
        company.getName(),
        company.getGstVat(),
        company.getContactPerson(),
        company.getPhone(),
        company.getEmail(),
        company.getCity(),
        totalBilled.doubleValue(),
        totalOutstanding.doubleValue(),
        reservationCount
    );
  }

  private BigDecimal safeAmount(BigDecimal amount) {
    return amount != null ? amount : BigDecimal.ZERO;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
