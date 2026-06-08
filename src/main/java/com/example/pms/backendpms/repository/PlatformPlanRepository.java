package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.PlatformPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformPlanRepository extends JpaRepository<PlatformPlan, Long> {

  Optional<PlatformPlan> findByCodeIgnoreCase(String code);
}
