package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  List<Company> findByPropertyIdOrderByNameAsc(Long propertyId);

  Optional<Company> findByIdAndPropertyId(Long id, Long propertyId);

  long countByPropertyId(Long propertyId);
}
