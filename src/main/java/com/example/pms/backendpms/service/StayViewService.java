package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.StayViewDtos.BookingBlockResponse;
import com.example.pms.backendpms.dto.StayViewDtos.LegendItemResponse;
import com.example.pms.backendpms.dto.StayViewDtos.RoomGroupResponse;
import com.example.pms.backendpms.dto.StayViewDtos.RoomRowResponse;
import com.example.pms.backendpms.dto.StayViewDtos.StayDateResponse;
import com.example.pms.backendpms.dto.StayViewDtos.StaySummaryResponse;
import com.example.pms.backendpms.dto.StayViewDtos.StayViewResponse;
import com.example.pms.backendpms.model.Reservation;
import com.example.pms.backendpms.model.ReservationRoom;
import com.example.pms.backendpms.model.ReservationStatus;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.ReservationRoomRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StayViewService {

  private final PropertyService propertyService;
  private final ReservationRepository reservationRepository;
  private final ReservationRoomRepository reservationRoomRepository;
  private final RoomRepository roomRepository;
  private final RoomTypeRepository roomTypeRepository;

  public StayViewService(
      PropertyService propertyService,
      ReservationRepository reservationRepository,
      ReservationRoomRepository reservationRoomRepository,
      RoomRepository roomRepository,
      RoomTypeRepository roomTypeRepository
  ) {
    this.propertyService = propertyService;
    this.reservationRepository = reservationRepository;
    this.reservationRoomRepository = reservationRoomRepository;
    this.roomRepository = roomRepository;
    this.roomTypeRepository = roomTypeRepository;
  }

  public StayViewResponse getStayView(Long propertyId, LocalDate focusDate, int days) {
    var property = propertyService.getPropertyEntity(propertyId);
    LocalDate safeFocusDate = focusDate != null ? focusDate : LocalDate.now();
    int safeDays = Math.max(days, 1);
    LocalDate rangeEndInclusive = safeFocusDate.plusDays(safeDays - 1);

    List<Room> rooms = roomRepository.findByPropertyIdOrderByRoomNumberAsc(propertyId);
    List<ReservationRoom> assignments = reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(propertyId).stream()
        .filter(assignment -> !assignment.getAssignedTo().isBefore(safeFocusDate)
            && !assignment.getAssignedFrom().isAfter(rangeEndInclusive))
        .toList();

    List<StayDateResponse> dates = buildDates(safeFocusDate, safeDays);
    List<RoomGroupResponse> roomGroups = roomTypeRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
        .map(roomType -> {
          List<Room> groupedRooms = rooms.stream()
              .filter(room -> room.getRoomType().getId().equals(roomType.getId()))
              .toList();

          List<RoomRowResponse> roomRows = groupedRooms.stream()
              .map(room -> new RoomRowResponse(
                  room.getId(),
                  room.getRoomNumber(),
                  "*",
                  room.getStatus().name(),
                  room.getHousekeepingStatus().name(),
                  assignments.stream()
                      .filter(assignment -> assignment.getRoom() != null && assignment.getRoom().getId().equals(room.getId()))
                      .flatMap(assignment -> toBlocks(assignment, safeFocusDate, rangeEndInclusive).stream())
                      .toList()
              ))
              .toList();

          List<Integer> dailyAvailability = dates.stream()
              .map(date -> (int) groupedRooms.stream()
                  .filter(room -> isAvailable(room, assignments, date.iso()))
                  .count())
              .toList();

          return new RoomGroupResponse(roomType.getName(), dailyAvailability, roomRows);
        })
        .toList();

    List<Integer> footerAvailability = dates.stream()
        .map(date -> (int) rooms.stream().filter(room -> isAvailable(room, assignments, date.iso())).count())
        .toList();

    return new StayViewResponse(
        property.getId(),
        property.getName(),
        safeFocusDate,
        List.of(
            new LegendItemResponse("assigned", "Assigned"),
            new LegendItemResponse("checked_in", "Checked in"),
            new LegendItemResponse("checking_out", "Checking out"),
            new LegendItemResponse("checked_out", "Checked out"),
            new LegendItemResponse("maintenance", "Maintenance")
        ),
        dates,
        summarize(rooms, assignments, safeFocusDate),
        roomGroups,
        footerAvailability
    );
  }

  private List<StayDateResponse> buildDates(LocalDate focusDate, int days) {
    return java.util.stream.IntStream.range(0, days)
        .mapToObj(offset -> {
          LocalDate date = focusDate.plusDays(offset);
          return new StayDateResponse(
              date,
              String.valueOf(date.getDayOfMonth()),
              date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
              date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
              date.equals(focusDate)
          );
        })
        .toList();
  }

  private StaySummaryResponse summarize(List<Room> rooms, List<ReservationRoom> assignments, LocalDate date) {
    Set<Long> occupiedRoomIds = assignments.stream()
        .filter(assignment -> assignment.getRoom() != null)
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .filter(assignment -> !assignment.getAssignedFrom().isAfter(date) && !assignment.getAssignedTo().isBefore(date))
        .map(assignment -> assignment.getRoom().getId())
        .collect(Collectors.toSet());

    int maintenance = (int) rooms.stream().filter(room -> room.getStatus() == RoomStatus.MAINTENANCE).count();
    int occupied = occupiedRoomIds.size();
    int available = Math.max(rooms.size() - occupied - maintenance, 0);
    int guests = assignments.stream()
        .map(ReservationRoom::getReservation)
        .distinct()
        .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED
            || reservation.getStatus() == ReservationStatus.CHECKED_IN)
        .filter(reservation -> !reservation.getCheckInDate().isAfter(date) && !reservation.getCheckOutDate().isBefore(date))
        .mapToInt(reservation -> reservation.getAdults() + reservation.getChildren())
        .sum();

    return new StaySummaryResponse(guests, occupied, available, 0, maintenance);
  }

  private List<BookingBlockResponse> toBlocks(
      ReservationRoom assignment,
      LocalDate rangeStart,
      LocalDate rangeEndInclusive
  ) {
    Reservation reservation = assignment.getReservation();
    LocalDate visibleStart = assignment.getAssignedFrom().isBefore(rangeStart) ? rangeStart : assignment.getAssignedFrom();
    LocalDate visibleEnd = assignment.getAssignedTo().isAfter(rangeEndInclusive) ? rangeEndInclusive : assignment.getAssignedTo();

    if (visibleEnd.isBefore(visibleStart)) {
      return List.of();
    }

    if (reservation.getStatus() == ReservationStatus.CHECKED_IN
        && !reservation.getCheckOutDate().isBefore(visibleStart)
        && !reservation.getCheckOutDate().isAfter(visibleEnd)) {
      List<BookingBlockResponse> blocks = new ArrayList<>();
      LocalDate checkedInEnd = reservation.getCheckOutDate().minusDays(1);

      if (!checkedInEnd.isBefore(visibleStart)) {
        blocks.add(buildBlock(
            assignment,
            rangeStart,
            rangeEndInclusive,
            visibleStart,
            checkedInEnd,
            "checked_in"
        ));
      }

      blocks.add(buildBlock(
          assignment,
          rangeStart,
          rangeEndInclusive,
          reservation.getCheckOutDate(),
          reservation.getCheckOutDate(),
          "checking_out"
      ));

      return blocks;
    }

    return List.of(buildBlock(
        assignment,
        rangeStart,
        rangeEndInclusive,
        visibleStart,
        visibleEnd,
        mapStatus(reservation)
    ));
  }

  private String mapStatus(Reservation reservation) {
    return switch (reservation.getStatus()) {
      case CHECKED_IN -> "checked_in";
      case CHECKED_OUT -> "checked_out";
      case CANCELLED, NO_SHOW -> "cancelled";
      default -> "assigned";
    };
  }

  private BookingBlockResponse buildBlock(
      ReservationRoom assignment,
      LocalDate rangeStart,
      LocalDate rangeEndInclusive,
      LocalDate visibleStart,
      LocalDate visibleEnd,
      String status
  ) {
    int span = (int) (visibleEnd.toEpochDay() - visibleStart.toEpochDay()) + 1;

    return new BookingBlockResponse(
        assignment.getReservation().getId(),
        assignment.getReservation().getReservationNumber(),
        assignment.getReservation().getPrimaryGuest().getFullName(),
        assignment.getRoom() != null ? assignment.getRoom().getRoomNumber() : "Unassigned",
        assignment.getRoomType().getName(),
        status,
        assignment.getReservation().getCheckInDate(),
        assignment.getReservation().getCheckOutDate(),
        assignment.getReservation().getAdults() + assignment.getReservation().getChildren(),
        (int) (visibleStart.toEpochDay() - rangeStart.toEpochDay()) + 1,
        Math.max(span, 1),
        assignment.getAssignedFrom().isBefore(rangeStart),
        assignment.getAssignedTo().isAfter(rangeEndInclusive)
    );
  }

  private boolean isAvailable(Room room, List<ReservationRoom> assignments, LocalDate date) {
    if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.OUT_OF_ORDER) {
      return false;
    }

    return assignments.stream()
        .filter(assignment -> assignment.getRoom() != null && assignment.getRoom().getId().equals(room.getId()))
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .noneMatch(assignment -> !assignment.getAssignedFrom().isAfter(date) && !assignment.getAssignedTo().isBefore(date));
  }
}
