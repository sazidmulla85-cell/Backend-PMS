package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.ReservationDtos.CreateReservationRequest;
import com.example.pms.backendpms.dto.ReservationDtos.AddChargeRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ExchangeRoomRequest;
import com.example.pms.backendpms.dto.ReservationDtos.GuestDocumentResponse;
import com.example.pms.backendpms.dto.ReservationDtos.RecordPaymentRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationActionRequest;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationDetailResponse;
import com.example.pms.backendpms.dto.ReservationDtos.ReservationSummaryResponse;
import com.example.pms.backendpms.service.GuestDocumentService;
import com.example.pms.backendpms.service.ReservationService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/properties/{propertyId}/reservations")
public class ReservationController {

  private final ReservationService reservationService;
  private final GuestDocumentService guestDocumentService;

  public ReservationController(
      ReservationService reservationService,
      GuestDocumentService guestDocumentService
  ) {
    this.reservationService = reservationService;
    this.guestDocumentService = guestDocumentService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public List<ReservationSummaryResponse> reservations(@PathVariable Long propertyId) {
    return reservationService.getReservations(propertyId);
  }

  @GetMapping("/{reservationId}")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse reservation(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId
  ) {
    return reservationService.getReservation(propertyId, reservationId);
  }

  @PostMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationSummaryResponse createReservation(
      @PathVariable Long propertyId,
      @Valid @RequestBody CreateReservationRequest request
  ) {
    return reservationService.createReservation(propertyId, request);
  }

  @PostMapping("/{reservationId}/check-in")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse checkIn(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestBody(required = false) ReservationActionRequest request
  ) {
    return reservationService.checkIn(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/check-out")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse checkOut(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestBody(required = false) ReservationActionRequest request
  ) {
    return reservationService.checkOut(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/undo-check-in")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse undoCheckIn(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestBody(required = false) ReservationActionRequest request
  ) {
    return reservationService.undoCheckIn(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/cancel")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse cancel(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestBody(required = false) ReservationActionRequest request
  ) {
    return reservationService.cancel(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/no-show")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse markNoShow(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestBody(required = false) ReservationActionRequest request
  ) {
    return reservationService.markNoShow(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/payments")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse recordPayment(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @Valid @RequestBody RecordPaymentRequest request
  ) {
    return reservationService.recordPayment(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/charges")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse addCharge(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @Valid @RequestBody AddChargeRequest request
  ) {
    return reservationService.addCharge(propertyId, reservationId, request);
  }

  @PostMapping("/{reservationId}/exchange-room")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ReservationDetailResponse exchangeRoom(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @Valid @RequestBody ExchangeRoomRequest request
  ) {
    return reservationService.exchangeRoom(propertyId, reservationId, request);
  }

  @GetMapping("/{reservationId}/documents")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public List<GuestDocumentResponse> documents(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId
  ) {
    return guestDocumentService.getDocuments(propertyId, reservationId);
  }

  @PostMapping(value = "/{reservationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public GuestDocumentResponse uploadDocument(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @RequestParam String documentType,
      @RequestParam(required = false) String uploadedByPhone,
      @RequestParam MultipartFile file
  ) {
    return guestDocumentService.uploadDocument(
        propertyId,
        reservationId,
        documentType,
        uploadedByPhone,
        file
    );
  }

  @GetMapping("/{reservationId}/documents/{documentId}/content")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public ResponseEntity<Resource> documentContent(
      @PathVariable Long propertyId,
      @PathVariable Long reservationId,
      @PathVariable Long documentId
  ) {
    GuestDocumentResponse document = guestDocumentService.getDocument(propertyId, reservationId, documentId);
    Resource resource = guestDocumentService.loadDocumentResource(propertyId, reservationId, documentId);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(document.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString()
        )
        .body(resource);
  }
}
