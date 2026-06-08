package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.AuthDtos.AccessibleProperty;
import com.example.pms.backendpms.dto.AuthDtos.ForgotPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.LoginRequest;
import com.example.pms.backendpms.dto.AuthDtos.MessageResponse;
import com.example.pms.backendpms.dto.AuthDtos.ResetPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.SessionResponse;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.Property;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.repository.PropertyRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import com.example.pms.backendpms.security.AppUserPrincipal;
import com.example.pms.backendpms.security.JwtService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PropertyRepository propertyRepository;
  private final AuditLogService auditLogService;
  private final SubscriptionLifecycleService subscriptionLifecycleService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final PasswordResetService passwordResetService;

  public AuthService(
      UserAccountRepository userAccountRepository,
      PropertyRepository propertyRepository,
      AuditLogService auditLogService,
      SubscriptionLifecycleService subscriptionLifecycleService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      PasswordResetService passwordResetService
  ) {
    this.userAccountRepository = userAccountRepository;
    this.propertyRepository = propertyRepository;
    this.auditLogService = auditLogService;
    this.subscriptionLifecycleService = subscriptionLifecycleService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.passwordResetService = passwordResetService;
  }

  public SessionResponse login(LoginRequest request) {
    UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.identifier())
        .or(() -> userAccountRepository.findByPhone(request.identifier()))
        .orElseThrow(() -> new NotFoundException("User account not found for the provided identifier."));

    if (!user.isActive()) {
      throw new IllegalArgumentException("User account is inactive.");
    }

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Incorrect password.");
    }

    user.setLastLoginAt(LocalDateTime.now());
    userAccountRepository.save(user);

    List<AccessibleProperty> properties = new ArrayList<>();

    if (user.getProperty() != null) {
      Property property = user.getProperty();
      if (subscriptionLifecycleService.refreshStatus(property)) {
        propertyRepository.save(property);
      }
      properties.add(
          new AccessibleProperty(
              property.getId(),
              property.getName(),
              property.getCode(),
              property.isActive(),
              property.getSubscriptionStatus(),
              property.getRenewalDate()
          )
      );
      auditLogService.log(
          user,
          property,
          AuditModule.AUTH,
          AuditAction.LOGIN,
          "UserAccount",
          String.valueOf(user.getId()),
          user.getFullName() + " logged into " + property.getName()
      );
    } else {
      propertyRepository.findAllByOrderByNameAsc().forEach(property -> {
        if (subscriptionLifecycleService.refreshStatus(property)) {
          propertyRepository.save(property);
        }
        properties.add(
            new AccessibleProperty(
                property.getId(),
                property.getName(),
                property.getCode(),
                property.isActive(),
                property.getSubscriptionStatus(),
                property.getRenewalDate()
            )
        );
      });

      auditLogService.log(
          user,
          null,
          AuditModule.AUTH,
          AuditAction.LOGIN,
          "UserAccount",
          String.valueOf(user.getId()),
          user.getFullName() + " logged into the platform console"
      );
    }

    AppUserPrincipal principal = new AppUserPrincipal(user);
    String accessToken = jwtService.generateToken(principal);

    return new SessionResponse(
        user.getId(),
        user.getFullName(),
        user.getRole().name(),
        user.getEmail(),
        user.getPhone(),
        user.getOrganization().getId(),
        properties,
        accessToken,
        jwtService.extractExpiration(accessToken)
    );
  }

  public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
    return passwordResetService.requestPasswordReset(request);
  }

  public MessageResponse resetPassword(ResetPasswordRequest request) {
    return passwordResetService.resetPassword(request);
  }
}
