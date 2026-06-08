package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Property extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private UserAccount owner;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  private String email;
  private String phone;
  private String city;
  private String state;
  private String country;
  private String timezone;
  private String currencyCode;
  private Integer subscribedRoomCount;
  private String subscriptionPlan;
  private String moduleEntitlementsCsv;
  private String accountManager;
  @Enumerated(EnumType.STRING)
  private CrmStage crmStage = CrmStage.ACTIVE_CUSTOMER;
  @Column(length = 2000)
  private String supportNotes;
  @Column(length = 2000)
  private String commercialNotes;

  @Enumerated(EnumType.STRING)
  private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

  private LocalDate subscriptionStartDate;
  private LocalDate renewalDate;
  private Double monthlySubscriptionAmount;
  private Boolean autoRenew = true;
  private boolean active = true;

  public boolean isAutoRenew() {
    return Boolean.TRUE.equals(autoRenew);
  }
}
