package com.example.pms.backendpms.dto;

import com.example.pms.backendpms.model.SubscriptionStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AuthDtos {

  private AuthDtos() {}

  public record LoginRequest(
      @NotBlank String identifier,
      @NotBlank String password
  ) {}

  public record ForgotPasswordRequest(
      @NotBlank @Email String email
  ) {}

  public record ResetPasswordRequest(
      @NotBlank String token,
      @NotBlank @Size(min = 8, max = 120) String newPassword,
      @NotBlank @Size(min = 8, max = 120) String confirmPassword
  ) {}

  public record MessageResponse(
      String message
  ) {}

  public record AccessibleProperty(
      Long propertyId,
      String propertyName,
      String propertyCode,
      Boolean propertyActive,
      SubscriptionStatus subscriptionStatus,
      LocalDate renewalDate
  ) {}

  public record SessionResponse(
      Long userId,
      String fullName,
      String role,
      String email,
      String phone,
      Long organizationId,
      List<AccessibleProperty> properties,
      String accessToken,
      Instant accessTokenExpiresAt
  ) {}
}
