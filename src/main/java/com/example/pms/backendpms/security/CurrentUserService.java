package com.example.pms.backendpms.security;

import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final UserAccountRepository userAccountRepository;

  public CurrentUserService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  public UserAccount getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      throw new NotFoundException("Authenticated user not found.");
    }

    return userAccountRepository.findById(principal.getUserId())
        .orElseThrow(() -> new NotFoundException("Authenticated user not found."));
  }

  public boolean isSuperAdmin() {
    return getCurrentUser().getRole() == UserRole.SUPER_ADMIN;
  }
}
