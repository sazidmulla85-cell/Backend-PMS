package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.StayViewDtos.StayViewResponse;
import com.example.pms.backendpms.service.StayViewService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/stay-view")
public class StayViewController {

  private final StayViewService stayViewService;

  public StayViewController(StayViewService stayViewService) {
    this.stayViewService = stayViewService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public StayViewResponse stayView(
      @PathVariable Long propertyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate focusDate,
      @RequestParam(defaultValue = "7") int days
  ) {
    return stayViewService.getStayView(propertyId, focusDate, days);
  }
}
