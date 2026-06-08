package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.ReservationRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {

  List<ReservationRoom> findByReservationPropertyIdOrderByAssignedFromAsc(Long propertyId);

  List<ReservationRoom> findByReservationId(Long reservationId);
}
