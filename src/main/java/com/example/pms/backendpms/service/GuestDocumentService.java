package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.ReservationDtos.GuestDocumentResponse;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.GuestDocument;
import com.example.pms.backendpms.model.Reservation;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.repository.GuestDocumentRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GuestDocumentService {

  private final ReservationService reservationService;
  private final GuestDocumentRepository guestDocumentRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogService auditLogService;
  private final Path uploadDirectory;

  public GuestDocumentService(
      ReservationService reservationService,
      GuestDocumentRepository guestDocumentRepository,
      UserAccountRepository userAccountRepository,
      AuditLogService auditLogService,
      @Value("${pms.storage.upload-dir:./uploads}") String uploadDirectory
  ) {
    this.reservationService = reservationService;
    this.guestDocumentRepository = guestDocumentRepository;
    this.userAccountRepository = userAccountRepository;
    this.auditLogService = auditLogService;
    this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
  }

  public List<GuestDocumentResponse> getDocuments(Long propertyId, Long reservationId) {
    Reservation reservation = reservationService.getReservationEntity(propertyId, reservationId);
    return guestDocumentRepository.findByReservation_IdOrderByUploadedAtDesc(reservation.getId()).stream()
        .map(document -> toResponse(propertyId, reservationId, document))
        .toList();
  }

  @Transactional
  public GuestDocumentResponse uploadDocument(
      Long propertyId,
      Long reservationId,
      String documentType,
      String uploadedByPhone,
      MultipartFile file
  ) {
    Reservation reservation = reservationService.getReservationEntity(propertyId, reservationId);

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Select a document file to upload.");
    }

    String normalizedDocumentType = documentType != null && !documentType.isBlank()
        ? documentType.trim().toUpperCase(Locale.ROOT)
        : "ID_PROOF";

    try {
      Path reservationDirectory = uploadDirectory
          .resolve("property-" + propertyId)
          .resolve("reservation-" + reservationId);
      Files.createDirectories(reservationDirectory);

      String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
      String storedFileName = UUID.randomUUID() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
      Path destination = reservationDirectory.resolve(storedFileName);
      Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

      GuestDocument document = new GuestDocument();
      document.setProperty(reservation.getProperty());
      document.setReservation(reservation);
      document.setGuest(reservation.getPrimaryGuest());
      document.setUploadedBy(resolveActor(uploadedByPhone));
      document.setDocumentType(normalizedDocumentType);
      document.setFileName(originalFileName);
      document.setStoredFileName(storedFileName);
      document.setContentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
      document.setFileSize(file.getSize());
      document.setStoragePath(destination.toString());
      document.setUploadedAt(LocalDateTime.now());
      document = guestDocumentRepository.save(document);

      auditLogService.log(
          document.getUploadedBy(),
          reservation.getProperty(),
          AuditModule.RESERVATIONS,
          AuditAction.UPDATE,
          "GuestDocument",
          String.valueOf(document.getId()),
          "Uploaded " + document.getDocumentType() + " for reservation " + reservation.getReservationNumber()
      );

      return toResponse(propertyId, reservationId, document);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to store the uploaded document.", exception);
    }
  }

  public Resource loadDocumentResource(Long propertyId, Long reservationId, Long documentId) {
    GuestDocument document = guestDocumentRepository.findByIdAndReservation_IdAndProperty_Id(documentId, reservationId, propertyId)
        .orElseThrow(() -> new NotFoundException("Document not found for id " + documentId));
    Resource resource = new FileSystemResource(document.getStoragePath());
    if (!resource.exists() || !resource.isReadable()) {
      throw new NotFoundException("Document file is not available for id " + documentId);
    }
    return resource;
  }

  public GuestDocumentResponse getDocument(Long propertyId, Long reservationId, Long documentId) {
    GuestDocument document = guestDocumentRepository.findByIdAndReservation_IdAndProperty_Id(documentId, reservationId, propertyId)
        .orElseThrow(() -> new NotFoundException("Document not found for id " + documentId));
    return toResponse(propertyId, reservationId, document);
  }

  private UserAccount resolveActor(String uploadedByPhone) {
    if (uploadedByPhone == null || uploadedByPhone.isBlank()) {
      return null;
    }

    return userAccountRepository.findByPhone(uploadedByPhone)
        .orElse(null);
  }

  private GuestDocumentResponse toResponse(Long propertyId, Long reservationId, GuestDocument document) {
    String encodedFileName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8);
    return new GuestDocumentResponse(
        document.getId(),
        document.getGuest().getId(),
        document.getGuest().getFullName(),
        document.getDocumentType(),
        document.getFileName(),
        document.getContentType(),
        document.getFileSize(),
        document.getUploadedAt(),
        "/api/properties/" + propertyId
            + "/reservations/" + reservationId
            + "/documents/" + document.getId()
            + "/content?filename=" + encodedFileName
    );
  }
}
