package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.Guest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

  List<Guest> findByPropertyIdOrderByFullNameAsc(Long propertyId);
}
