package com.example.pms.backendpms.security;

import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  public AppUserDetailsService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userAccountRepository.findByPhone(username)
        .or(() -> userAccountRepository.findByEmailIgnoreCase(username))
        .map(AppUserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found for " + username));
  }

  public AppUserPrincipal loadUserById(Long userId) {
    return userAccountRepository.findById(userId)
        .map(AppUserPrincipal::new)
        .orElseThrow(() -> new NotFoundException("User account not found for id " + userId));
  }
}
