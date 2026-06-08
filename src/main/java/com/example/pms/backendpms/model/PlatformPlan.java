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
public class PlatformPlan extends AbstractEntity {

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer includedRooms;

  @Column(nullable = false)
  private Double baseMonthlyAmount;

  @Column(nullable = false)
  private Double perRoomAmount;

  @Column(length = 500)
  private String description;

  @Column(length = 1000)
  private String moduleCodesCsv;

  private boolean active = true;
}
