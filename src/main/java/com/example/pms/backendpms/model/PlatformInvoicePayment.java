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
public class PlatformInvoicePayment extends AbstractEntity {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "invoice_id", nullable = false)
  private PlatformInvoice invoice;

  @Column(nullable = false)
  private Double amount;

  @Column(nullable = false)
  private String paymentMethod;

  private String referenceNumber;

  @Column(nullable = false)
  private LocalDateTime receivedAt;
}
