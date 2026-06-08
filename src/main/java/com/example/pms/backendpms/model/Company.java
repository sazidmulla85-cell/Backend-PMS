package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Company extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @Column(nullable = false)
  private String name;

  private String gstVat;
  private String contactPerson;
  private String phone;
  private String email;
  private String city;
  private String billingAddress;
  private Boolean active = true;
}
