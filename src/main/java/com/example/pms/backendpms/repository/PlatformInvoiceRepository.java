package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.PlatformInvoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformInvoiceRepository extends JpaRepository<PlatformInvoice, Long> {

  List<PlatformInvoice> findByPropertyIdOrderByBillingMonthDesc(Long propertyId);

  List<PlatformInvoice> findAllByOrderByBillingMonthDesc();
}
