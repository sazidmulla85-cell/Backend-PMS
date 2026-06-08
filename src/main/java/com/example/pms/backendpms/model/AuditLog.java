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
public class AuditLog extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id")
  private Property property;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "actor_user_id")
  private UserAccount actor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditModule module;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditAction action;

  @Column(nullable = false)
  private String entityType;

  @Column(nullable = false)
  private String entityId;

  @Column(nullable = false, length = 1000)
  private String description;
}
