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
public class PlatformInvoice extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @Column(nullable = false, unique = true)
  private String invoiceNumber;

  @Column(nullable = false)
  private String planName;

  @Column(nullable = false)
  private LocalDate billingMonth;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false)
  private Double totalAmount;

  @Column(nullable = false)
  private Double paidAmount = 0D;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlatformInvoiceStatus status = PlatformInvoiceStatus.PENDING;

  @Column(length = 1000)
  private String notes;
}
