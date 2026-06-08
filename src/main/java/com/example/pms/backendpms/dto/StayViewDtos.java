package com.example.pms.backendpms.dto;

import java.time.LocalDate;
import java.util.List;

public final class StayViewDtos {

  private StayViewDtos() {}

  public record StayDateResponse(
      LocalDate iso,
      String dayLabel,
      String dayName,
      String monthLabel,
      boolean focusDate
  ) {}

  public record StaySummaryResponse(
      int guests,
      int occupied,
      int available,
      int complimentary,
      int maintenance
  ) {}

  public record LegendItemResponse(
      String key,
      String label
  ) {}

  public record BookingBlockResponse(
      Long reservationId,
      String reservationNumber,
      String guestName,
      String roomNumber,
      String roomType,
      String status,
      LocalDate checkInDate,
      LocalDate checkOutDate,
      int guests,
      int startColumn,
      int span,
      boolean startsBeforeRange,
      boolean endsAfterRange
  ) {}

  public record RoomRowResponse(
      Long roomId,
      String roomNumber,
      String indicator,
      String state,
      String housekeepingStatus,
      List<BookingBlockResponse> bookings
  ) {}

  public record RoomGroupResponse(
      String name,
      List<Integer> dailyAvailability,
      List<RoomRowResponse> rooms
  ) {}

  public record StayViewResponse(
      Long propertyId,
      String propertyName,
      LocalDate focusDate,
      List<LegendItemResponse> legend,
      List<StayDateResponse> dates,
      StaySummaryResponse summary,
      List<RoomGroupResponse> roomGroups,
      List<Integer> footerAvailability
  ) {}
}
