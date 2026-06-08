package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.PropertyCommunicationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyCommunicationLogRepository extends JpaRepository<PropertyCommunicationLog, Long> {

  List<PropertyCommunicationLog> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

  List<PropertyCommunicationLog> findAllByOrderByCreatedAtDesc();
}
