package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.ReservationCharge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationChargeRepository extends JpaRepository<ReservationCharge, Long> {

  List<ReservationCharge> findByReservationIdOrderByChargeDateDesc(Long reservationId);
}
