package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.AdminDtos.AdminOverviewResponse;
import com.example.pms.backendpms.dto.AdminDtos.PropertyAccountSummary;
import com.example.pms.backendpms.dto.AdminDtos.RoomTypeBlueprintRequest;
import com.example.pms.backendpms.dto.AdminDtos.CreatePropertyAccountRequest;
import com.example.pms.backendpms.dto.AdminDtos.UpdatePropertyAccountRequest;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.HousekeepingStatus;
import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.model.RoomType;
import com.example.pms.backendpms.model.SubscriptionStatus;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.OrganizationRepository;
import com.example.pms.backendpms.repository.PropertyRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  private final OrganizationRepository organizationRepository;
  private final UserAccountRepository userAccountRepository;
  private final PropertyRepository propertyRepository;
  private final RoomTypeRepository roomTypeRepository;
  private final RoomRepository roomRepository;
  private final AuditLogService auditLogService;
  private final SubscriptionLifecycleService subscriptionLifecycleService;
  private final PasswordEncoder passwordEncoder;

  public AdminService(
      OrganizationRepository organizationRepository,
      UserAccountRepository userAccountRepository,
      PropertyRepository propertyRepository,
      RoomTypeRepository roomTypeRepository,
      RoomRepository roomRepository,
      AuditLogService auditLogService,
      SubscriptionLifecycleService subscriptionLifecycleService,
      PasswordEncoder passwordEncoder
  ) {
    this.organizationRepository = organizationRepository;
    this.userAccountRepository = userAccountRepository;
    this.propertyRepository = propertyRepository;
    this.roomTypeRepository = roomTypeRepository;
    this.roomRepository = roomRepository;
    this.auditLogService = auditLogService;
    this.subscriptionLifecycleService = subscriptionLifecycleService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AdminOverviewResponse getOverview() {
    List<PropertyAccountSummary> properties = propertyRepository.findAllByOrderByNameAsc().stream()
        .peek(this::refreshSubscriptionState)
        .map(this::toSummary)
        .toList();

    return new AdminOverviewResponse(
        userAccountRepository.countByRole(UserRole.SUPER_ADMIN),
        userAccountRepository.countByRole(UserRole.HOTEL_OWNER),
        propertyRepository.countByActiveTrue(),
        properties
    );
  }

  @Transactional
  public PropertyAccountSummary createPropertyAccount(CreatePropertyAccountRequest request) {
    validateBlueprint(request);

    Organization organization = organizationRepository.findByNameIgnoreCase(request.organizationName())
        .orElseGet(() -> {
          Organization nextOrganization = new Organization();
          nextOrganization.setName(request.organizationName());
          nextOrganization.setLegalName(request.legalName());
          return organizationRepository.save(nextOrganization);
        });

    if (userAccountRepository.findByPhone(request.ownerPhone()).isPresent()) {
      throw new IllegalArgumentException("An owner account already exists with this phone number.");
    }

    if (request.ownerEmail() != null && !request.ownerEmail().isBlank()
        && userAccountRepository.findByEmailIgnoreCase(request.ownerEmail()).isPresent()) {
      throw new IllegalArgumentException("An owner account already exists with this email.");
    }

    String normalizedCode = request.propertyCode().trim().toUpperCase(Locale.ROOT);

    if (propertyRepository.findByCode(normalizedCode).isPresent()) {
      throw new IllegalArgumentException("Property code already exists. Please use a unique code.");
    }

    UserAccount owner = new UserAccount();
    owner.setOrganization(organization);
    owner.setFullName(request.ownerFullName());
    owner.setEmail(blankToNull(request.ownerEmail()));
    owner.setPhone(request.ownerPhone());
    owner.setPassword(passwordEncoder.encode(request.ownerPassword()));
    owner.setRole(UserRole.HOTEL_OWNER);
    owner = userAccountRepository.save(owner);

    Property property = new Property();
    property.setOrganization(organization);
    property.setOwner(owner);
    property.setName(request.propertyName());
    property.setCode(normalizedCode);
    property.setEmail(blankToNull(request.propertyEmail()));
    property.setPhone(blankToNull(request.propertyPhone()));
    property.setCity(request.city());
    property.setState(request.state());
    property.setCountry(request.country());
    property.setTimezone(blankToNull(request.timezone()) != null ? request.timezone() : "Asia/Kolkata");
    property.setCurrencyCode(blankToNull(request.currencyCode()) != null ? request.currencyCode() : "INR");
    property.setSubscribedRoomCount(request.subscribedRoomCount());
    property.setSubscriptionPlan(resolvePlanName(request.subscribedRoomCount()));
    property.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
    property.setSubscriptionStartDate(LocalDate.now());
    property.setRenewalDate(LocalDate.now().plusMonths(1));
    property.setMonthlySubscriptionAmount(calculateMonthlySubscriptionAmount(request.subscribedRoomCount()));
    property.setAutoRenew(true);
    property = propertyRepository.save(property);

    owner.setProperty(property);
    userAccountRepository.save(owner);

    List<Room> rooms = new ArrayList<>();

    for (RoomTypeBlueprintRequest blueprint : request.roomTypes()) {
      RoomType roomType = new RoomType();
      roomType.setProperty(property);
      roomType.setName(blueprint.name());
      roomType.setCode(blueprint.code().trim().toUpperCase(Locale.ROOT));
      roomType.setBaseOccupancy(blueprint.baseOccupancy());
      roomType.setMaxOccupancy(blueprint.maxOccupancy());
      roomType.setBaseRate(blueprint.baseRate());
      roomType = roomTypeRepository.save(roomType);

      for (String roomNumber : blueprint.roomNumbers()) {
        Room room = new Room();
        room.setProperty(property);
        room.setRoomType(roomType);
        room.setRoomNumber(roomNumber.trim());
        room.setFloorName(extractFloorName(roomNumber));
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        rooms.add(room);
      }
    }

    roomRepository.saveAll(rooms);

    UserAccount superAdmin = userAccountRepository.findByRole(UserRole.SUPER_ADMIN).stream().findFirst()
        .orElseThrow(() -> new NotFoundException("Super admin account not found for audit logging."));

    auditLogService.log(
        superAdmin,
        property,
        AuditModule.ADMIN,
        AuditAction.CREATE,
        "Property",
        String.valueOf(property.getId()),
        "Created property " + property.getName() + " with " + rooms.size() + " rooms for owner "
            + owner.getFullName()
    );

    return toSummary(property);
  }

  @Transactional
  public PropertyAccountSummary updatePropertyAccount(Long propertyId, UpdatePropertyAccountRequest request) {
    Property property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));
    UserAccount owner = property.getOwner();

    validateOwnerIdentityUpdate(owner, request);

    owner.setFullName(request.ownerFullName().trim());
    owner.setEmail(blankToNull(request.ownerEmail()));
    owner.setPhone(request.ownerPhone().trim());
    owner.setActive(Boolean.TRUE.equals(request.ownerActive()));

    String nextPassword = blankToNull(request.ownerPassword());
    if (nextPassword != null) {
      owner.setPassword(passwordEncoder.encode(nextPassword));
    }
    userAccountRepository.save(owner);

    property.setName(request.propertyName().trim());
    property.setEmail(blankToNull(request.propertyEmail()));
    property.setPhone(blankToNull(request.propertyPhone()));
    property.setCity(request.city().trim());
    property.setState(request.state().trim());
    property.setCountry(request.country().trim());
    property.setTimezone(blankToNull(request.timezone()) != null ? request.timezone().trim() : property.getTimezone());
    property.setCurrencyCode(blankToNull(request.currencyCode()) != null ? request.currencyCode().trim() : property.getCurrencyCode());
    property.setSubscribedRoomCount(request.subscribedRoomCount());
    property.setSubscriptionPlan(request.subscriptionPlan().trim());
    property.setSubscriptionStatus(request.subscriptionStatus());
    property.setSubscriptionStartDate(request.subscriptionStartDate());
    property.setRenewalDate(request.renewalDate());
    property.setMonthlySubscriptionAmount(request.monthlySubscriptionAmount());
    property.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
    property.setActive(Boolean.TRUE.equals(request.propertyActive()));
    refreshSubscriptionState(property);
    property = propertyRepository.save(property);

    auditLogService.log(
        userAccountRepository.findByRole(UserRole.SUPER_ADMIN).stream().findFirst().orElse(null),
        property,
        AuditModule.ADMIN,
        AuditAction.UPDATE,
        "Property",
        String.valueOf(property.getId()),
        "Updated property account for " + property.getName()
    );

    return toSummary(property);
  }

  @Transactional
  public PropertyAccountSummary renewPropertySubscription(Long propertyId) {
    Property property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));

    LocalDate baseDate = property.getRenewalDate() != null && property.getRenewalDate().isAfter(LocalDate.now())
        ? property.getRenewalDate()
        : LocalDate.now();

    property.setSubscriptionStartDate(LocalDate.now());
    property.setRenewalDate(baseDate.plusMonths(1));
    property.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
    property.setAutoRenew(true);
    refreshSubscriptionState(property);
    property = propertyRepository.save(property);

    logAdminAction(property, "Renewed subscription for " + property.getName());
    return toSummary(property);
  }

  @Transactional
  public PropertyAccountSummary suspendPropertySubscription(Long propertyId) {
    Property property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));

    property.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
    property = propertyRepository.save(property);

    logAdminAction(property, "Suspended subscription for " + property.getName());
    return toSummary(property);
  }

  @Transactional
  public PropertyAccountSummary reactivatePropertySubscription(Long propertyId) {
    Property property = propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));

    property.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
    refreshSubscriptionState(property);
    property = propertyRepository.save(property);

    logAdminAction(property, "Reactivated subscription for " + property.getName());
    return toSummary(property);
  }

  private void validateBlueprint(CreatePropertyAccountRequest request) {
    Set<String> roomNumbers = new HashSet<>();
    int totalRooms = 0;

    for (RoomTypeBlueprintRequest roomType : request.roomTypes()) {
      totalRooms += roomType.roomNumbers().size();

      for (String roomNumber : roomType.roomNumbers()) {
        String normalized = roomNumber.trim().toUpperCase(Locale.ROOT);

        if (!roomNumbers.add(normalized)) {
          throw new IllegalArgumentException("Duplicate room number detected: " + roomNumber);
        }
      }
    }

    if (totalRooms > request.subscribedRoomCount()) {
      throw new IllegalArgumentException("Room blueprint exceeds subscribed room count.");
    }
  }

  private void validateOwnerIdentityUpdate(UserAccount owner, UpdatePropertyAccountRequest request) {
    userAccountRepository.findByPhone(request.ownerPhone().trim())
        .filter(existing -> !existing.getId().equals(owner.getId()))
        .ifPresent(existing -> {
          throw new IllegalArgumentException("Another account already uses this owner phone number.");
        });

    String ownerEmail = blankToNull(request.ownerEmail());
    if (ownerEmail != null) {
      userAccountRepository.findByEmailIgnoreCase(ownerEmail)
          .filter(existing -> !existing.getId().equals(owner.getId()))
          .ifPresent(existing -> {
            throw new IllegalArgumentException("Another account already uses this owner email.");
          });
    }
  }

  private void refreshSubscriptionState(Property property) {
    subscriptionLifecycleService.refreshStatus(property);
  }

  private PropertyAccountSummary toSummary(Property property) {
    refreshSubscriptionState(property);
    return new PropertyAccountSummary(
        property.getId(),
        property.getName(),
        property.getCode(),
        property.getEmail(),
        property.getPhone(),
        property.getOwner().getId(),
        property.getOwner().getFullName(),
        property.getOwner().getPhone(),
        property.getOwner().getEmail(),
        property.isActive(),
        property.getOwner().isActive(),
        property.getSubscribedRoomCount(),
        roomRepository.countByPropertyId(property.getId()),
        property.getCity(),
        property.getState(),
        property.getCountry(),
        property.getTimezone(),
        property.getCurrencyCode(),
        property.getSubscriptionPlan() != null ? property.getSubscriptionPlan() : resolvePlanName(property.getSubscribedRoomCount()),
        property.getSubscriptionStatus() != null ? property.getSubscriptionStatus() : SubscriptionStatus.ACTIVE,
        property.getSubscriptionStartDate() != null ? property.getSubscriptionStartDate() : LocalDate.now(),
        property.getRenewalDate() != null ? property.getRenewalDate() : LocalDate.now().plusMonths(1),
        property.getMonthlySubscriptionAmount() != null
            ? property.getMonthlySubscriptionAmount()
            : calculateMonthlySubscriptionAmount(property.getSubscribedRoomCount()),
        property.isAutoRenew()
    );
  }

  private void logAdminAction(Property property, String message) {
    auditLogService.log(
        userAccountRepository.findByRole(UserRole.SUPER_ADMIN).stream().findFirst().orElse(null),
        property,
        AuditModule.ADMIN,
        AuditAction.UPDATE,
        "Property",
        String.valueOf(property.getId()),
        message
    );
  }

  private double calculateMonthlySubscriptionAmount(Integer subscribedRoomCount) {
    int roomCount = subscribedRoomCount != null ? subscribedRoomCount : 0;
    return 8500D + roomCount * 649D;
  }

  private String resolvePlanName(Integer subscribedRoomCount) {
    int roomCount = subscribedRoomCount != null ? subscribedRoomCount : 0;

    if (roomCount >= 25) {
      return "Enterprise";
    }

    if (roomCount >= 12) {
      return "Growth";
    }

    return "Starter";
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String extractFloorName(String roomNumber) {
    if (roomNumber != null && roomNumber.length() >= 1 && Character.isDigit(roomNumber.charAt(0))) {
      return "Floor " + roomNumber.charAt(0);
    }

    return null;
  }
}
