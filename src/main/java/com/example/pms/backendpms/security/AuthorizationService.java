package com.example.pms.backendpms.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("authorizationService")
public class AuthorizationService {

  public boolean canAccessProperty(Authentication authentication, Long propertyId) {
    if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      return false;
    }

    if (!principal.isEnabled()) {
      return false;
    }

    if (principal.getRole().name().equals("SUPER_ADMIN")) {
      return true;
    }

    return principal.getPropertyId() != null && principal.getPropertyId().equals(propertyId);
  }
}
