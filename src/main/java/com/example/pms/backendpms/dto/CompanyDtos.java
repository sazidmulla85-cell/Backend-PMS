package com.example.pms.backendpms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class CompanyDtos {

  private CompanyDtos() {}

  public record CreateCompanyRequest(
      @NotBlank String name,
      String gstVat,
      String contactPerson,
      String phone,
      @Email String email,
      String city,
      String billingAddress
  ) {}

  public record CompanySummaryResponse(
      Long companyId,
      String name,
      String gstVat,
      String contactPerson,
      String phone,
      String email,
      String city,
      Double totalBilled,
      Double totalOutstanding,
      Long reservationsCount
  ) {}
}
