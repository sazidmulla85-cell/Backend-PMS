package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.GuestDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestDocumentRepository extends JpaRepository<GuestDocument, Long> {

  List<GuestDocument> findByReservation_IdOrderByUploadedAtDesc(Long reservationId);

  Optional<GuestDocument> findByIdAndReservation_IdAndProperty_Id(Long id, Long reservationId, Long propertyId);
}
