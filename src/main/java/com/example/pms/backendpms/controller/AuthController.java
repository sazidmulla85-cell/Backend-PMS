package com.example.pms.backendpms.controller;

import com.example.pms.backendpms.dto.AuthDtos.ForgotPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.LoginRequest;
import com.example.pms.backendpms.dto.AuthDtos.MessageResponse;
import com.example.pms.backendpms.dto.AuthDtos.ResetPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.SessionResponse;
import com.example.pms.backendpms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public SessionResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/forgot-password")
  public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return authService.requestPasswordReset(request);
  }

  @PostMapping("/reset-password")
  public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    return authService.resetPassword(request);
  }
}
