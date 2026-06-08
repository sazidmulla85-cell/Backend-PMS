package com.example.pms.backendpms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class GuestDocument extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "reservation_id", nullable = false)
  private Reservation reservation;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "guest_id", nullable = false)
  private Guest guest;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "uploaded_by_user_id")
  private UserAccount uploadedBy;

  @Column(nullable = false)
  private String documentType;

  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false)
  private String storedFileName;

  @Column(nullable = false)
  private String contentType;

  private Long fileSize;

  @Column(nullable = false, length = 1000)
  private String storagePath;

  private LocalDateTime uploadedAt;
}
