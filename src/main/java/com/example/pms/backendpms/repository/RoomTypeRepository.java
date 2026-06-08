package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.RoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

  List<RoomType> findByPropertyIdOrderByNameAsc(Long propertyId);

  Optional<RoomType> findByIdAndPropertyId(Long id, Long propertyId);

  Optional<RoomType> findByPropertyIdAndNameIgnoreCase(Long propertyId, String name);
}
