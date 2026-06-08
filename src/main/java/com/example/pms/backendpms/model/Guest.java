package com.example.pms.backendpms.model;

import jakarta.persistence.Entity;
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
public class Guest extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  private String fullName;
  private String email;
  private String phone;
  private LocalDate dateOfBirth;
  private String gender;
  private String country;
  private String state;
  private String city;
  private String postalCode;
  private String address;
  private String idType;
  private String idNumber;
  private boolean vip;
  private String notes;
}
