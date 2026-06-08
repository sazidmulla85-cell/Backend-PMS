package com.example.pms.backendpms.dto;

import com.example.pms.backendpms.model.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

  private AdminDtos() {}

  public record RoomTypeBlueprintRequest(
      @NotBlank String name,
      @NotBlank String code,
      @NotNull @Positive Integer baseOccupancy,
      @NotNull @Positive Integer maxOccupancy,
      @NotNull @Positive Double baseRate,
      @NotEmpty List<@NotBlank String> roomNumbers
  ) {}

  public record CreatePropertyAccountRequest(
      @NotBlank String organizationName,
      String legalName,
      @NotBlank String ownerFullName,
      String ownerEmail,
      @NotBlank String ownerPhone,
      @NotBlank String ownerPassword,
      @NotBlank String propertyName,
      @NotBlank String propertyCode,
      String propertyEmail,
      String propertyPhone,
      @NotBlank String city,
      @NotBlank String state,
      @NotBlank String country,
      String timezone,
      String currencyCode,
      @NotNull @Positive Integer subscribedRoomCount,
      @Valid @NotEmpty List<RoomTypeBlueprintRequest> roomTypes
  ) {}

  public record PropertyAccountSummary(
      Long propertyId,
      String propertyName,
      String propertyCode,
      String propertyEmail,
      String propertyPhone,
      Long ownerUserId,
      String ownerName,
      String ownerPhone,
      String ownerEmail,
      Boolean propertyActive,
      Boolean ownerActive,
      Integer subscribedRoomCount,
      Long actualRoomCount,
      String city,
      String state,
      String country,
      String timezone,
      String currencyCode,
      String subscriptionPlan,
      SubscriptionStatus subscriptionStatus,
      LocalDate subscriptionStartDate,
      LocalDate renewalDate,
      Double monthlySubscriptionAmount,
      Boolean autoRenew
  ) {}

  public record UpdatePropertyAccountRequest(
      @NotBlank String ownerFullName,
      String ownerEmail,
      @NotBlank String ownerPhone,
      String ownerPassword,
      @NotBlank String propertyName,
      String propertyEmail,
      String propertyPhone,
      @NotBlank String city,
      @NotBlank String state,
      @NotBlank String country,
      String timezone,
      String currencyCode,
      @NotNull @Positive Integer subscribedRoomCount,
      @NotBlank String subscriptionPlan,
      @NotNull SubscriptionStatus subscriptionStatus,
      @NotNull LocalDate subscriptionStartDate,
      @NotNull LocalDate renewalDate,
      @NotNull @Positive Double monthlySubscriptionAmount,
      @NotNull Boolean autoRenew,
      @NotNull Boolean propertyActive,
      @NotNull Boolean ownerActive
  ) {}

  public record AdminOverviewResponse(
      long superAdminCount,
      long hotelOwnerCount,
      long activePropertyCount,
      List<PropertyAccountSummary> properties
  ) {}
}
