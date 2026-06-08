package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Organization extends AbstractEntity {

  @Column(nullable = false, unique = true)
  private String name;

  private String legalName;
  private String gstNumber;
  private String billingEmail;
  private String billingPhone;
  private String billingAddress;

  private boolean active = true;
}
