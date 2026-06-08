package com.example.pms.backendpms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class DashboardDtos {

  private DashboardDtos() {}

  public record ReservationMovementItem(
      Long reservationId,
      String reservationNumber,
      String guestName,
      String roomNumbers,
      LocalDate date,
      String status,
      String paymentStatus,
      Double balanceAmount
  ) {}

  public record RoomStatusSummary(
      int totalRooms,
      int sellableRooms,
      int available,
      int assigned,
      int checkedIn,
      int checkingOut,
      int maintenance,
      int outOfOrder
  ) {}

  public record RevenueSnapshot(
      double revenueToday,
      double monthlyRevenue,
      double outstandingBalance,
      int pendingPayments,
      int partialPayments,
      int paidReservations
  ) {}

  public record TrendPoint(
      LocalDate date,
      String label,
      double value
  ) {}

  public record ActivityItem(
      Long auditLogId,
      String module,
      String action,
      String description,
      String actorName,
      LocalDateTime createdAt
  ) {}

  public record DashboardResponse(
      Long propertyId,
      String propertyName,
      LocalDate businessDate,
      int guests,
      int occupied,
      int available,
      int maintenance,
      int complimentary,
      double occupancyPercent,
      double adrToday,
      double revparToday,
      int arrivalsToday,
      int departuresToday,
      int dueIn,
      int dueOut,
      int inHouse,
      int noShowCount,
      int cancellationCount,
      RoomStatusSummary roomStatus,
      RevenueSnapshot revenue,
      List<TrendPoint> arrivalTrend,
      List<TrendPoint> occupancyTrend,
      List<ReservationMovementItem> arrivals,
      List<ReservationMovementItem> departures,
      List<ReservationMovementItem> upcomingReservations,
      List<ReservationMovementItem> unpaidReservations,
      List<ActivityItem> recentActivity
  ) {}
}
