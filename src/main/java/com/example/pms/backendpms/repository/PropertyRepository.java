package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Property;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

  Optional<Property> findByCode(String code);

  List<Property> findAllByOrderByNameAsc();

  long countByActiveTrue();
}
