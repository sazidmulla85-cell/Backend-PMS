package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Room extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "room_type_id", nullable = false)
  private RoomType roomType;

  @Column(nullable = false)
  private String roomNumber;

  private String floorName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoomStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private HousekeepingStatus housekeepingStatus;

  private boolean active = true;
}
