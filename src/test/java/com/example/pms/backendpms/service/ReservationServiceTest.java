package com.example.pms.backendpms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pms.backendpms.dto.ReservationDtos.AssignedRoomRequest;
import com.example.pms.backendpms.dto.ReservationDtos.CreateReservationRequest;
import com.example.pms.backendpms.dto.ReservationDtos.GuestRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationActionRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationDetailResponse;
import com.example.pms.backendpms.model.AuditLog;
import com.example.pms.backendpms.model.BillToType;
import com.example.pms.backendpms.model.BookingSource;
import com.example.pms.backendpms.model.Guest;
import com.example.pms.backendpms.model.GuestDocument;
import com.example.pms.backendpms.model.HousekeepingStatus;
import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.Payment;
import com.example.pms.backendpms.model.PaymentMethod;
import com.example.pms.backendpms.model.PaymentStatus;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.Reservation;
import com.example.pms.backendpms.model.ReservationCharge;
import com.example.pms.backendpms.model.ReservationRoom;
import com.example.pms.backendpms.model.ReservationRoomStatus;
import com.example.pms.backendpms.model.ReservationStatus;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.model.RoomType;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.GuestDocumentRepository;
import com.example.pms.backendpms.repository.GuestRepository;
import com.example.pms.backendpms.repository.PaymentRepository;
import com.example.pms.backendpms.repository.ReservationChargeRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.ReservationRoomRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  @Mock private PropertyService propertyService;
  @Mock private CompanyService companyService;
  @Mock private GuestRepository guestRepository;
  @Mock private GuestDocumentRepository guestDocumentRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private ReservationRoomRepository reservationRoomRepository;
  @Mock private ReservationChargeRepository reservationChargeRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private RoomTypeRepository roomTypeRepository;
  @Mock private RoomRepository roomRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private AuditLogService auditLogService;
  @Mock private GuestEmailNotificationService guestEmailNotificationService;

  @InjectMocks
  private ReservationService reservationService;

  private Property property;
  private UserAccount owner;
  private RoomType roomType;
  private Room room;

  @BeforeEach
  void setUp() {
    owner = new UserAccount();
    owner.setId(11L);
    owner.setFullName("Owner User");
    owner.setPhone("9999999999");
    owner.setEmail("owner@example.com");
    owner.setRole(UserRole.HOTEL_OWNER);
    owner.setActive(true);

    Organization organization = new Organization();
    organization.setId(21L);
    organization.setName("Test Org");

    property = new Property();
    property.setId(1L);
    property.setName("Test Hotel");
    property.setCode("THT");
    property.setOrganization(organization);
    property.setOwner(owner);
    property.setActive(true);

    roomType = new RoomType();
    roomType.setId(31L);
    roomType.setProperty(property);
    roomType.setName("Deluxe");
    roomType.setCode("DLX");
    roomType.setBaseRate(2500D);

    room = new Room();
    room.setId(41L);
    room.setProperty(property);
    room.setRoomType(roomType);
    room.setRoomNumber("101");
    room.setStatus(RoomStatus.AVAILABLE);
    room.setHousekeepingStatus(HousekeepingStatus.CLEAN);

    lenient().when(propertyService.getPropertyEntity(property.getId())).thenReturn(property);
    lenient().when(userAccountRepository.findByRole(UserRole.HOTEL_OWNER)).thenReturn(List.of(owner));
    lenient().when(userAccountRepository.findByPhone(owner.getPhone())).thenReturn(Optional.of(owner));
    lenient().when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> {
      Guest guest = invocation.getArgument(0);
      guest.setId(51L);
      return guest;
    });
    lenient().when(reservationRepository.count()).thenReturn(0L);
    lenient().when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
      Reservation reservation = invocation.getArgument(0);
      if (reservation.getId() == null) {
        reservation.setId(61L);
      }
      return reservation;
    });
    lenient().when(reservationChargeRepository.findByReservationIdOrderByChargeDateDesc(anyLong()))
        .thenReturn(List.<ReservationCharge>of());
    lenient().when(paymentRepository.findByReservationIdOrderByPaymentDateDesc(anyLong()))
        .thenReturn(List.<Payment>of());
    lenient().when(guestDocumentRepository.findByReservation_IdOrderByUploadedAtDesc(anyLong()))
        .thenReturn(List.<GuestDocument>of());
    lenient().doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void createReservationRejectsDuplicateRoomInSameBooking() {
    when(roomTypeRepository.findByIdAndPropertyId(roomType.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(roomType));
    when(roomRepository.findByIdAndPropertyId(room.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(room));
    when(reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(property.getId()))
        .thenReturn(List.of());

    CreateReservationRequest request = buildReservationRequest(
        List.of(
            new AssignedRoomRequest(roomType.getId(), room.getId(), 2500D),
            new AssignedRoomRequest(roomType.getId(), room.getId(), 2500D)
        )
    );

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> reservationService.createReservation(property.getId(), request)
    );

    assertEquals("The same room cannot be assigned multiple times in one reservation.", exception.getMessage());
  }

  @Test
  void createReservationRejectsOccupiedRoomForOverlappingDates() {
    when(roomTypeRepository.findByIdAndPropertyId(roomType.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(roomType));
    when(roomRepository.findByIdAndPropertyId(room.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(room));

    Reservation existingReservation = new Reservation();
    existingReservation.setId(88L);
    existingReservation.setStatus(ReservationStatus.CONFIRMED);
    existingReservation.setCheckInDate(LocalDate.of(2026, 5, 15));
    existingReservation.setCheckOutDate(LocalDate.of(2026, 5, 17));

    ReservationRoom existingAssignment = new ReservationRoom();
    existingAssignment.setReservation(existingReservation);
    existingAssignment.setRoom(room);
    existingAssignment.setAssignedFrom(LocalDate.of(2026, 5, 15));
    existingAssignment.setAssignedTo(LocalDate.of(2026, 5, 17));

    when(reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(property.getId()))
        .thenReturn(List.of(existingAssignment));

    CreateReservationRequest request = buildReservationRequest(
        List.of(new AssignedRoomRequest(roomType.getId(), room.getId(), 2500D))
    );

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> reservationService.createReservation(property.getId(), request)
    );

    assertEquals("Selected room is already occupied for the stay range.", exception.getMessage());
    verify(reservationRoomRepository, never()).save(any(ReservationRoom.class));
  }

  @Test
  void checkOutBlocksGuestBookingWithOutstandingBalance() {
    Reservation reservation = buildCheckedInReservation(BillToType.GUEST, PaymentStatus.PARTIAL);
    reservation.setBalanceAmount(BigDecimal.valueOf(1500));
    when(reservationRepository.findByIdAndPropertyId(reservation.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(reservation));

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> reservationService.checkOut(property.getId(), reservation.getId(), new ReservationActionRequest(null, null))
    );

    assertEquals(
        "Outstanding balance must be cleared before checkout unless the booking is billed to a company.",
        exception.getMessage()
    );
    verify(reservationRepository, never()).save(any(Reservation.class));
  }

  @Test
  void checkOutAllowsCompanyBilledReservationAndMarksRoomDirty() {
    Reservation reservation = buildCheckedInReservation(BillToType.COMPANY, PaymentStatus.PENDING);
    reservation.setBalanceAmount(BigDecimal.valueOf(2000));
    reservation.setTotalAmount(BigDecimal.valueOf(2000));

    ReservationRoom assignment = new ReservationRoom();
    assignment.setId(71L);
    assignment.setReservation(reservation);
    assignment.setRoom(room);
    assignment.setRoomType(roomType);
    assignment.setStatus(ReservationRoomStatus.CHECKED_IN);
    assignment.setNightlyRate(BigDecimal.valueOf(2500));

    when(reservationRepository.findByIdAndPropertyId(reservation.getId(), property.getId()))
        .thenReturn(java.util.Optional.of(reservation));
    when(reservationRoomRepository.findByReservationId(reservation.getId()))
        .thenReturn(List.of(assignment));

    ReservationDetailResponse response = reservationService.checkOut(
        property.getId(),
        reservation.getId(),
        new ReservationActionRequest(null, "Company settlement pending")
    );

    assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    assertEquals(ReservationRoomStatus.CHECKED_OUT, assignment.getStatus());
    assertEquals(HousekeepingStatus.DIRTY, room.getHousekeepingStatus());
    assertEquals("CHECKED_OUT", response.status());
    verify(guestEmailNotificationService).sendCheckOutNotification(reservation);
    verify(roomRepository).save(room);
  }

  private CreateReservationRequest buildReservationRequest(List<AssignedRoomRequest> rooms) {
    return new CreateReservationRequest(
        BookingSource.WALK_IN.name(),
        ReservationStatus.CONFIRMED.name(),
        LocalDate.of(2026, 5, 16),
        LocalDate.of(2026, 5, 18),
        2,
        0,
        "Breakfast",
        "BAR",
        "Late arrival",
        5000D,
        250D,
        0D,
        owner.getPhone(),
        BillToType.GUEST.name(),
        PaymentMethod.CASH.name(),
        null,
        false,
        false,
        null,
        new GuestRequest(
            "John Guest",
            "john@example.com",
            "8888888888",
            "India",
            "MP",
            "Bhopal",
            "462001",
            "Street 1",
            "AADHAAR",
            "1234",
            "VIP"
        ),
        rooms
    );
  }

  private Reservation buildCheckedInReservation(BillToType billToType, PaymentStatus paymentStatus) {
    Guest guest = new Guest();
    guest.setId(81L);
    guest.setProperty(property);
    guest.setFullName("Checked In Guest");
    guest.setPhone("7777777777");
    guest.setEmail("checkedin@example.com");

    Reservation reservation = new Reservation();
    reservation.setId(91L);
    reservation.setProperty(property);
    reservation.setPrimaryGuest(guest);
    reservation.setBookedBy(owner);
    reservation.setReservationNumber("THT-1091");
    reservation.setStatus(ReservationStatus.CHECKED_IN);
    reservation.setSource(BookingSource.WALK_IN);
    reservation.setBillToType(billToType);
    reservation.setPaymentMethod(
        billToType == BillToType.COMPANY ? PaymentMethod.BILL_TO_COMPANY : PaymentMethod.CASH
    );
    reservation.setBookingDate(LocalDate.of(2026, 5, 16));
    reservation.setCheckInDate(LocalDate.of(2026, 5, 16));
    reservation.setCheckOutDate(LocalDate.of(2026, 5, 18));
    reservation.setNights(2);
    reservation.setAdults(2);
    reservation.setChildren(0);
    reservation.setRoomsCount(1);
    reservation.setComplimentary(false);
    reservation.setGroupBooking(false);
    reservation.setRoomAmount(BigDecimal.valueOf(5000));
    reservation.setTaxAmount(BigDecimal.ZERO);
    reservation.setDiscountAmount(BigDecimal.ZERO);
    reservation.setTotalAmount(BigDecimal.valueOf(5000));
    reservation.setBalanceAmount(BigDecimal.ZERO);
    reservation.setPaymentStatus(paymentStatus);
    return reservation;
  }
}
