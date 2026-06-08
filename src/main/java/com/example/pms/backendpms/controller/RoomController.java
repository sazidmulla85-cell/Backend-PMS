package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.RoomDtos.CreateRoomRequest;
import com.example.pms.backendpms.dto.RoomDtos.CreateRoomTypeRequest;
import com.example.pms.backendpms.dto.RoomDtos.RoomResponse;
import com.example.pms.backendpms.dto.RoomDtos.RoomTypeGroupResponse;
import com.example.pms.backendpms.dto.RoomDtos.RoomsViewResponse;
import com.example.pms.backendpms.dto.RoomDtos.UpdateRoomStatusRequest;
import com.example.pms.backendpms.service.RoomService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties/{propertyId}/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public RoomsViewResponse rooms(
      @PathVariable Long propertyId,
      @RequestParam(required = false) LocalDate businessDate
  ) {
    return roomService.getRoomsView(propertyId, businessDate);
  }

  @PostMapping("/room-types")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public RoomTypeGroupResponse createRoomType(
      @PathVariable Long propertyId,
      @Valid @RequestBody CreateRoomTypeRequest request
  ) {
    return roomService.createRoomType(propertyId, request);
  }

  @PostMapping
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public RoomResponse createRoom(
      @PathVariable Long propertyId,
      @Valid @RequestBody CreateRoomRequest request
  ) {
    return roomService.createRoom(propertyId, request);
  }

  @PostMapping("/{roomId}/status")
  @PreAuthorize("@authorizationService.canAccessProperty(authentication, #propertyId)")
  public RoomResponse updateRoomStatus(
      @PathVariable Long propertyId,
      @PathVariable Long roomId,
      @Valid @RequestBody UpdateRoomStatusRequest request
  ) {
    return roomService.updateRoomStatus(propertyId, roomId, request);
  }
}
