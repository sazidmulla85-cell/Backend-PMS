package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.DashboardDtos.ActivityItem;
import com.example.pms.backendpms.dto.DashboardDtos.DashboardResponse;
import com.example.pms.backendpms.dto.DashboardDtos.ReservationMovementItem;
import com.example.pms.backendpms.dto.DashboardDtos.RevenueSnapshot;
import com.example.pms.backendpms.dto.DashboardDtos.RoomStatusSummary;
import com.example.pms.backendpms.dto.DashboardDtos.TrendPoint;
import com.example.pms.backendpms.model.AuditLog;
import com.example.pms.backendpms.model.Payment;
import com.example.pms.backendpms.model.PaymentStatus;
import com.example.pms.backendpms.model.Reservation;
import com.example.pms.backendpms.model.ReservationRoom;
import com.example.pms.backendpms.model.ReservationStatus;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.repository.AuditLogRepository;
import com.example.pms.backendpms.repository.PaymentRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.ReservationRoomRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

  private final PropertyService propertyService;
  private final ReservationRepository reservationRepository;
  private final ReservationRoomRepository reservationRoomRepository;
  private final RoomRepository roomRepository;
  private final PaymentRepository paymentRepository;
  private final AuditLogRepository auditLogRepository;

  public DashboardService(
      PropertyService propertyService,
      ReservationRepository reservationRepository,
      ReservationRoomRepository reservationRoomRepository,
      RoomRepository roomRepository,
      PaymentRepository paymentRepository,
      AuditLogRepository auditLogRepository
  ) {
    this.propertyService = propertyService;
    this.reservationRepository = reservationRepository;
    this.reservationRoomRepository = reservationRoomRepository;
    this.roomRepository = roomRepository;
    this.paymentRepository = paymentRepository;
    this.auditLogRepository = auditLogRepository;
  }

  public DashboardResponse getDashboard(Long propertyId) {
    var property = propertyService.getPropertyEntity(propertyId);
    LocalDate businessDate = LocalDate.now();

    List<Reservation> reservations = reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId);
    List<Room> rooms = roomRepository.findByPropertyIdOrderByRoomNumberAsc(propertyId);
    List<ReservationRoom> assignments =
        reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(propertyId);
    List<Payment> payments = paymentRepository.findByReservationPropertyIdOrderByPaymentDateDesc(propertyId);
    List<AuditLog> auditLogs = auditLogRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId);

    Map<Long, List<ReservationRoom>> assignmentsByReservationId = assignments.stream()
        .collect(Collectors.groupingBy(assignment -> assignment.getReservation().getId()));

    RoomStatusSummary roomStatus = buildRoomStatusSummary(rooms, assignments, businessDate);
    int occupiedRooms = roomStatus.checkedIn() + roomStatus.checkingOut();
    int guests = reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CHECKED_IN)
        .filter(reservation -> overlapsBusinessDate(reservation, businessDate))
        .mapToInt(reservation -> reservation.getAdults() + reservation.getChildren())
        .sum();

    List<ReservationMovementItem> arrivals = reservations.stream()
        .filter(reservation -> reservation.getCheckInDate().equals(businessDate))
        .filter(this::isOperationalReservation)
        .map(reservation -> toMovementItem(reservation, assignmentsByReservationId, reservation.getCheckInDate()))
        .toList();

    List<ReservationMovementItem> departures = reservations.stream()
        .filter(reservation -> reservation.getCheckOutDate().equals(businessDate))
        .filter(this::isOperationalReservation)
        .map(reservation -> toMovementItem(reservation, assignmentsByReservationId, reservation.getCheckOutDate()))
        .toList();

    List<ReservationMovementItem> upcomingReservations = reservations.stream()
        .filter(reservation -> !reservation.getCheckInDate().isBefore(businessDate))
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
        .limit(6)
        .map(reservation -> toMovementItem(reservation, assignmentsByReservationId, reservation.getCheckInDate()))
        .toList();

    List<ReservationMovementItem> unpaidReservations = reservations.stream()
        .filter(reservation -> reservation.getPaymentStatus() == PaymentStatus.PENDING
            || reservation.getPaymentStatus() == PaymentStatus.PARTIAL)
        .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED
            && reservation.getStatus() != ReservationStatus.NO_SHOW)
        .sorted(Comparator.comparing(Reservation::getCheckInDate))
        .limit(6)
        .map(reservation -> toMovementItem(reservation, assignmentsByReservationId, reservation.getCheckInDate()))
        .toList();

    int dueIn = (int) reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
        .filter(reservation -> reservation.getCheckInDate().equals(businessDate))
        .count();
    int dueOut = (int) reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CHECKED_IN)
        .filter(reservation -> reservation.getCheckOutDate().equals(businessDate))
        .count();
    int inHouse = (int) reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CHECKED_IN)
        .filter(reservation -> overlapsBusinessDate(reservation, businessDate))
        .count();
    int noShowCount = (int) reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.NO_SHOW)
        .filter(reservation -> reservation.getCheckInDate().equals(businessDate))
        .count();
    int cancellationCount = (int) reservations.stream()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CANCELLED)
        .filter(reservation -> !reservation.getCheckInDate().isBefore(businessDate))
        .count();

    double roomRevenueToday = assignments.stream()
        .filter(assignment -> assignment.getRoom() != null)
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .filter(assignment -> !assignment.getAssignedFrom().isAfter(businessDate)
            && assignment.getAssignedTo().isAfter(businessDate))
        .map(ReservationRoom::getNightlyRate)
        .filter(rate -> rate != null)
        .mapToDouble(BigDecimal::doubleValue)
        .sum();

    int soldRoomsToday = roomStatus.assigned() + roomStatus.checkedIn() + roomStatus.checkingOut();
    double occupancyPercent = roomStatus.sellableRooms() > 0
        ? roundToTwoDecimals((occupiedRooms * 100.0) / roomStatus.sellableRooms())
        : 0D;
    double adrToday = soldRoomsToday > 0
        ? roundToTwoDecimals(roomRevenueToday / soldRoomsToday)
        : 0D;
    double revparToday = roomStatus.sellableRooms() > 0
        ? roundToTwoDecimals(roomRevenueToday / roomStatus.sellableRooms())
        : 0D;

    RevenueSnapshot revenue = buildRevenueSnapshot(reservations, payments, businessDate);

    return new DashboardResponse(
        property.getId(),
        property.getName(),
        businessDate,
        guests,
        occupiedRooms,
        roomStatus.available(),
        roomStatus.maintenance(),
        0,
        occupancyPercent,
        adrToday,
        revparToday,
        arrivals.size(),
        departures.size(),
        dueIn,
        dueOut,
        inHouse,
        noShowCount,
        cancellationCount,
        roomStatus,
        revenue,
        buildArrivalTrend(reservations, businessDate),
        buildOccupancyTrend(roomStatus.sellableRooms(), assignments, businessDate),
        arrivals,
        departures,
        upcomingReservations,
        unpaidReservations,
        auditLogs.stream().limit(6).map(this::toActivityItem).toList()
    );
  }

  private RoomStatusSummary buildRoomStatusSummary(
      List<Room> rooms,
      List<ReservationRoom> assignments,
      LocalDate businessDate
  ) {
    Map<Long, ReservationRoom> activeAssignmentsByRoom = assignments.stream()
        .filter(assignment -> assignment.getRoom() != null)
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .filter(assignment -> !assignment.getAssignedFrom().isAfter(businessDate)
            && assignment.getAssignedTo().isAfter(businessDate))
        .collect(Collectors.toMap(
            assignment -> assignment.getRoom().getId(),
            Function.identity(),
            (left, right) -> left
        ));

    int available = 0;
    int assigned = 0;
    int checkedIn = 0;
    int checkingOut = 0;
    int maintenance = 0;
    int outOfOrder = 0;

    for (Room room : rooms) {
      if (room.getStatus() == RoomStatus.MAINTENANCE) {
        maintenance++;
        continue;
      }

      if (room.getStatus() == RoomStatus.OUT_OF_ORDER) {
        outOfOrder++;
        continue;
      }

      ReservationRoom activeAssignment = activeAssignmentsByRoom.get(room.getId());
      if (activeAssignment == null) {
        available++;
        continue;
      }

      Reservation reservation = activeAssignment.getReservation();
      if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
        if (reservation.getCheckOutDate() != null
            && businessDate.equals(reservation.getCheckOutDate().minusDays(1))) {
          checkingOut++;
        } else {
          checkedIn++;
        }
      } else {
        assigned++;
      }
    }

    int totalRooms = rooms.size();
    return new RoomStatusSummary(
        totalRooms,
        Math.max(totalRooms - maintenance - outOfOrder, 0),
        available,
        assigned,
        checkedIn,
        checkingOut,
        maintenance,
        outOfOrder
    );
  }

  private RevenueSnapshot buildRevenueSnapshot(
      List<Reservation> reservations,
      List<Payment> payments,
      LocalDate businessDate
  ) {
    double revenueToday = payments.stream()
        .filter(payment -> payment.getPaymentDate() != null)
        .filter(payment -> payment.getPaymentDate().toLocalDate().equals(businessDate))
        .map(Payment::getAmount)
        .mapToDouble(BigDecimal::doubleValue)
        .sum();

    double monthlyRevenue = payments.stream()
        .filter(payment -> payment.getPaymentDate() != null)
        .filter(payment -> payment.getPaymentDate().getYear() == businessDate.getYear()
            && payment.getPaymentDate().getMonth() == businessDate.getMonth())
        .map(Payment::getAmount)
        .mapToDouble(BigDecimal::doubleValue)
        .sum();

    double outstandingBalance = reservations.stream()
        .filter(reservation -> reservation.getStatus() != ReservationStatus.CANCELLED
            && reservation.getStatus() != ReservationStatus.NO_SHOW)
        .map(Reservation::getBalanceAmount)
        .filter(balance -> balance != null && balance.compareTo(BigDecimal.ZERO) > 0)
        .mapToDouble(BigDecimal::doubleValue)
        .sum();

    int pendingPayments = (int) reservations.stream()
        .filter(reservation -> reservation.getPaymentStatus() == PaymentStatus.PENDING)
        .count();
    int partialPayments = (int) reservations.stream()
        .filter(reservation -> reservation.getPaymentStatus() == PaymentStatus.PARTIAL)
        .count();
    int paidReservations = (int) reservations.stream()
        .filter(reservation -> reservation.getPaymentStatus() == PaymentStatus.PAID)
        .count();

    return new RevenueSnapshot(
        roundToTwoDecimals(revenueToday),
        roundToTwoDecimals(monthlyRevenue),
        roundToTwoDecimals(outstandingBalance),
        pendingPayments,
        partialPayments,
        paidReservations
    );
  }

  private List<TrendPoint> buildArrivalTrend(List<Reservation> reservations, LocalDate businessDate) {
    return businessDate.datesUntil(businessDate.plusDays(7))
        .map(date -> new TrendPoint(
            date,
            date.getDayOfWeek().name().substring(0, 3),
            reservations.stream()
                .filter(this::isOperationalReservation)
                .filter(reservation -> reservation.getCheckInDate().equals(date))
                .count()
        ))
        .toList();
  }

  private List<TrendPoint> buildOccupancyTrend(
      int sellableRooms,
      List<ReservationRoom> assignments,
      LocalDate businessDate
  ) {
    return businessDate.datesUntil(businessDate.plusDays(7))
        .map(date -> {
          Set<Long> soldRooms = assignments.stream()
              .filter(assignment -> assignment.getRoom() != null)
              .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
                  || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
              .filter(assignment -> !assignment.getAssignedFrom().isAfter(date)
                  && assignment.getAssignedTo().isAfter(date))
              .map(assignment -> assignment.getRoom().getId())
              .collect(Collectors.toSet());

          double value = sellableRooms > 0 ? (soldRooms.size() * 100.0) / sellableRooms : 0D;
          return new TrendPoint(date, date.getDayOfWeek().name().substring(0, 3), roundToTwoDecimals(value));
        })
        .toList();
  }

  private ReservationMovementItem toMovementItem(
      Reservation reservation,
      Map<Long, List<ReservationRoom>> assignmentsByReservationId,
      LocalDate date
  ) {
    String roomNumbers = assignmentsByReservationId.getOrDefault(reservation.getId(), List.of()).stream()
        .map(assignment -> assignment.getRoom() != null ? assignment.getRoom().getRoomNumber() : "Unassigned")
        .collect(Collectors.joining(", "));

    return new ReservationMovementItem(
        reservation.getId(),
        reservation.getReservationNumber(),
        reservation.getPrimaryGuest().getFullName(),
        roomNumbers,
        date,
        reservation.getStatus().name(),
        reservation.getPaymentStatus() != null ? reservation.getPaymentStatus().name() : "PENDING",
        reservation.getBalanceAmount() != null ? reservation.getBalanceAmount().doubleValue() : 0D
    );
  }

  private ActivityItem toActivityItem(AuditLog auditLog) {
    return new ActivityItem(
        auditLog.getId(),
        auditLog.getModule().name(),
        auditLog.getAction().name(),
        auditLog.getDescription(),
        auditLog.getActor() != null ? auditLog.getActor().getFullName() : "System",
        auditLog.getCreatedAt()
    );
  }

  private boolean overlapsBusinessDate(Reservation reservation, LocalDate businessDate) {
    return reservation.getCheckInDate() != null
        && reservation.getCheckOutDate() != null
        && !reservation.getCheckInDate().isAfter(businessDate)
        && reservation.getCheckOutDate().isAfter(businessDate);
  }

  private boolean isOperationalReservation(Reservation reservation) {
    return reservation.getStatus() != ReservationStatus.CANCELLED
        && reservation.getStatus() != ReservationStatus.NO_SHOW;
  }

  private double roundToTwoDecimals(double value) {
    return Math.round(value * 100D) / 100D;
  }
}
