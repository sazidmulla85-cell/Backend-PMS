package com.example.pms.backendpms.dto;

public final class PropertyDtos {

  private PropertyDtos() {}

  public record PropertySummaryResponse(
      Long propertyId,
      String name,
      String code,
      String city,
      String state,
      String country,
      Integer subscribedRoomCount
  ) {}

  public record PropertyDetailsResponse(
      Long propertyId,
      String name,
      String code,
      String city,
      String state,
      String country,
      String timezone,
      String currencyCode,
      String ownerName,
      String ownerPhone,
      Long totalRooms,
      int roomTypeCount,
      Integer subscribedRoomCount
  ) {}
}
