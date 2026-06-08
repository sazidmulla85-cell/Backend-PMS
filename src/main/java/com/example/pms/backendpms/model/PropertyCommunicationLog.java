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
public class PropertyCommunicationLog extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommunicationChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CommunicationStatus status;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, length = 2000)
  private String message;

  private String actorName;
}
