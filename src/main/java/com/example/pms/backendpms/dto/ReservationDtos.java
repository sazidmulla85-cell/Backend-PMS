package com.example.pms.backendpms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReservationDtos {

  private ReservationDtos() {}

  public record GuestRequest(
      @NotBlank String fullName,
      String email,
      @NotBlank String phone,
      String country,
      String state,
      String city,
      String postalCode,
      String address,
      String idType,
      String idNumber,
      String notes
  ) {}

  public record AssignedRoomRequest(
      @NotNull Long roomTypeId,
      Long roomId,
      @NotNull @PositiveOrZero Double nightlyRate
  ) {}

  public record CreateReservationRequest(
      @NotBlank String source,
      @NotBlank String status,
      @NotNull LocalDate checkInDate,
      @NotNull LocalDate checkOutDate,
      @NotNull @Positive Integer adults,
      Integer children,
      String mealPlan,
      String ratePlan,
      String specialRequests,
      @NotNull @PositiveOrZero Double roomAmount,
      Double taxAmount,
      Double discountAmount,
      String bookedByPhone,
      String billToType,
      String paymentMethod,
      Long companyId,
      Boolean complimentary,
      Boolean groupBooking,
      String groupCode,
      @Valid @NotNull GuestRequest guest,
      @Valid @NotEmpty List<AssignedRoomRequest> rooms
  ) {}

  public record ReservationActionRequest(
      String actedByPhone,
      String notes
  ) {}

  public record RecordPaymentRequest(
      @NotNull @Positive Double amount,
      @NotBlank String paymentMethod,
      String referenceNumber,
      String notes,
      String receivedByPhone
  ) {}

  public record AddChargeRequest(
      @NotBlank String description,
      @NotNull @Positive Double amount,
      String createdByPhone
  ) {}

  public record ExchangeRoomRequest(
      @NotNull Long roomId,
      String actedByPhone,
      String notes
  ) {}

  public record PaymentResponse(
      Long paymentId,
      Double amount,
      String paymentMethod,
      String status,
      String referenceNumber,
      LocalDateTime paymentDate
  ) {}

  public record ChargeResponse(
      Long chargeId,
      String description,
      Double amount,
      LocalDateTime chargeDate
  ) {}

  public record GuestDocumentResponse(
      Long documentId,
      Long guestId,
      String guestName,
      String documentType,
      String fileName,
      String contentType,
      Long fileSize,
      LocalDateTime uploadedAt,
      String downloadUrl
  ) {}

  public record RoomAssignmentResponse(
      Long roomId,
      String roomNumber,
      String roomTypeName,
      Double nightlyRate
  ) {}

  public record ReservationSummaryResponse(
      Long reservationId,
      String reservationNumber,
      String guestName,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      String status,
      String source,
      Double totalAmount,
      String paymentStatus,
      Boolean complimentary,
      Boolean groupBooking,
      String companyName,
      List<String> roomNumbers
  ) {}

  public record ReservationDetailResponse(
      Long reservationId,
      String reservationNumber,
      String guestName,
      String guestPhone,
      String guestEmail,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      Integer nights,
      Integer adults,
      Integer children,
      String status,
      String source,
      LocalDate bookingDate,
      String billToType,
      String paymentMethod,
      Long companyId,
      String companyName,
      Boolean complimentary,
      Boolean groupBooking,
      String groupCode,
      String mealPlan,
      String ratePlan,
      String specialRequests,
      Double roomAmount,
      Double taxAmount,
      Double discountAmount,
      Double totalAmount,
      Double paidAmount,
      Double balanceAmount,
      String paymentStatus,
      List<String> roomNumbers,
      List<RoomAssignmentResponse> roomAssignments,
      List<PaymentResponse> payments,
      List<ChargeResponse> charges,
      List<GuestDocumentResponse> documents
  ) {}
}
