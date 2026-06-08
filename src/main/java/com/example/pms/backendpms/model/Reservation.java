package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Reservation extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "primary_guest_id", nullable = false)
  private Guest primaryGuest;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "booked_by_user_id")
  private UserAccount bookedBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(nullable = false, unique = true)
  private String reservationNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReservationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingSource source;

  @Enumerated(EnumType.STRING)
  private BillToType billToType;

  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  private LocalDate bookingDate;
  private LocalDate checkInDate;
  private LocalDate checkOutDate;
  private Integer nights;
  private Integer adults;
  private Integer children;
  private Integer roomsCount;
  private Boolean complimentary = false;
  private Boolean groupBooking = false;
  private String groupCode;
  private String mealPlan;
  private String ratePlan;
  private String specialRequests;
  private BigDecimal roomAmount;
  private BigDecimal taxAmount;
  private BigDecimal discountAmount;
  private BigDecimal totalAmount;
  private BigDecimal balanceAmount;

  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentStatus;
}
