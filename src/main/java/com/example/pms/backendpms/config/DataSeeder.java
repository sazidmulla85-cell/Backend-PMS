package com.example.pms.backendpms.config;

import com.example.pms.backendpms.dto.AdminDtos.CreatePropertyAccountRequest;
import com.example.pms.backendpms.dto.AdminDtos.RoomTypeBlueprintRequest;
import com.example.pms.backendpms.dto.CompanyDtos.CreateCompanyRequest;
import com.example.pms.backendpms.dto.ReservationDtos.AssignedRoomRequest;
import com.example.pms.backendpms.dto.ReservationDtos.CreateReservationRequest;
import com.example.pms.backendpms.dto.ReservationDtos.GuestRequest;
import com.example.pms.backendpms.dto.ReservationDtos.RecordPaymentRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationSummaryResponse;
import com.example.pms.backendpms.model.CommunicationChannel;
import com.example.pms.backendpms.model.CommunicationStatus;
import com.example.pms.backendpms.model.CrmStage;
import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.PlatformInvoice;
import com.example.pms.backendpms.model.PlatformInvoicePayment;
import com.example.pms.backendpms.model.PlatformInvoiceStatus;
import com.example.pms.backendpms.model.PlatformPlan;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.PropertyCommunicationLog;
import com.example.pms.backendpms.model.Room;
import com.example.pms.backendpms.model.RoomStatus;
import com.example.pms.backendpms.model.SubscriptionStatus;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.OrganizationRepository;
import com.example.pms.backendpms.repository.PlatformInvoicePaymentRepository;
import com.example.pms.backendpms.repository.PlatformInvoiceRepository;
import com.example.pms.backendpms.repository.PlatformPlanRepository;
import com.example.pms.backendpms.repository.PropertyRepository;
import com.example.pms.backendpms.repository.PropertyCommunicationLogRepository;
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import com.example.pms.backendpms.service.AdminService;
import com.example.pms.backendpms.service.CompanyService;
import com.example.pms.backendpms.service.ReservationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@ConditionalOnProperty(value = "pms.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

  private final UserAccountRepository userAccountRepository;
  private final OrganizationRepository organizationRepository;
  private final PropertyRepository propertyRepository;
  private final RoomTypeRepository roomTypeRepository;
  private final RoomRepository roomRepository;
  private final ReservationRepository reservationRepository;
  private final PlatformPlanRepository platformPlanRepository;
  private final PlatformInvoiceRepository platformInvoiceRepository;
  private final PlatformInvoicePaymentRepository platformInvoicePaymentRepository;
  private final PropertyCommunicationLogRepository propertyCommunicationLogRepository;
  private final AdminService adminService;
  private final CompanyService companyService;
  private final ReservationService reservationService;
  private final PasswordEncoder passwordEncoder;

  @Value("${pms.super-admin.name}")
  private String superAdminName;

  @Value("${pms.super-admin.email}")
  private String superAdminEmail;

  @Value("${pms.super-admin.phone}")
  private String superAdminPhone;

  @Value("${pms.super-admin.password}")
  private String superAdminPassword;

  public DataSeeder(
      UserAccountRepository userAccountRepository,
      OrganizationRepository organizationRepository,
      PropertyRepository propertyRepository,
      RoomTypeRepository roomTypeRepository,
      RoomRepository roomRepository,
      ReservationRepository reservationRepository,
      PlatformPlanRepository platformPlanRepository,
      PlatformInvoiceRepository platformInvoiceRepository,
      PlatformInvoicePaymentRepository platformInvoicePaymentRepository,
      PropertyCommunicationLogRepository propertyCommunicationLogRepository,
      AdminService adminService,
      CompanyService companyService,
      ReservationService reservationService,
      PasswordEncoder passwordEncoder
  ) {
    this.userAccountRepository = userAccountRepository;
    this.organizationRepository = organizationRepository;
    this.propertyRepository = propertyRepository;
    this.roomTypeRepository = roomTypeRepository;
    this.roomRepository = roomRepository;
    this.reservationRepository = reservationRepository;
    this.platformPlanRepository = platformPlanRepository;
    this.platformInvoiceRepository = platformInvoiceRepository;
    this.platformInvoicePaymentRepository = platformInvoicePaymentRepository;
    this.propertyCommunicationLogRepository = propertyCommunicationLogRepository;
    this.adminService = adminService;
    this.companyService = companyService;
    this.reservationService = reservationService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    migratePlaintextPasswords();
    userAccountRepository.findByPhone(superAdminPhone).orElseGet(this::createSuperAdmin);
    seedPlatformPlans();

    Long dwarikaPropertyId = ensureProperty(new CreatePropertyAccountRequest(
        "Hotel Dwarika Hospitality",
        "Hotel Dwarika Hospitality Private Limited",
        "Hotel Dwarika Owner",
        "owner@hoteldwarika.local",
        "8888888888",
        "owner123",
        "Hotel Dwarika",
        "HDW",
        "frontdesk@hoteldwarika.local",
        "07682200001",
        "Tikamgarh",
        "Madhya Pradesh",
        "India",
        "Asia/Kolkata",
        "INR",
        10,
        List.of(
            new RoomTypeBlueprintRequest(
                "Deluxe Room",
                "DLX",
                2,
                3,
                1500D,
                List.of("102", "103", "104", "202", "203", "204")
            ),
            new RoomTypeBlueprintRequest(
                "Super Deluxe Room",
                "SDX",
                2,
                4,
                2100D,
                List.of("101", "105", "201", "205")
            )
        )
    ));

    Long whiteHousePropertyId = ensureProperty(new CreatePropertyAccountRequest(
        "The White House Hospitality",
        "The White House Hospitality Private Limited",
        "Rhea Thomas",
        "rhea@whitehousehotel.local",
        "9000011122",
        "whitehouse123",
        "The White House",
        "TWH",
        "frontdesk@whitehousehotel.local",
        "07552223344",
        "Bhopal",
        "Madhya Pradesh",
        "India",
        "Asia/Kolkata",
        "INR",
        28,
        List.of(
            new RoomTypeBlueprintRequest(
                "Classic Room",
                "CLR",
                2,
                2,
                2600D,
                List.of("201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212")
            ),
            new RoomTypeBlueprintRequest(
                "Premium King",
                "PKG",
                2,
                3,
                3400D,
                List.of("301", "302", "303", "305", "306", "307", "308", "309")
            ),
            new RoomTypeBlueprintRequest(
                "Executive Suite",
                "ESU",
                2,
                4,
                5200D,
                List.of("401", "402", "403", "404", "405", "406")
            )
        )
    ));

    Long lakesidePropertyId = ensureProperty(new CreatePropertyAccountRequest(
        "Lakeside Suites Collection",
        "Lakeside Suites Collection LLP",
        "Aman Verma",
        "aman@lakesidesuites.local",
        "9011122233",
        "lakeside123",
        "Lakeside Suites",
        "LKS",
        "frontdesk@lakesidesuites.local",
        "07314252626",
        "Indore",
        "Madhya Pradesh",
        "India",
        "Asia/Kolkata",
        "INR",
        16,
        List.of(
            new RoomTypeBlueprintRequest(
                "Courtyard Room",
                "CRY",
                2,
                3,
                2200D,
                List.of("101", "102", "103", "104", "105", "106", "107", "108")
            ),
            new RoomTypeBlueprintRequest(
                "Lake Suite",
                "LSU",
                2,
                4,
                3900D,
                List.of("201", "202", "203", "204", "205", "206", "207", "208")
            )
        )
    ));

    configureSubscriptionProfile("HDW", "Growth", SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(12), LocalDate.now().plusDays(18), 14990D, true);
    configureSubscriptionProfile("TWH", "Enterprise", SubscriptionStatus.DUE_SOON, LocalDate.now().minusDays(28), LocalDate.now().plusDays(4), 26672D, true);
    configureSubscriptionProfile("LKS", "Growth", SubscriptionStatus.OVERDUE, LocalDate.now().minusDays(40), LocalDate.now().minusDays(6), 18884D, false);

    markRoomStatus("HDW", "105", RoomStatus.MAINTENANCE);
    markRoomStatus("TWH", "406", RoomStatus.OUT_OF_ORDER);
    markRoomStatus("TWH", "210", RoomStatus.MAINTENANCE);
    markRoomStatus("LKS", "108", RoomStatus.MAINTENANCE);

    seedCompanies(dwarikaPropertyId);
    seedCompanies(whiteHousePropertyId);
    seedCompanies(lakesidePropertyId);

    seedDwarikaReservations(dwarikaPropertyId);
    seedWhiteHouseReservations(whiteHousePropertyId);
    seedLakesideReservations(lakesidePropertyId);

    enrichPropertyAdminProfile("HDW", "Sajid Success", CrmStage.ACTIVE_CUSTOMER, "Dashboard,Stay View,Rooms,Reservations,Companies,Audit Logs", "Healthy rollout. Good candidate for advanced reports.", "Growth customer, expected annual upsell for reports and automation.");
    enrichPropertyAdminProfile("TWH", "Neha Accounts", CrmStage.DEMO, "Dashboard,Stay View,Rooms,Reservations,Companies,Audit Logs", "Configured well but still missing a few live rooms against subscription.", "Enterprise prospect needs tighter billing follow-up and formal invoice sharing.");
    enrichPropertyAdminProfile("LKS", "Rohit Retention", CrmStage.TRIAL, "Dashboard,Stay View,Rooms,Reservations,Audit Logs", "Owner is active but billing needs support follow-up because account is overdue.", "Could convert cleanly once platform billing is regularized.");

    seedPlatformLedger("HDW", "PLT-HDW-1001", LocalDate.now().withDayOfMonth(1), LocalDate.now().plusDays(10), 14990D, 14990D, PlatformInvoiceStatus.PAID, "UPI", "SUB-HDW-APR");
    seedPlatformLedger("TWH", "PLT-TWH-1002", LocalDate.now().withDayOfMonth(1), LocalDate.now().plusDays(4), 26672D, 12000D, PlatformInvoiceStatus.PARTIAL, "BANK_TRANSFER", "SUB-TWH-APR");
    seedPlatformLedger("LKS", "PLT-LKS-1003", LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now().minusDays(6), 18884D, 0D, PlatformInvoiceStatus.OVERDUE, null, null);

    seedCommunication("HDW", CommunicationChannel.EMAIL, "Welcome to platform operations", "Shared onboarding checkpoints, companies workflow, and first billing guidance.");
    seedCommunication("TWH", CommunicationChannel.CALL, "Room subscription follow-up", "Discussed mismatch between subscribed rooms and live configured rooms.");
    seedCommunication("LKS", CommunicationChannel.SMS, "Subscription overdue reminder", "Reminder sent for overdue platform subscription before operational suspension.");
  }

  private Long ensureProperty(CreatePropertyAccountRequest request) {
    String code = request.propertyCode().trim().toUpperCase();
    return propertyRepository.findByCode(code)
        .map(property -> ensureExistingPropertyAccount(property, request).getId())
        .orElseGet(() -> adminService.createPropertyAccount(request).propertyId());
  }

  private Property ensureExistingPropertyAccount(Property property, CreatePropertyAccountRequest request) {
    UserAccount owner = userAccountRepository.findByEmailIgnoreCase(request.ownerEmail())
        .or(() -> userAccountRepository.findByPhone(request.ownerPhone()))
        .orElseGet(() -> createOwnerAccount(property.getOrganization(), request));

    owner.setOrganization(property.getOrganization());
    owner.setFullName(request.ownerFullName());
    owner.setEmail(request.ownerEmail());
    owner.setPhone(request.ownerPhone());
    owner.setPassword(encodePassword(request.ownerPassword()));
    owner.setRole(UserRole.HOTEL_OWNER);
    owner.setProperty(property);
    owner.setActive(true);
    owner = userAccountRepository.save(owner);

    property.setOwner(owner);
    property.setEmail(request.propertyEmail());
    property.setPhone(request.propertyPhone());
    property.setCity(request.city());
    property.setState(request.state());
    property.setCountry(request.country());
    property.setTimezone(request.timezone());
    property.setCurrencyCode(request.currencyCode());
    property.setSubscribedRoomCount(request.subscribedRoomCount());
    property.setSubscriptionPlan(resolvePlanName(request.subscribedRoomCount()));
    property.setSubscriptionStatus(property.getSubscriptionStatus() != null ? property.getSubscriptionStatus() : SubscriptionStatus.ACTIVE);
    property.setSubscriptionStartDate(
        property.getSubscriptionStartDate() != null ? property.getSubscriptionStartDate() : LocalDate.now().minusDays(10)
    );
    property.setRenewalDate(
        property.getRenewalDate() != null ? property.getRenewalDate() : LocalDate.now().plusDays(20)
    );
    property.setMonthlySubscriptionAmount(
        property.getMonthlySubscriptionAmount() != null
            ? property.getMonthlySubscriptionAmount()
            : calculateMonthlySubscriptionAmount(request.subscribedRoomCount())
    );
    property.setAutoRenew(property.isAutoRenew());
    return propertyRepository.save(property);
  }

  private void configureSubscriptionProfile(
      String propertyCode,
      String plan,
      SubscriptionStatus status,
      LocalDate startDate,
      LocalDate renewalDate,
      double monthlyAmount,
      boolean autoRenew
  ) {
    propertyRepository.findByCode(propertyCode).ifPresent((property) -> {
      property.setSubscriptionPlan(plan);
      property.setSubscriptionStatus(status);
      property.setSubscriptionStartDate(startDate);
      property.setRenewalDate(renewalDate);
      property.setMonthlySubscriptionAmount(monthlyAmount);
      property.setAutoRenew(autoRenew);
      propertyRepository.save(property);
    });
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

  private UserAccount createOwnerAccount(Organization organization, CreatePropertyAccountRequest request) {
    UserAccount owner = new UserAccount();
    owner.setOrganization(organization);
    owner.setFullName(request.ownerFullName());
    owner.setEmail(request.ownerEmail());
    owner.setPhone(request.ownerPhone());
    owner.setPassword(encodePassword(request.ownerPassword()));
    owner.setRole(UserRole.HOTEL_OWNER);
    owner.setActive(true);
    return owner;
  }

  private void seedDwarikaReservations(Long propertyId) {
    if (!reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).isEmpty()) {
      return;
    }

    LocalDate today = LocalDate.now();

    ReservationSummaryResponse mohanReservation = createReservation(
        propertyId,
        "PMS",
        "CHECKED_IN",
        today.minusDays(1),
        today.plusDays(1),
        1,
        0,
        "MAP",
        "EP",
        "Corporate guest",
        2400D,
        "8888888888",
        new GuestRequest("Mohan Singh", "mohan@example.com", "9000000001", "India", "Madhya Pradesh", "Tikamgarh", "472001", "Tikamgarh", "Aadhaar", "1234", "Corporate booking"),
        "Deluxe Room",
        "203",
        1200D
    );
    recordPayment(propertyId, mohanReservation.reservationId(), 1200D, "UPI", "HDW-PAY-001");

    createReservation(
        propertyId,
        "WALK_IN",
        "CONFIRMED",
        today,
        today.plusDays(2),
        2,
        0,
        "CP",
        "CP",
        null,
        3200D,
        "8888888888",
        new GuestRequest("Anita Sharma", "anita@example.com", "9000000004", "India", "Madhya Pradesh", "Tikamgarh", "472001", "Tikamgarh", "PAN", "ZZ1234", null),
        "Deluxe Room",
        "204",
        1600D
    );

    createReservation(
        propertyId,
        "PMS",
        "CONFIRMED",
        today.plusDays(1),
        today.plusDays(3),
        2,
        0,
        "CP",
        "CP",
        null,
        2600D,
        "8888888888",
        new GuestRequest("Atul Jha", "atul@example.com", "9000000003", "India", "Madhya Pradesh", "Tikamgarh", "472001", "Tikamgarh", "Driving License", "DL12", null),
        "Super Deluxe Room",
        "201",
        1300D
    );

    createReservation(
        propertyId,
        "ONLINE",
        "CONFIRMED",
        today.plusDays(2),
        today.plusDays(4),
        2,
        0,
        "MAP",
        "MAP",
        "OTA booking",
        3600D,
        "8888888888",
        new GuestRequest("Priya Nair", "priya@example.com", "9000000005", "India", "Madhya Pradesh", "Tikamgarh", "472001", "Tikamgarh", "Passport", "P1234", "OTA booking"),
        "Deluxe Room",
        "103",
        1800D
    );

    ReservationSummaryResponse checkedOutReservation = createReservation(
        propertyId,
        "ONLINE",
        "CHECKED_OUT",
        today.minusDays(4),
        today.minusDays(2),
        2,
        0,
        "CP",
        "BAR",
        null,
        2800D,
        "8888888888",
        new GuestRequest("Ritika Soni", "ritika@example.com", "9000000006", "India", "Madhya Pradesh", "Tikamgarh", "472001", "Tikamgarh", "Passport", "RS2211", null),
        "Deluxe Room",
        "102",
        1400D
    );
    recordPayment(propertyId, checkedOutReservation.reservationId(), 2800D, "CARD", "HDW-PAY-002");
  }

  private void seedWhiteHouseReservations(Long propertyId) {
    if (!reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).isEmpty()) {
      return;
    }

    LocalDate today = LocalDate.now();

    ReservationSummaryResponse rahulReservation = createReservation(
        propertyId,
        "CORPORATE",
        "CHECKED_IN",
        today.minusDays(2),
        today.plusDays(1),
        2,
        0,
        "CP",
        "BAR",
        "VIP arrival",
        10200D,
        "9000011122",
        new GuestRequest("Rahul Sethi", "rahul.sethi@example.com", "9100000001", "India", "Madhya Pradesh", "Bhopal", "462001", "Bhopal", "Aadhaar", "TX9123", "Corporate stay"),
        "Executive Suite",
        "401",
        3400D
    );
    recordPayment(propertyId, rahulReservation.reservationId(), 5000D, "BANK_TRANSFER", "TWH-PAY-001");

    ReservationSummaryResponse saraReservation = createReservation(
        propertyId,
        "ONLINE",
        "CONFIRMED",
        today,
        today.plusDays(2),
        2,
        1,
        "MAP",
        "MAP",
        null,
        6800D,
        "9000011122",
        new GuestRequest("Sara Khan", "sara@example.com", "9100000002", "India", "Madhya Pradesh", "Bhopal", "462001", "Bhopal", "PAN", "SK4432", null),
        "Premium King",
        "301",
        3400D
    );
    recordPayment(propertyId, saraReservation.reservationId(), 6800D, "CARD", "TWH-PAY-002");

    createReservation(
        propertyId,
        "PMS",
        "CONFIRMED",
        today.plusDays(1),
        today.plusDays(3),
        1,
        0,
        "EP",
        "BAR",
        null,
        5200D,
        "9000011122",
        new GuestRequest("Daniel Dsouza", "daniel@example.com", "9100000003", "India", "Madhya Pradesh", "Bhopal", "462001", "Bhopal", "Driving License", "DL9231", null),
        "Executive Suite",
        "402",
        2600D
    );

    createReservation(
        propertyId,
        "ONLINE",
        "CANCELLED",
        today.plusDays(4),
        today.plusDays(6),
        2,
        0,
        "EP",
        "BAR",
        "Cancelled by guest before arrival",
        5200D,
        "9000011122",
        new GuestRequest("Sana Mirza", "sana@example.com", "9100000004", "India", "Madhya Pradesh", "Bhopal", "462001", "Bhopal", "Aadhaar", "SM8721", null),
        "Premium King",
        "302",
        2600D
    );

    createReservation(
        propertyId,
        "PMS",
        "NO_SHOW",
        today.minusDays(1),
        today.plusDays(1),
        1,
        0,
        "EP",
        "BAR",
        "Guest did not arrive",
        2600D,
        "9000011122",
        new GuestRequest("Vikram Rao", "vikram@example.com", "9100000005", "India", "Madhya Pradesh", "Bhopal", "462001", "Bhopal", "Driving License", "VR1122", null),
        "Classic Room",
        "205",
        1300D
    );
  }

  private void seedLakesideReservations(Long propertyId) {
    if (!reservationRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).isEmpty()) {
      return;
    }

    LocalDate today = LocalDate.now();

    ReservationSummaryResponse nishaReservation = createReservation(
        propertyId,
        "PMS",
        "CHECKED_IN",
        today.minusDays(1),
        today.plusDays(2),
        2,
        0,
        "CP",
        "EP",
        null,
        6600D,
        "9011122233",
        new GuestRequest("Nisha Verma", "nisha@example.com", "9200000001", "India", "Madhya Pradesh", "Indore", "452001", "Indore", "Aadhaar", "NV2233", null),
        "Lake Suite",
        "201",
        2200D
    );
    recordPayment(propertyId, nishaReservation.reservationId(), 6600D, "UPI", "LKS-PAY-001");

    ReservationSummaryResponse karanReservation = createReservation(
        propertyId,
        "WALK_IN",
        "CONFIRMED",
        today,
        today.plusDays(1),
        1,
        0,
        "EP",
        "EP",
        null,
        2200D,
        "9011122233",
        new GuestRequest("Karan Mehta", "karan@example.com", "9200000002", "India", "Madhya Pradesh", "Indore", "452001", "Indore", "PAN", "KM8432", null),
        "Courtyard Room",
        "101",
        2200D
    );
    recordPayment(propertyId, karanReservation.reservationId(), 1000D, "CASH", "LKS-PAY-002");

    createReservation(
        propertyId,
        "ONLINE",
        "CONFIRMED",
        today.plusDays(3),
        today.plusDays(5),
        2,
        0,
        "MAP",
        "MAP",
        null,
        7800D,
        "9011122233",
        new GuestRequest("Ira Joseph", "ira@example.com", "9200000003", "India", "Madhya Pradesh", "Indore", "452001", "Indore", "Passport", "IJ2232", null),
        "Lake Suite",
        "202",
        3900D
    );
  }

  private ReservationSummaryResponse createReservation(
      Long propertyId,
      String source,
      String status,
      LocalDate checkIn,
      LocalDate checkOut,
      int adults,
      int children,
      String mealPlan,
      String ratePlan,
      String specialRequest,
      double amount,
      String bookedByPhone,
      GuestRequest guest,
      String roomTypeName,
      String roomNumber,
      double nightlyRate
  ) {
    Long roomTypeId = roomTypeRepository.findByPropertyIdAndNameIgnoreCase(propertyId, roomTypeName)
        .orElseThrow()
        .getId();
    Long roomId = roomRepository.findByPropertyIdAndRoomNumber(propertyId, roomNumber)
        .orElseThrow()
        .getId();

    return reservationService.createReservation(propertyId, new CreateReservationRequest(
        source,
        status,
        checkIn,
        checkOut,
        adults,
        children,
        mealPlan,
        ratePlan,
        specialRequest,
        amount,
        0D,
        0D,
        bookedByPhone,
        null,
        null,
        null,
        false,
        false,
        null,
        guest,
        List.of(new AssignedRoomRequest(roomTypeId, roomId, nightlyRate))
    ));
  }

  private void seedCompanies(Long propertyId) {
    if (!companyService.getCompanies(propertyId).isEmpty()) {
      return;
    }

    companyService.createCompany(propertyId, new CreateCompanyRequest(
        "Anand Singh Family",
        null,
        "Anand Singh",
        "9300000001",
        "ledger@anandsinghfamily.local",
        "Bhopal",
        "Arera Colony"
    ));
    companyService.createCompany(propertyId, new CreateCompanyRequest(
        "Goibibo Prepaid",
        "22AAAAA0000A1Z5",
        "OTA Desk",
        "9300000002",
        "ota@goibibo-prepaid.local",
        "Gurugram",
        "Sector 44"
    ));
    companyService.createCompany(propertyId, new CreateCompanyRequest(
        "Redbus India Private Limited",
        "23AAHCP1178L1Z8",
        "Corporate Travel Desk",
        "9300000003",
        "finance@redbus.local",
        "Bengaluru",
        "Outer Ring Road"
    ));
  }

  private void recordPayment(
      Long propertyId,
      Long reservationId,
      double amount,
      String method,
      String referenceNumber
  ) {
    reservationService.recordPayment(
        propertyId,
        reservationId,
        new RecordPaymentRequest(amount, method, referenceNumber, "Seeded payment", null)
    );
  }

  private void markRoomStatus(String propertyCode, String roomNumber, RoomStatus status) {
    propertyRepository.findByCode(propertyCode)
        .flatMap(property -> roomRepository.findByPropertyIdAndRoomNumber(property.getId(), roomNumber))
        .ifPresent(room -> updateRoomStatus(room, status));
  }

  private void updateRoomStatus(Room room, RoomStatus status) {
    room.setStatus(status);
    roomRepository.save(room);
  }

  private UserAccount createSuperAdmin() {
    Organization organization = new Organization();
    organization.setName("Codex PMS Platform");
    organization.setLegalName("Codex PMS Platform");
    organization.setBillingEmail("billing@codexpms.local");
    organization.setBillingPhone("9999999999");
    organization.setBillingAddress("Indore Platform Office");
    organization = organizationRepository.save(organization);

    UserAccount superAdmin = new UserAccount();
    superAdmin.setOrganization(organization);
    superAdmin.setFullName(superAdminName);
    superAdmin.setEmail(superAdminEmail);
    superAdmin.setPhone(superAdminPhone);
    superAdmin.setPassword(encodePassword(superAdminPassword));
    superAdmin.setRole(UserRole.SUPER_ADMIN);
    return userAccountRepository.save(superAdmin);
  }

  private void migratePlaintextPasswords() {
    userAccountRepository.findAll().forEach((user) -> {
      if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")
          && !user.getPassword().startsWith("$2b$") && !user.getPassword().startsWith("$2y$")) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userAccountRepository.save(user);
      }
    });
  }

  private String encodePassword(String rawPassword) {
    if (rawPassword != null && (rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$")
        || rawPassword.startsWith("$2y$"))) {
      return rawPassword;
    }
    return passwordEncoder.encode(rawPassword);
  }

  private void seedPlatformPlans() {
    if (platformPlanRepository.count() > 0) {
      return;
    }

    platformPlanRepository.save(createPlan(
        "STARTER",
        "Starter",
        12,
        8500D,
        649D,
        "Best for smaller single-property hotels onboarding core PMS operations.",
        "Dashboard,Stay View,Rooms,Reservations,Audit Logs"
    ));
    platformPlanRepository.save(createPlan(
        "GROWTH",
        "Growth",
        25,
        14990D,
        699D,
        "For scaling hotels that need companies, billing controls, and operational adoption support.",
        "Dashboard,Stay View,Rooms,Reservations,Companies,Audit Logs"
    ));
    platformPlanRepository.save(createPlan(
        "ENTERPRISE",
        "Enterprise",
        60,
        24990D,
        799D,
        "For larger hotels requiring stronger reporting, support management, and advanced rollout visibility.",
        "Dashboard,Stay View,Rooms,Reservations,Companies,Audit Logs,Platform Reports,Support"
    ));
  }

  private PlatformPlan createPlan(
      String code,
      String name,
      int includedRooms,
      double baseMonthlyAmount,
      double perRoomAmount,
      String description,
      String modulesCsv
  ) {
    PlatformPlan plan = new PlatformPlan();
    plan.setCode(code);
    plan.setName(name);
    plan.setIncludedRooms(includedRooms);
    plan.setBaseMonthlyAmount(baseMonthlyAmount);
    plan.setPerRoomAmount(perRoomAmount);
    plan.setDescription(description);
    plan.setModuleCodesCsv(modulesCsv);
    plan.setActive(true);
    return plan;
  }

  private void enrichPropertyAdminProfile(
      String propertyCode,
      String accountManager,
      CrmStage crmStage,
      String modulesCsv,
      String supportNotes,
      String commercialNotes
  ) {
    propertyRepository.findByCode(propertyCode).ifPresent(property -> {
      property.setAccountManager(accountManager);
      property.setCrmStage(crmStage);
      property.setModuleEntitlementsCsv(modulesCsv);
      property.setSupportNotes(supportNotes);
      property.setCommercialNotes(commercialNotes);
      propertyRepository.save(property);

      Organization organization = property.getOrganization();
      if (organization.getBillingEmail() == null) {
        organization.setBillingEmail("accounts@" + propertyCode.toLowerCase() + ".local");
      }
      if (organization.getBillingPhone() == null) {
        organization.setBillingPhone(property.getPhone());
      }
      if (organization.getBillingAddress() == null) {
        organization.setBillingAddress(property.getCity() + ", " + property.getState());
      }
      if (organization.getGstNumber() == null) {
        organization.setGstNumber("23ABCDE" + propertyCode.length() + "F1Z5");
      }
      organizationRepository.save(organization);

      UserAccount owner = property.getOwner();
      if (owner.getLastLoginAt() == null) {
        owner.setLastLoginAt(LocalDateTime.now().minusDays("LKS".equals(propertyCode) ? 18 : 3));
        userAccountRepository.save(owner);
      }
    });
  }

  private void seedPlatformLedger(
      String propertyCode,
      String invoiceNumber,
      LocalDate billingMonth,
      LocalDate dueDate,
      double totalAmount,
      double paidAmount,
      PlatformInvoiceStatus status,
      String paymentMethod,
      String paymentReference
  ) {
    Property property = propertyRepository.findByCode(propertyCode).orElse(null);
    if (property == null || !platformInvoiceRepository.findByPropertyIdOrderByBillingMonthDesc(property.getId()).isEmpty()) {
      return;
    }

    PlatformInvoice invoice = new PlatformInvoice();
    invoice.setProperty(property);
    invoice.setInvoiceNumber(invoiceNumber);
    invoice.setPlanName(property.getSubscriptionPlan());
    invoice.setBillingMonth(billingMonth);
    invoice.setDueDate(dueDate);
    invoice.setTotalAmount(totalAmount);
    invoice.setPaidAmount(paidAmount);
    invoice.setStatus(status);
    invoice.setNotes("Seeded platform billing invoice for " + property.getName());
    invoice = platformInvoiceRepository.save(invoice);

    if (paidAmount > 0D && paymentMethod != null) {
      PlatformInvoicePayment payment = new PlatformInvoicePayment();
      payment.setInvoice(invoice);
      payment.setAmount(paidAmount);
      payment.setPaymentMethod(paymentMethod);
      payment.setReferenceNumber(paymentReference);
      payment.setReceivedAt(LocalDateTime.now().minusDays(2));
      platformInvoicePaymentRepository.save(payment);
    }
  }

  private void seedCommunication(
      String propertyCode,
      CommunicationChannel channel,
      String subject,
      String message
  ) {
    Property property = propertyRepository.findByCode(propertyCode).orElse(null);
    if (property == null || !propertyCommunicationLogRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId()).isEmpty()) {
      return;
    }

    PropertyCommunicationLog log = new PropertyCommunicationLog();
    log.setProperty(property);
    log.setChannel(channel);
    log.setStatus(CommunicationStatus.SENT);
    log.setSubject(subject);
    log.setMessage(message);
    log.setActorName("Super Admin");
    propertyCommunicationLogRepository.save(log);
  }
}
