package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.PropertyDtos.PropertyDetailsResponse;
import com.example.pms.backendpms.dto.PropertyDtos.PropertySummaryResponse;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.repository.PropertyRepository;
import com.example.pms.backendpms.repository.RoomRepository;
import com.example.pms.backendpms.repository.RoomTypeRepository;
import com.example.pms.backendpms.security.CurrentUserService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final RoomRepository roomRepository;
  private final RoomTypeRepository roomTypeRepository;
  private final CurrentUserService currentUserService;

  public PropertyService(
      PropertyRepository propertyRepository,
      RoomRepository roomRepository,
      RoomTypeRepository roomTypeRepository,
      CurrentUserService currentUserService
  ) {
    this.propertyRepository = propertyRepository;
    this.roomRepository = roomRepository;
    this.roomTypeRepository = roomTypeRepository;
    this.currentUserService = currentUserService;
  }

  public List<PropertySummaryResponse> getProperties() {
    List<Property> properties = currentUserService.isSuperAdmin()
        ? propertyRepository.findAllByOrderByNameAsc()
        : currentUserService.getCurrentUser().getProperty() != null
            ? List.of(currentUserService.getCurrentUser().getProperty())
            : List.of();

    return properties.stream()
        .map(property -> new PropertySummaryResponse(
            property.getId(),
            property.getName(),
            property.getCode(),
            property.getCity(),
            property.getState(),
            property.getCountry(),
            property.getSubscribedRoomCount()
        ))
        .toList();
  }

  public PropertyDetailsResponse getProperty(Long propertyId) {
    Property property = getPropertyEntity(propertyId);

    return new PropertyDetailsResponse(
        property.getId(),
        property.getName(),
        property.getCode(),
        property.getCity(),
        property.getState(),
        property.getCountry(),
        property.getTimezone(),
        property.getCurrencyCode(),
        property.getOwner().getFullName(),
        property.getOwner().getPhone(),
        roomRepository.countByPropertyId(propertyId),
        roomTypeRepository.findByPropertyIdOrderByNameAsc(propertyId).size(),
        property.getSubscribedRoomCount()
    );
  }

  public Property getPropertyEntity(Long propertyId) {
    return propertyRepository.findById(propertyId)
        .orElseThrow(() -> new NotFoundException("Property not found for id " + propertyId));
  }
}
