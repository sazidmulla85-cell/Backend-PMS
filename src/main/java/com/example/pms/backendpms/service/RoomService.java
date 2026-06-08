package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.RoomDtos.CreateRoomRequest;
import com.example.pms.backendpms.dto.RoomDtos.CreateRoomTypeRequest;
import com.example.pms.backendpms.dto.RoomDtos.RoomResponse;
import com.example.pms.backendpms.dto.RoomDtos.RoomSummaryResponse;
import com.example.pms.backendpms.dto.RoomDtos.RoomTypeGroupResponse;
import com.example.pms.backendpms.dto.RoomDtos.RoomsViewResponse;
import com.example.pms.backendpms.dto.RoomDtos.UpdateRoomStatusRequest;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.HousekeepingStatus;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.ReservationRoom;
import com.example.pms.backendpms.model.ReservationStatus;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.model.RoomType;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.ReservationRoomRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

  private final PropertyService propertyService;
  private final RoomTypeRepository roomTypeRepository;
  private final RoomRepository roomRepository;
  private final ReservationRoomRepository reservationRoomRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogService auditLogService;

  public RoomService(
      PropertyService propertyService,
      RoomTypeRepository roomTypeRepository,
      RoomRepository roomRepository,
      ReservationRoomRepository reservationRoomRepository,
      UserAccountRepository userAccountRepository,
      AuditLogService auditLogService
  ) {
    this.propertyService = propertyService;
    this.roomTypeRepository = roomTypeRepository;
    this.roomRepository = roomRepository;
    this.reservationRoomRepository = reservationRoomRepository;
    this.userAccountRepository = userAccountRepository;
    this.auditLogService = auditLogService;
  }

  public RoomsViewResponse getRoomsView(Long propertyId, LocalDate requestedBusinessDate) {
    Property property = propertyService.getPropertyEntity(propertyId);
    List<Room> rooms = roomRepository.findByPropertyIdOrderByRoomNumberAsc(propertyId);
    List<ReservationRoom> assignments = reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(propertyId);
    LocalDate businessDate = requestedBusinessDate != null ? requestedBusinessDate : LocalDate.now();

    List<RoomTypeGroupResponse> roomTypes = roomTypeRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
        .map(roomType -> {
          List<Room> groupedRooms = rooms.stream()
              .filter(room -> room.getRoomType().getId().equals(roomType.getId()))
              .sorted(Comparator.comparing(Room::getRoomNumber))
              .toList();

          List<RoomResponse> roomResponses = groupedRooms.stream()
              .map(room -> toRoomResponse(room, roomType, assignments, businessDate))
              .toList();

          return new RoomTypeGroupResponse(
              roomType.getId(),
              roomType.getName(),
              roomType.getBaseRate(),
              summarize(groupedRooms, assignments, businessDate),
              roomResponses
          );
        })
        .toList();

    return new RoomsViewResponse(
        property.getId(),
        property.getName(),
        businessDate,
        summarize(rooms, assignments, businessDate),
        roomTypes
    );
  }

  @Transactional
  public RoomTypeGroupResponse createRoomType(Long propertyId, CreateRoomTypeRequest request) {
    Property property = propertyService.getPropertyEntity(propertyId);

    RoomType roomType = new RoomType();
    roomType.setProperty(property);
    roomType.setName(request.name());
    roomType.setCode(request.code());
    roomType.setBaseOccupancy(request.baseOccupancy());
    roomType.setMaxOccupancy(request.maxOccupancy());
    roomType.setBaseRate(request.baseRate());
    roomType = roomTypeRepository.save(roomType);

    auditLogService.log(
        defaultActor(property),
        property,
        AuditModule.ROOMS,
        AuditAction.CREATE,
        "RoomType",
        String.valueOf(roomType.getId()),
        "Created room type " + roomType.getName()
    );

    return new RoomTypeGroupResponse(
        roomType.getId(),
        roomType.getName(),
        roomType.getBaseRate(),
        new RoomSummaryResponse(0, 0, 0, 0, 0, 0, 0),
        List.of()
    );
  }

  @Transactional
  public RoomResponse createRoom(Long propertyId, CreateRoomRequest request) {
    Property property = propertyService.getPropertyEntity(propertyId);
    RoomType roomType = roomTypeRepository.findByIdAndPropertyId(request.roomTypeId(), propertyId)
        .orElseThrow(() -> new NotFoundException("Room type not found for id " + request.roomTypeId()));

    Room room = new Room();
    room.setProperty(property);
    room.setRoomType(roomType);
    room.setRoomNumber(request.roomNumber());
    room.setFloorName(request.floorName());
    room.setStatus(request.status() != null ? RoomStatus.valueOf(request.status().toUpperCase()) : RoomStatus.AVAILABLE);
    room.setHousekeepingStatus(request.housekeepingStatus() != null
        ? HousekeepingStatus.valueOf(request.housekeepingStatus().toUpperCase())
        : HousekeepingStatus.CLEAN);
    room = roomRepository.save(room);

    auditLogService.log(
        defaultActor(property),
        property,
        AuditModule.ROOMS,
        AuditAction.CREATE,
        "Room",
        String.valueOf(room.getId()),
        "Created room " + room.getRoomNumber() + " under " + roomType.getName()
    );

    return new RoomResponse(
        room.getId(),
        null,
        room.getRoomNumber(),
        roomType.getName(),
        room.getStatus().name(),
        room.getStatus().name(),
        room.getHousekeepingStatus().name(),
        null,
        null,
        null,
        null
    );
  }

  @Transactional
  public RoomResponse updateRoomStatus(Long propertyId, Long roomId, UpdateRoomStatusRequest request) {
    Room room = roomRepository.findByIdAndPropertyId(roomId, propertyId)
        .orElseThrow(() -> new NotFoundException("Room not found for id " + roomId));
    room.setStatus(RoomStatus.valueOf(request.status().trim().toUpperCase()));

    if (request.housekeepingStatus() != null && !request.housekeepingStatus().isBlank()) {
      room.setHousekeepingStatus(HousekeepingStatus.valueOf(request.housekeepingStatus().trim().toUpperCase()));
    }

    room = roomRepository.save(room);

    auditLogService.log(
        defaultActor(room.getProperty()),
        room.getProperty(),
        AuditModule.ROOMS,
        AuditAction.UPDATE,
        "Room",
        String.valueOf(room.getId()),
        "Updated room " + room.getRoomNumber() + " status to " + room.getStatus().name()
    );

    return toRoomResponse(
        room,
        room.getRoomType(),
        reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(propertyId),
        LocalDate.now()
    );
  }

  private RoomSummaryResponse summarize(List<Room> rooms, List<ReservationRoom> assignments, LocalDate businessDate) {
    int assigned = 0;
    int checkedIn = 0;
    int checkingOut = 0;
    int available = 0;
    int maintenance = 0;
    int outOfOrder = 0;
    int guests = 0;

    for (Room room : rooms) {
      String occupancyStatus = resolveOccupancyStatus(room, assignments, businessDate);

      switch (occupancyStatus) {
        case "maintenance" -> maintenance++;
        case "out_of_order" -> outOfOrder++;
        case "assigned" -> assigned++;
        case "checked_in" -> {
          checkedIn++;
          guests += countGuests(findRelevantAssignment(room.getId(), assignments, businessDate));
        }
        case "checking_out" -> {
          checkingOut++;
          guests += countGuests(findRelevantAssignment(room.getId(), assignments, businessDate));
        }
        default -> available++;
      }
    }

    return new RoomSummaryResponse(guests, assigned, checkedIn, checkingOut, available, maintenance, outOfOrder);
  }

  private RoomResponse toRoomResponse(
      Room room,
      RoomType roomType,
      List<ReservationRoom> assignments,
      LocalDate businessDate
  ) {
    ReservationRoom assignment = findRelevantAssignment(room.getId(), assignments, businessDate);

    return new RoomResponse(
        room.getId(),
        assignment != null ? assignment.getReservation().getId() : null,
        room.getRoomNumber(),
        roomType.getName(),
        room.getStatus().name(),
        resolveOccupancyStatus(room, assignments, businessDate),
        room.getHousekeepingStatus().name(),
        assignment != null ? assignment.getReservation().getPrimaryGuest().getFullName() : null,
        assignment != null ? assignment.getReservation().getReservationNumber() : null,
        assignment != null ? assignment.getReservation().getCheckInDate() : null,
        assignment != null ? assignment.getReservation().getCheckOutDate() : null
    );
  }

  private String resolveOccupancyStatus(Room room, List<ReservationRoom> assignments, LocalDate businessDate) {
    if (room.getStatus() == RoomStatus.MAINTENANCE) {
      return "maintenance";
    }

    if (room.getStatus() == RoomStatus.OUT_OF_ORDER) {
      return "out_of_order";
    }

    ReservationRoom assignment = findRelevantAssignment(room.getId(), assignments, businessDate);

    if (assignment == null) {
      return "available";
    }

    if (assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN) {
      LocalDate checkOutDate = assignment.getReservation().getCheckOutDate();
      if (checkOutDate != null && businessDate.equals(checkOutDate)) {
        return "checking_out";
      }

      return "checked_in";
    }

    return "assigned";
  }

  private ReservationRoom findRelevantAssignment(
      Long roomId,
      List<ReservationRoom> assignments,
      LocalDate businessDate
  ) {
    return assignments.stream()
        .filter(assignment -> assignment.getRoom() != null && assignment.getRoom().getId().equals(roomId))
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .filter(assignment -> !assignment.getAssignedFrom().isAfter(businessDate)
            && !assignment.getAssignedTo().isBefore(businessDate))
        .findFirst()
        .orElse(null);
  }

  private int countGuests(ReservationRoom assignment) {
    if (assignment == null) {
      return 0;
    }

    return assignment.getReservation().getAdults() + assignment.getReservation().getChildren();
  }

  private UserAccount defaultActor(Property property) {
    return userAccountRepository.findByRole(UserRole.HOTEL_OWNER).stream()
        .filter(user -> user.getProperty() != null && user.getProperty().getId().equals(property.getId()))
        .findFirst()
        .orElse(null);
  }
}
