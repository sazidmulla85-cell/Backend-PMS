package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.CompanyDtos.CompanySummaryResponse;
import com.example.pms.backendpms.dto.CompanyDtos.CreateCompanyRequest;
import com.example.pms.backendpms.service.CompanyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/companies")
public class CompanyController {

  private final CompanyService companyService;

  public CompanyController(CompanyService companyService) {
    this.companyService = companyService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public List<CompanySummaryResponse> companies(@PathVariable Long propertyId) {
    return companyService.getCompanies(propertyId);
  }

  @PostMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public CompanySummaryResponse createCompany(
      @PathVariable Long propertyId,
      @Valid @RequestBody CreateCompanyRequest request
  ) {
    return companyService.createCompany(propertyId, request);
  }
}
