package com.example.pms.backendpms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;

public final class RoomDtos {

  private RoomDtos() {}

  public record RoomSummaryResponse(
      int guests,
      int assigned,
      int checkedIn,
      int checkingOut,
      int available,
      int maintenance,
      int outOfOrder
  ) {}

  public record RoomResponse(
      Long roomId,
      Long reservationId,
      String roomNumber,
      String roomTypeName,
      String status,
      String occupancyStatus,
      String housekeepingStatus,
      String currentGuestName,
      String reservationNumber,
      LocalDate checkInDate,
      LocalDate checkOutDate
  ) {}

  public record RoomTypeGroupResponse(
      Long roomTypeId,
      String name,
      Double baseRate,
      RoomSummaryResponse summary,
      List<RoomResponse> rooms
  ) {}

  public record RoomsViewResponse(
      Long propertyId,
      String propertyName,
      LocalDate businessDate,
      RoomSummaryResponse summary,
      List<RoomTypeGroupResponse> roomTypes
  ) {}

  public record CreateRoomTypeRequest(
      @NotBlank String name,
      @NotBlank String code,
      @NotNull @Positive Integer baseOccupancy,
      @NotNull @Positive Integer maxOccupancy,
      @NotNull @Positive Double baseRate
  ) {}

  public record CreateRoomRequest(
      @NotNull Long roomTypeId,
      @NotBlank String roomNumber,
      String floorName,
      String status,
      String housekeepingStatus
  ) {}

  public record UpdateRoomStatusRequest(
      @NotBlank String status,
      String housekeepingStatus,
      String notes
  ) {}
}
