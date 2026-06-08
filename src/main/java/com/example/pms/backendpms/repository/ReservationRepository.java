package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Reservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findByPropertyIdOrderByCheckInDateAsc(Long propertyId);

  long countByPropertyId(Long propertyId);

  Optional<Reservation> findByIdAndPropertyId(Long id, Long propertyId);

  List<Reservation> findByPropertyIdAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanOrderByCheckInDateAsc(
      Long propertyId,
      LocalDate rangeEnd,
      LocalDate rangeStart
  );
}
