package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.ReservationDtos.AssignedRoomRequest;
import com.example.pms.backendpms.dto.ReservationDtos.AddChargeRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ChargeResponse;
import com.example.pms.backendpms.dto.ReservationDtos.CreateReservationRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ExchangeRoomRequest;
import com.example.pms.backendpms.dto.ReservationDtos.GuestDocumentResponse;
import com.example.pms.backendpms.dto.ReservationDtos.PaymentResponse;
import com.example.pms.backendpms.dto.ReservationDtos.RecordPaymentRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationActionRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationDetailResponse;
import com.example.pms.backendpms.dto.ReservationDtos.RoomAssignmentResponse;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationSummaryResponse;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.BillToType;
import com.example.pms.backendpms.model.BookingSource;
import com.example.pms.backendpms.model.Company;
import com.example.pms.backendpms.model.Guest;
import com.example.pms.backendpms.model.HousekeepingStatus;
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
import com.example.pms.backendpms.repository.GuestRepository;
import com.example.pms.backendpms.repository.GuestDocumentRepository;
import com.example.pms.backendpms.repository.PaymentRepository;
import com.example.pms.backendpms.repository.ReservationChargeRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.ReservationRoomRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ReservationService {

  private final PropertyService propertyService;
  private final CompanyService companyService;
  private final GuestRepository guestRepository;
  private final GuestDocumentRepository guestDocumentRepository;
  private final ReservationRepository reservationRepository;
  private final ReservationRoomRepository reservationRoomRepository;
  private final ReservationChargeRepository reservationChargeRepository;
  private final PaymentRepository paymentRepository;
  private final RoomTypeRepository roomTypeRepository;
  private final RoomRepository roomRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogService auditLogService;
  private final GuestEmailNotificationService guestEmailNotificationService;

  public ReservationService(
      PropertyService propertyService,
      CompanyService companyService,
      GuestRepository guestRepository,
      GuestDocumentRepository guestDocumentRepository,
      ReservationRepository reservationRepository,
      ReservationRoomRepository reservationRoomRepository,
      ReservationChargeRepository reservationChargeRepository,
      PaymentRepository paymentRepository,
      RoomTypeRepository roomTypeRepository,
      RoomRepository roomRepository,
      UserAccountRepository userAccountRepository,
      AuditLogService auditLogService,
      GuestEmailNotificationService guestEmailNotificationService
  ) {
    this.propertyService = propertyService;
    this.companyService = companyService;
    this.guestRepository = guestRepository;
    this.guestDocumentRepository = guestDocumentRepository;
    this.reservationRepository = reservationRepository;
    this.reservationRoomRepository = reservationRoomRepository;
    this.reservationChargeRepository = reservationChargeRepository;
    this.paymentRepository = paymentRepository;
    this.roomTypeRepository = roomTypeRepository;
    this.roomRepository = roomRepository;
    this.userAccountRepository = userAccountRepository;
    this.auditLogService = auditLogService;
    this.guestEmailNotificationService = guestEmailNotificationService;
  }

  public List<ReservationSummaryResponse> getReservations(Long propertyId) {
    return reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).stream()
        .map(this::toSummary)
        .toList();
  }

  public ReservationDetailResponse getReservation(Long propertyId, Long reservationId) {
    Reservation reservation = reservationRepository.findByIdAndPropertyId(reservationId, propertyId)
        .orElseThrow(() -> new NotFoundException("Reservation not found for id " + reservationId));

    return toDetail(reservation);
  }

  @Transactional
  public ReservationSummaryResponse createReservation(Long propertyId, CreateReservationRequest request) {
    Property property = propertyService.getPropertyEntity(propertyId);
    Company company = resolveCompany(propertyId, request.companyId());
    int nights = calculateNights(request.checkInDate(), request.checkOutDate());

    Guest guest = new Guest();
    guest.setProperty(property);
    guest.setFullName(request.guest().fullName());
    guest.setEmail(blankToNull(request.guest().email()));
    guest.setPhone(request.guest().phone());
    guest.setCountry(blankToNull(request.guest().country()));
    guest.setState(blankToNull(request.guest().state()));
    guest.setCity(blankToNull(request.guest().city()));
    guest.setPostalCode(blankToNull(request.guest().postalCode()));
    guest.setAddress(blankToNull(request.guest().address()));
    guest.setIdType(blankToNull(request.guest().idType()));
    guest.setIdNumber(blankToNull(request.guest().idNumber()));
    guest.setNotes(blankToNull(request.guest().notes()));
    guest = guestRepository.save(guest);

    Reservation reservation = new Reservation();
    reservation.setProperty(property);
    reservation.setPrimaryGuest(guest);
    reservation.setBookedBy(resolveBookedBy(property, request.bookedByPhone()));
    reservation.setReservationNumber(generateReservationNumber(property));
    reservation.setStatus(ReservationStatus.valueOf(request.status().toUpperCase(Locale.ROOT)));
    reservation.setSource(BookingSource.valueOf(request.source().toUpperCase(Locale.ROOT)));
    reservation.setBillToType(resolveBillToType(request.billToType(), company));
    reservation.setPaymentMethod(resolvePaymentMethod(request.paymentMethod(), reservation.getBillToType()));
    reservation.setCompany(company);
    reservation.setBookingDate(LocalDate.now());
    reservation.setCheckInDate(request.checkInDate());
    reservation.setCheckOutDate(request.checkOutDate());
    reservation.setNights(nights);
    reservation.setAdults(request.adults());
    reservation.setChildren(request.children() != null ? request.children() : 0);
    reservation.setRoomsCount(request.rooms().size());
    reservation.setComplimentary(Boolean.TRUE.equals(request.complimentary()));
    reservation.setGroupBooking(Boolean.TRUE.equals(request.groupBooking()));
    reservation.setGroupCode(blankToNull(request.groupCode()));
    reservation.setMealPlan(blankToNull(request.mealPlan()));
    reservation.setRatePlan(blankToNull(request.ratePlan()));
    reservation.setSpecialRequests(blankToNull(request.specialRequests()));
    reservation.setRoomAmount(BigDecimal.ZERO);
    reservation.setTaxAmount(calculateTotalTax(
        request.taxAmount(),
        nights,
        request.rooms().size(),
        reservation.getComplimentary()
    ));
    reservation.setDiscountAmount(toBigDecimal(request.discountAmount()));
    recalculateChargesAndPayments(reservation);
    reservation = reservationRepository.save(reservation);

    Set<Long> assignedRoomIds = new HashSet<>();
    for (AssignedRoomRequest assignedRoom : request.rooms()) {
      RoomType roomType = roomTypeRepository.findByIdAndPropertyId(assignedRoom.roomTypeId(), propertyId)
          .orElseThrow(() -> new NotFoundException("Room type not found for id " + assignedRoom.roomTypeId()));

      Room room = null;
      if (assignedRoom.roomId() != null) {
        room = roomRepository.findByIdAndPropertyId(assignedRoom.roomId(), propertyId)
            .orElseThrow(() -> new NotFoundException("Room not found for id " + assignedRoom.roomId()));
        if (!assignedRoomIds.add(room.getId())) {
          throw new IllegalStateException("The same room cannot be assigned multiple times in one reservation.");
        }
        ensureRoomIsAvailableForReservation(propertyId, reservation, room.getId());
      }

      ReservationRoom reservationRoom = new ReservationRoom();
      reservationRoom.setReservation(reservation);
      reservationRoom.setRoomType(roomType);
      reservationRoom.setRoom(room);
      reservationRoom.setStatus(mapReservationRoomStatus(reservation.getStatus()));
      reservationRoom.setAssignedFrom(request.checkInDate());
      reservationRoom.setAssignedTo(request.checkOutDate());
      reservationRoom.setNightlyRate(resolveNightlyRate(
          assignedRoom,
          roomType,
          reservation.getComplimentary()
      ));
      reservationRoomRepository.save(reservationRoom);
    }

    recalculateChargesAndPayments(reservation);
    reservationRepository.save(reservation);

    auditLogService.log(
        reservation.getBookedBy(),
        property,
        AuditModule.RESERVATIONS,
        AuditAction.CREATE,
        "Reservation",
        String.valueOf(reservation.getId()),
        "Created reservation " + reservation.getReservationNumber() + " for guest " + guest.getFullName()
    );

    return toSummary(reservation);
  }

  @Transactional
  public ReservationDetailResponse checkIn(
      Long propertyId,
      Long reservationId,
      ReservationActionRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);
    validateTransition(reservation, ReservationStatus.CHECKED_IN);

    reservation.setStatus(ReservationStatus.CHECKED_IN);
    reservationRepository.save(reservation);
    updateReservationRoomStatuses(reservation, ReservationRoomStatus.CHECKED_IN);

    auditReservationTransition(
        reservation,
        request,
        AuditAction.UPDATE,
        "Checked in guest " + reservation.getPrimaryGuest().getFullName()
    );

    guestEmailNotificationService.sendCheckInNotification(reservation);

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse checkOut(
      Long propertyId,
      Long reservationId,
      ReservationActionRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);
    validateTransition(reservation, ReservationStatus.CHECKED_OUT);

    reservation.setStatus(ReservationStatus.CHECKED_OUT);
    reservationRepository.save(reservation);
    updateReservationRoomStatuses(reservation, ReservationRoomStatus.CHECKED_OUT);

    auditReservationTransition(
        reservation,
        request,
        AuditAction.UPDATE,
        "Checked out guest " + reservation.getPrimaryGuest().getFullName()
    );

    guestEmailNotificationService.sendCheckOutNotification(reservation);

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse undoCheckIn(
      Long propertyId,
      Long reservationId,
      ReservationActionRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);

    if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
      throw new IllegalStateException("Only checked-in reservations can be moved back to confirmed.");
    }

    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservationRepository.save(reservation);
    updateReservationRoomStatuses(reservation, ReservationRoomStatus.ASSIGNED);

    auditReservationTransition(
        reservation,
        request,
        AuditAction.UPDATE,
        "Reverted check-in for reservation " + reservation.getReservationNumber()
    );

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse cancel(
      Long propertyId,
      Long reservationId,
      ReservationActionRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);
    validateTransition(reservation, ReservationStatus.CANCELLED);

    reservation.setStatus(ReservationStatus.CANCELLED);
    reservationRepository.save(reservation);
    updateReservationRoomStatuses(reservation, ReservationRoomStatus.CANCELLED);

    auditReservationTransition(
        reservation,
        request,
        AuditAction.UPDATE,
        "Cancelled reservation " + reservation.getReservationNumber()
    );

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse markNoShow(
      Long propertyId,
      Long reservationId,
      ReservationActionRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);
    validateTransition(reservation, ReservationStatus.NO_SHOW);

    reservation.setStatus(ReservationStatus.NO_SHOW);
    reservationRepository.save(reservation);
    updateReservationRoomStatuses(reservation, ReservationRoomStatus.CANCELLED);

    auditReservationTransition(
        reservation,
        request,
        AuditAction.UPDATE,
        "Marked reservation " + reservation.getReservationNumber() + " as no-show"
    );

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse recordPayment(
      Long propertyId,
      Long reservationId,
      RecordPaymentRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);

    BigDecimal paymentAmount = toBigDecimal(request.amount());
    if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payment amount must be greater than zero.");
    }

    if (reservation.getBalanceAmount() != null
        && paymentAmount.compareTo(reservation.getBalanceAmount()) > 0) {
      throw new IllegalArgumentException("Payment amount cannot exceed the outstanding balance.");
    }

    Payment payment = new Payment();
    payment.setReservation(reservation);
    payment.setReceivedBy(resolveActor(reservation.getProperty(), request.receivedByPhone()));
    payment.setAmount(paymentAmount);
    payment.setPaymentMethod(PaymentMethod.valueOf(request.paymentMethod().toUpperCase(Locale.ROOT)));
    payment.setStatus(
        reservation.getBalanceAmount() != null
            && paymentAmount.compareTo(reservation.getBalanceAmount()) < 0
            ? PaymentStatus.PARTIAL
            : PaymentStatus.PAID
    );
    payment.setReferenceNumber(blankToNull(request.referenceNumber()));
    payment.setNotes(blankToNull(request.notes()));
    payment.setPaymentDate(LocalDateTime.now());
    paymentRepository.save(payment);

    recalculateChargesAndPayments(reservation);
    reservationRepository.save(reservation);

    auditLogService.log(
        payment.getReceivedBy(),
        reservation.getProperty(),
        AuditModule.RESERVATIONS,
        AuditAction.UPDATE,
        "Payment",
        String.valueOf(payment.getId()),
        "Recorded "
            + payment.getAmount()
            + " via "
            + payment.getPaymentMethod().name()
            + " for reservation "
            + reservation.getReservationNumber()
    );

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse addCharge(
      Long propertyId,
      Long reservationId,
      AddChargeRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);

    ReservationCharge charge = new ReservationCharge();
    charge.setReservation(reservation);
    charge.setCreatedBy(resolveActor(reservation.getProperty(), request.createdByPhone()));
    charge.setDescription(request.description().trim());
    charge.setAmount(toBigDecimal(request.amount()));
    charge.setChargeDate(LocalDateTime.now());
    reservationChargeRepository.save(charge);

    recalculateChargesAndPayments(reservation);
    reservationRepository.save(reservation);

    auditLogService.log(
        charge.getCreatedBy(),
        reservation.getProperty(),
        AuditModule.RESERVATIONS,
        AuditAction.UPDATE,
        "ReservationCharge",
        String.valueOf(charge.getId()),
        "Added service charge " + charge.getDescription() + " to " + reservation.getReservationNumber()
    );

    return toDetail(reservation);
  }

  @Transactional
  public ReservationDetailResponse exchangeRoom(
      Long propertyId,
      Long reservationId,
      ExchangeRoomRequest request
  ) {
    Reservation reservation = getReservationEntity(propertyId, reservationId);
    Room newRoom = roomRepository.findByIdAndPropertyId(request.roomId(), propertyId)
        .orElseThrow(() -> new NotFoundException("Room not found for id " + request.roomId()));

    if (newRoom.getStatus() == RoomStatus.MAINTENANCE || newRoom.getStatus() == RoomStatus.OUT_OF_ORDER) {
      throw new IllegalStateException("Selected room is not assignable.");
    }

    ensureRoomIsAvailableForReservation(propertyId, reservation, newRoom.getId());

    ReservationRoom reservationRoom = reservationRoomRepository.findByReservationId(reservation.getId()).stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Assigned room not found for reservation " + reservationId));

    reservationRoom.setRoom(newRoom);
    reservationRoomRepository.save(reservationRoom);

    auditLogService.log(
        resolveActor(reservation.getProperty(), request.actedByPhone()),
        reservation.getProperty(),
        AuditModule.RESERVATIONS,
        AuditAction.UPDATE,
        "Reservation",
        String.valueOf(reservation.getId()),
        "Exchanged room to " + newRoom.getRoomNumber() + appendNotes(request.notes())
    );

    return toDetail(reservation);
  }

  private ReservationSummaryResponse toSummary(Reservation reservation) {
    List<String> roomNumbers = reservationRoomRepository.findByReservationId(reservation.getId()).stream()
        .map(room -> room.getRoom() != null ? room.getRoom().getRoomNumber() : "Unassigned")
        .toList();

    return new ReservationSummaryResponse(
        reservation.getId(),
        reservation.getReservationNumber(),
        reservation.getPrimaryGuest().getFullName(),
        reservation.getCheckInDate(),
        reservation.getCheckOutDate(),
        reservation.getStatus().name(),
        reservation.getSource().name(),
        reservation.getTotalAmount().doubleValue(),
        reservation.getPaymentStatus().name(),
        Boolean.TRUE.equals(reservation.getComplimentary()),
        Boolean.TRUE.equals(reservation.getGroupBooking()),
        reservation.getCompany() != null ? reservation.getCompany().getName() : null,
        roomNumbers
    );
  }

  private ReservationDetailResponse toDetail(Reservation reservation) {
    List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservationId(reservation.getId());
    List<String> roomNumbers = reservationRooms.stream()
        .map(room -> room.getRoom() != null ? room.getRoom().getRoomNumber() : "Unassigned")
        .toList();
    List<RoomAssignmentResponse> roomAssignments = reservationRooms.stream()
        .map(room -> new RoomAssignmentResponse(
            room.getRoom() != null ? room.getRoom().getId() : null,
            room.getRoom() != null ? room.getRoom().getRoomNumber() : "Unassigned",
            room.getRoomType().getName(),
            room.getNightlyRate() != null ? room.getNightlyRate().doubleValue() : 0D
        ))
        .toList();

    List<PaymentResponse> payments = paymentRepository.findByReservationIdOrderByPaymentDateDesc(reservation.getId()).stream()
        .map(this::toPayment)
        .toList();
    List<ChargeResponse> charges = reservationChargeRepository.findByReservationIdOrderByChargeDateDesc(reservation.getId()).stream()
        .map(this::toCharge)
        .toList();
    List<GuestDocumentResponse> documents = guestDocumentRepository.findByReservation_IdOrderByUploadedAtDesc(reservation.getId()).stream()
        .map(document -> new GuestDocumentResponse(
            document.getId(),
            document.getGuest().getId(),
            document.getGuest().getFullName(),
            document.getDocumentType(),
            document.getFileName(),
            document.getContentType(),
            document.getFileSize(),
            document.getUploadedAt(),
            "/api/properties/" + reservation.getProperty().getId()
                + "/reservations/" + reservation.getId()
                + "/documents/" + document.getId()
                + "/content"
        ))
        .toList();

    return new ReservationDetailResponse(
        reservation.getId(),
        reservation.getReservationNumber(),
        reservation.getPrimaryGuest().getFullName(),
        reservation.getPrimaryGuest().getPhone(),
        reservation.getPrimaryGuest().getEmail(),
        reservation.getCheckInDate(),
        reservation.getCheckOutDate(),
        reservation.getNights(),
        reservation.getAdults(),
        reservation.getChildren(),
        reservation.getStatus().name(),
        reservation.getSource().name(),
        reservation.getBookingDate(),
        reservation.getBillToType() != null ? reservation.getBillToType().name() : BillToType.GUEST.name(),
        reservation.getPaymentMethod() != null ? reservation.getPaymentMethod().name() : PaymentMethod.CASH.name(),
        reservation.getCompany() != null ? reservation.getCompany().getId() : null,
        reservation.getCompany() != null ? reservation.getCompany().getName() : null,
        Boolean.TRUE.equals(reservation.getComplimentary()),
        Boolean.TRUE.equals(reservation.getGroupBooking()),
        reservation.getGroupCode(),
        reservation.getMealPlan(),
        reservation.getRatePlan(),
        reservation.getSpecialRequests(),
        reservation.getRoomAmount().doubleValue(),
        reservation.getTaxAmount().doubleValue(),
        reservation.getDiscountAmount().doubleValue(),
        reservation.getTotalAmount().doubleValue(),
        reservation.getTotalAmount().subtract(reservation.getBalanceAmount()).doubleValue(),
        reservation.getBalanceAmount().doubleValue(),
        reservation.getPaymentStatus().name(),
        roomNumbers,
        roomAssignments,
        payments,
        charges,
        documents
    );
  }

  private PaymentResponse toPayment(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getAmount().doubleValue(),
        payment.getPaymentMethod().name(),
        payment.getStatus().name(),
        payment.getReferenceNumber(),
        payment.getPaymentDate()
    );
  }

  private ChargeResponse toCharge(ReservationCharge charge) {
    return new ChargeResponse(
        charge.getId(),
        charge.getDescription(),
        charge.getAmount().doubleValue(),
        charge.getChargeDate()
    );
  }

  Reservation getReservationEntity(Long propertyId, Long reservationId) {
    return reservationRepository.findByIdAndPropertyId(reservationId, propertyId)
        .orElseThrow(() -> new NotFoundException("Reservation not found for id " + reservationId));
  }

  private void validateTransition(Reservation reservation, ReservationStatus targetStatus) {
    ReservationStatus currentStatus = reservation.getStatus();

    if (currentStatus == targetStatus) {
      return;
    }

    switch (targetStatus) {
      case CHECKED_IN -> {
        if (currentStatus != ReservationStatus.CONFIRMED) {
          throw new IllegalStateException("Only confirmed reservations can be checked in.");
        }
      }
      case CHECKED_OUT -> {
        if (currentStatus != ReservationStatus.CHECKED_IN) {
          throw new IllegalStateException("Only checked-in reservations can be checked out.");
        }

        if (reservation.getBillToType() != BillToType.COMPANY
            && reservation.getPaymentStatus() != PaymentStatus.PAID) {
          throw new IllegalStateException(
              "Outstanding balance must be cleared before checkout unless the booking is billed to a company."
          );
        }
      }
      case CANCELLED -> {
        if (currentStatus != ReservationStatus.CONFIRMED) {
          throw new IllegalStateException("Only confirmed reservations can be cancelled.");
        }
      }
      case NO_SHOW -> {
        if (currentStatus != ReservationStatus.CONFIRMED) {
          throw new IllegalStateException("Only confirmed reservations can be marked as no-show.");
        }

        if (reservation.getCheckInDate() != null && LocalDate.now().isBefore(reservation.getCheckInDate())) {
          throw new IllegalStateException("No-show can only be marked on or after check-in date.");
        }
      }
      default -> {
      }
    }
  }

  private void updateReservationRoomStatuses(
      Reservation reservation,
      ReservationRoomStatus nextStatus
  ) {
    reservationRoomRepository.findByReservationId(reservation.getId()).forEach(reservationRoom -> {
      reservationRoom.setStatus(nextStatus);
      reservationRoomRepository.save(reservationRoom);

      if (nextStatus == ReservationRoomStatus.CHECKED_OUT && reservationRoom.getRoom() != null) {
        Room room = reservationRoom.getRoom();
        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        roomRepository.save(room);
      }
    });
  }

  private void recalculateChargesAndPayments(Reservation reservation) {
    reservation.setNights(calculateNights(reservation.getCheckInDate(), reservation.getCheckOutDate()));

    BigDecimal baseRoomAmount = Boolean.TRUE.equals(reservation.getComplimentary())
        ? BigDecimal.ZERO
        : calculateRoomRevenue(reservation);
    BigDecimal taxAmount = Boolean.TRUE.equals(reservation.getComplimentary())
        ? BigDecimal.ZERO
        : safeAmount(reservation.getTaxAmount());
    BigDecimal discountAmount = safeAmount(reservation.getDiscountAmount());
    BigDecimal serviceCharges = reservationChargeRepository.findByReservationIdOrderByChargeDateDesc(reservation.getId())
        .stream()
        .map(ReservationCharge::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalAmount = baseRoomAmount.add(taxAmount).add(serviceCharges).subtract(discountAmount);
    if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
      totalAmount = BigDecimal.ZERO;
    }
    reservation.setRoomAmount(baseRoomAmount);
    reservation.setTaxAmount(taxAmount);
    reservation.setTotalAmount(totalAmount);

    BigDecimal totalPaid = paymentRepository.findByReservationIdOrderByPaymentDateDesc(reservation.getId())
        .stream()
        .filter(payment -> payment.getStatus() != PaymentStatus.REFUNDED)
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal balanceAmount = totalAmount.subtract(totalPaid);

    if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) {
      balanceAmount = BigDecimal.ZERO;
    }

    reservation.setBalanceAmount(balanceAmount);

    if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
      reservation.setPaymentStatus(PaymentStatus.PENDING);
    } else if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
      reservation.setPaymentStatus(PaymentStatus.PAID);
    } else {
      reservation.setPaymentStatus(PaymentStatus.PARTIAL);
    }

    if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
      reservation.setPaymentStatus(PaymentStatus.PAID);
      reservation.setBalanceAmount(BigDecimal.ZERO);
    }
  }

  private void auditReservationTransition(
      Reservation reservation,
      ReservationActionRequest request,
      AuditAction action,
      String description
  ) {
    auditLogService.log(
        resolveActor(reservation.getProperty(), request != null ? request.actedByPhone() : null),
        reservation.getProperty(),
        AuditModule.RESERVATIONS,
        action,
        "Reservation",
        String.valueOf(reservation.getId()),
        description + appendNotes(request != null ? request.notes() : null)
    );
  }

  private UserAccount resolveBookedBy(Property property, String bookedByPhone) {
    if (bookedByPhone != null && !bookedByPhone.isBlank()) {
      return userAccountRepository.findByPhone(bookedByPhone)
          .orElseThrow(() -> new NotFoundException("Booked-by user not found for phone " + bookedByPhone));
    }

    return userAccountRepository.findByRole(com.example.pms.backendpms.model.UserRole.HOTEL_OWNER).stream()
        .filter(user -> user.getProperty() != null && user.getProperty().getId().equals(property.getId()))
        .findFirst()
        .orElse(null);
  }

  private UserAccount resolveActor(Property property, String actorPhone) {
    if (actorPhone != null && !actorPhone.isBlank()) {
      return userAccountRepository.findByPhone(actorPhone)
          .orElseThrow(() -> new NotFoundException("User not found for phone " + actorPhone));
    }

    return resolveBookedBy(property, null);
  }

  private ReservationRoomStatus mapReservationRoomStatus(ReservationStatus status) {
    return switch (status) {
      case CHECKED_IN -> ReservationRoomStatus.CHECKED_IN;
      case CHECKED_OUT -> ReservationRoomStatus.CHECKED_OUT;
      case CANCELLED, NO_SHOW -> ReservationRoomStatus.CANCELLED;
      default -> ReservationRoomStatus.ASSIGNED;
    };
  }

  private String generateReservationNumber(Property property) {
    long sequence = reservationRepository.count() + 1001;
    return property.getCode() + "-" + sequence;
  }

  private BigDecimal toBigDecimal(Double value) {
    return BigDecimal.valueOf(value != null ? value : 0D);
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private int calculateNights(LocalDate checkInDate, LocalDate checkOutDate) {
    if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
      throw new IllegalArgumentException("Check-out date must be after check-in date.");
    }

    return (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
  }

  private BigDecimal calculateTotalTax(
      Double requestedPerDayTax,
      int nights,
      int roomsCount,
      Boolean complimentary
  ) {
    if (Boolean.TRUE.equals(complimentary)) {
      return BigDecimal.ZERO;
    }

    BigDecimal perDayTax = toBigDecimal(requestedPerDayTax);
    return perDayTax.multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(Math.max(roomsCount, 1)));
  }

  private BigDecimal resolveNightlyRate(
      AssignedRoomRequest assignedRoom,
      RoomType roomType,
      Boolean complimentary
  ) {
    if (Boolean.TRUE.equals(complimentary)) {
      return BigDecimal.ZERO;
    }

    BigDecimal requestedRate = toBigDecimal(assignedRoom.nightlyRate());
    if (requestedRate.compareTo(BigDecimal.ZERO) > 0) {
      return requestedRate;
    }

    return BigDecimal.valueOf(roomType.getBaseRate() != null ? roomType.getBaseRate() : 0D);
  }

  private BigDecimal calculateRoomRevenue(Reservation reservation) {
    List<ReservationRoom> reservationRooms = reservationRoomRepository.findByReservationId(reservation.getId());

    if (reservationRooms.isEmpty()) {
      return safeAmount(reservation.getRoomAmount());
    }

    BigDecimal nightlyRateTotal = reservationRooms.stream()
        .map(room -> room.getNightlyRate() != null
            ? room.getNightlyRate()
            : BigDecimal.valueOf(room.getRoomType().getBaseRate() != null ? room.getRoomType().getBaseRate() : 0D))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return nightlyRateTotal.multiply(BigDecimal.valueOf(
        Math.max(reservation.getNights() != null ? reservation.getNights() : 1, 1)
    ));
  }

  private Company resolveCompany(Long propertyId, Long companyId) {
    if (companyId == null) {
      return null;
    }

    return companyService.getCompanyEntity(propertyId, companyId);
  }

  private BillToType resolveBillToType(String requestedBillToType, Company company) {
    if (requestedBillToType != null && !requestedBillToType.isBlank()) {
      return BillToType.valueOf(requestedBillToType.trim().toUpperCase(Locale.ROOT));
    }

    return company != null ? BillToType.COMPANY : BillToType.GUEST;
  }

  private PaymentMethod resolvePaymentMethod(String requestedPaymentMethod, BillToType billToType) {
    if (requestedPaymentMethod != null && !requestedPaymentMethod.isBlank()) {
      return PaymentMethod.valueOf(requestedPaymentMethod.trim().toUpperCase(Locale.ROOT));
    }

    return billToType == BillToType.COMPANY ? PaymentMethod.BILL_TO_COMPANY : PaymentMethod.CASH;
  }

  private void ensureRoomIsAvailableForReservation(Long propertyId, Reservation reservation, Long roomId) {
    boolean occupied = reservationRoomRepository.findByReservationPropertyIdOrderByAssignedFromAsc(propertyId).stream()
        .filter(assignment -> assignment.getRoom() != null && assignment.getRoom().getId().equals(roomId))
        .filter(assignment -> !assignment.getReservation().getId().equals(reservation.getId()))
        .filter(assignment -> assignment.getReservation().getStatus() == ReservationStatus.CONFIRMED
            || assignment.getReservation().getStatus() == ReservationStatus.CHECKED_IN)
        .anyMatch(assignment -> !assignment.getAssignedFrom().isAfter(reservation.getCheckOutDate().minusDays(1))
            && assignment.getAssignedTo().isAfter(reservation.getCheckInDate()));

    if (occupied) {
      throw new IllegalStateException("Selected room is already occupied for the stay range.");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String appendNotes(String notes) {
    String normalizedNotes = blankToNull(notes);
    return normalizedNotes != null ? " (" + normalizedNotes + ")" : "";
  }
}
