package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

  List<Room> findByPropertyIdOrderByRoomNumberAsc(Long propertyId);

  Optional<Room> findByIdAndPropertyId(Long id, Long propertyId);

  Optional<Room> findByPropertyIdAndRoomNumber(Long propertyId, String roomNumber);

  long countByPropertyId(Long propertyId);
}
