package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.PropertyDtos.PropertyDetailsResponse;
import com.example.pms.backendpms.dto.PropertyDtos.PropertySummaryResponse;
import com.example.pms.backendpms.service.PropertyService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

  private final PropertyService propertyService;

  public PropertyController(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<PropertySummaryResponse> properties() {
    return propertyService.getProperties();
  }

  @GetMapping("/{propertyId}")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public PropertyDetailsResponse property(@PathVariable Long propertyId) {
    return propertyService.getProperty(propertyId);
  }
}
