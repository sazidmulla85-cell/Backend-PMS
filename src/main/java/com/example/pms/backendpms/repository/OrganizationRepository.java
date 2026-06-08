package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByNameIgnoreCase(String name);
}
