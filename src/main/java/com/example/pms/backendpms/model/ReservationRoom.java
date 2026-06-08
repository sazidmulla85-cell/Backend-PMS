package com.example.pms.backendpms.model;

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
public class ReservationRoom extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "reservation_id", nullable = false)
  private Reservation reservation;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "room_id")
  private Room room;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "room_type_id", nullable = false)
  private RoomType roomType;

  @Enumerated(EnumType.STRING)
  private ReservationRoomStatus status;

  private LocalDate assignedFrom;
  private LocalDate assignedTo;
  private BigDecimal nightlyRate;
}
