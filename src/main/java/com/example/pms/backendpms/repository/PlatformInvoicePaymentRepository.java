package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.PlatformInvoicePayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformInvoicePaymentRepository extends JpaRepository<PlatformInvoicePayment, Long> {

  List<PlatformInvoicePayment> findByInvoiceIdOrderByReceivedAtDesc(Long invoiceId);

  List<PlatformInvoicePayment> findAllByOrderByReceivedAtDesc();
}
