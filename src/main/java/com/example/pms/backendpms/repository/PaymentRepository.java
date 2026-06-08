package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Payment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  List<Payment> findByReservationIdOrderByPaymentDateDesc(Long reservationId);

  List<Payment> findByReservationPropertyIdOrderByPaymentDateDesc(Long propertyId);
}
