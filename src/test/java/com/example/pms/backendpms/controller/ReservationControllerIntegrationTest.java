package com.example.pms.backendpms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pms.backendpms.model.CrmStage;
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
import com.example.pms.backendpms.repository.ReservationRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private RoomRepository roomRepository;
  @Autowired private RoomTypeRepository roomTypeRepository;
  @Autowired private PropertyRepository propertyRepository;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Property propertyOne;
  private Property propertyTwo;
  private RoomType roomTypeOne;
  private Room roomOne;
  private UserAccount ownerOne;
  private UserAccount ownerTwo;
  private String ownerOneToken;

  @BeforeEach
  void setUp() throws Exception {
    String suffix = String.valueOf(System.nanoTime());
    Organization organization = new Organization();
    organization.setName("Reservation Integration Org " + suffix);
    organization.setActive(true);
    organization = organizationRepository.save(organization);

    ownerOne = buildOwner(organization, "Owner One", "owner.one+" + suffix + "@example.com", "92" + suffix.substring(Math.max(0, suffix.length() - 8)));
    ownerTwo = buildOwner(organization, "Owner Two", "owner.two+" + suffix + "@example.com", "93" + suffix.substring(Math.max(0, suffix.length() - 8)));
    ownerOne = userAccountRepository.save(ownerOne);
    ownerTwo = userAccountRepository.save(ownerTwo);

    propertyOne = buildProperty(organization, ownerOne, "Property One " + suffix, "P1" + suffix.substring(Math.max(0, suffix.length() - 6)));
    propertyTwo = buildProperty(organization, ownerTwo, "Property Two " + suffix, "P2" + suffix.substring(Math.max(0, suffix.length() - 6)));
    propertyOne = propertyRepository.save(propertyOne);
    propertyTwo = propertyRepository.save(propertyTwo);

    ownerOne.setProperty(propertyOne);
    ownerTwo.setProperty(propertyTwo);
    userAccountRepository.save(ownerOne);
    userAccountRepository.save(ownerTwo);

    roomTypeOne = new RoomType();
    roomTypeOne.setProperty(propertyOne);
    roomTypeOne.setName("Deluxe");
    roomTypeOne.setCode("DLX");
    roomTypeOne.setBaseRate(2500D);
    roomTypeOne.setBaseOccupancy(2);
    roomTypeOne.setMaxOccupancy(3);
    roomTypeOne.setActive(true);
    roomTypeOne = roomTypeRepository.save(roomTypeOne);

    roomOne = new Room();
    roomOne.setProperty(propertyOne);
    roomOne.setRoomType(roomTypeOne);
    roomOne.setRoomNumber("101");
    roomOne.setStatus(RoomStatus.AVAILABLE);
    roomOne.setHousekeepingStatus(HousekeepingStatus.CLEAN);
    roomOne.setActive(true);
    roomOne = roomRepository.save(roomOne);

    ownerOneToken = loginAndExtractToken(ownerOne.getEmail(), "owner12345");
  }

  @Test
  void createReservationPersistsBookingForAuthorizedOwner() throws Exception {
    long beforeCount = reservationRepository.count();

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations", propertyOne.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createReservationPayload(roomTypeOne.getId(), roomOne.getId(), ownerOne.getPhone())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.guestName").value("Guest One"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.roomNumbers[0]").value("101"));

    assertThat(reservationRepository.count()).isEqualTo(beforeCount + 1);
  }

  @Test
  void createReservationRejectsAccessToAnotherOwnersProperty() throws Exception {
    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations", propertyTwo.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createReservationPayload(roomTypeOne.getId(), roomOne.getId(), ownerOne.getPhone())))
        .andExpect(status().isForbidden());
  }

  @Test
  void createReservationRejectsOverlappingRoomAssignment() throws Exception {
    String payload = createReservationPayload(roomTypeOne.getId(), roomOne.getId(), ownerOne.getPhone());

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations", propertyOne.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations", propertyOne.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.replace("Guest One", "Guest Two").replace("9000000100", "9000000101")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Selected room is already occupied for the stay range."));
  }

  @Test
  void checkOutRejectsGuestReservationWithOutstandingBalance() throws Exception {
    Long reservationId = createReservationAndReturnId();

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations/{reservationId}/check-in", propertyOne.getId(), reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "actedByPhone": "%s",
                      "notes": "Guest arrived"
                    }
                    """.formatted(ownerOne.getPhone())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_IN"));

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations/{reservationId}/check-out", propertyOne.getId(), reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "actedByPhone": "%s",
                      "notes": "Attempting checkout"
                    }
                    """.formatted(ownerOne.getPhone())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Outstanding balance must be cleared before checkout unless the booking is billed to a company."));
  }

  @Test
  void paymentThenCheckOutCompletesStayAndMarksRoomDirty() throws Exception {
    Long reservationId = createReservationAndReturnId();

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations/{reservationId}/check-in", propertyOne.getId(), reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "actedByPhone": "%s",
                      "notes": "Guest arrived"
                    }
                    """.formatted(ownerOne.getPhone())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_IN"));

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations/{reservationId}/payments", propertyOne.getId(), reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": 5500,
                      "paymentMethod": "CASH",
                      "referenceNumber": "PAY-001",
                      "notes": "Collected full amount",
                      "receivedByPhone": "%s"
                    }
                    """.formatted(ownerOne.getPhone())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentStatus").value("PAID"))
        .andExpect(jsonPath("$.balanceAmount").value(0.0));

    mockMvc.perform(
            post("/api/properties/{propertyId}/reservations/{reservationId}/check-out", propertyOne.getId(), reservationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "actedByPhone": "%s",
                      "notes": "Guest departed"
                    }
                    """.formatted(ownerOne.getPhone())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

    Room refreshedRoom = roomRepository.findById(roomOne.getId()).orElseThrow();
    assertThat(refreshedRoom.getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
  }

  private UserAccount buildOwner(Organization organization, String fullName, String email, String phone) {
    UserAccount user = new UserAccount();
    user.setOrganization(organization);
    user.setFullName(fullName);
    user.setEmail(email);
    user.setPhone(phone);
    user.setPassword(passwordEncoder.encode("owner12345"));
    user.setRole(UserRole.HOTEL_OWNER);
    user.setActive(true);
    return user;
  }

  private Property buildProperty(
      Organization organization,
      UserAccount owner,
      String name,
      String code
  ) {
    Property property = new Property();
    property.setOrganization(organization);
    property.setOwner(owner);
    property.setName(name);
    property.setCode(code);
    property.setEmail(code.toLowerCase() + "@example.com");
    property.setPhone("0755000000");
    property.setCity("Bhopal");
    property.setState("MP");
    property.setCountry("India");
    property.setTimezone("Asia/Kolkata");
    property.setCurrencyCode("INR");
    property.setActive(true);
    property.setCrmStage(CrmStage.ACTIVE_CUSTOMER);
    property.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
    property.setAutoRenew(true);
    return property;
  }

  private String loginAndExtractToken(String identifier, String password) throws Exception {
    JsonNode jsonNode = objectMapper.readTree(
        mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "identifier": "%s",
                          "password": "%s"
                        }
                        """.formatted(identifier, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
    );

    return jsonNode.get("accessToken").asText();
  }

  private String createReservationPayload(Long roomTypeId, Long roomId, String bookedByPhone) {
    return """
        {
          "source": "WALK_IN",
          "status": "CONFIRMED",
          "checkInDate": "%s",
          "checkOutDate": "%s",
          "adults": 2,
          "children": 0,
          "mealPlan": "Breakfast",
          "ratePlan": "BAR",
          "specialRequests": "Late arrival",
          "roomAmount": 5000,
          "taxAmount": 250,
          "discountAmount": 0,
          "bookedByPhone": "%s",
          "billToType": "GUEST",
          "paymentMethod": "CASH",
          "complimentary": false,
          "groupBooking": false,
          "guest": {
            "fullName": "Guest One",
            "email": "guest.one@example.com",
            "phone": "9000000100",
            "country": "India",
            "state": "MP",
            "city": "Bhopal",
            "postalCode": "462001",
            "address": "Test Address",
            "idType": "AADHAAR",
            "idNumber": "123412341234",
            "notes": "Integration test"
          },
          "rooms": [
            {
              "roomTypeId": %d,
              "roomId": %d,
              "nightlyRate": 2500
            }
          ]
        }
        """.formatted(
        LocalDate.now().plusDays(2),
        LocalDate.now().plusDays(4),
        bookedByPhone,
        roomTypeId,
        roomId
    );
  }

  private Long createReservationAndReturnId() throws Exception {
    JsonNode jsonNode = objectMapper.readTree(
        mockMvc.perform(
                post("/api/properties/{propertyId}/reservations", propertyOne.getId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(ownerOneToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createReservationPayload(roomTypeOne.getId(), roomOne.getId(), ownerOne.getPhone())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
    );

    return jsonNode.get("reservationId").asLong();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
