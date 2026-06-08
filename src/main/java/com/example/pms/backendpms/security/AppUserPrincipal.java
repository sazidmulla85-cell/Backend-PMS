package com.example.pms.backendpms.security;

import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserPrincipal implements UserDetails {

  private final Long userId;
  private final Long propertyId;
  private final String username;
  private final String password;
  private final boolean active;
  private final UserRole role;

  public AppUserPrincipal(UserAccount user) {
    this.userId = user.getId();
    this.propertyId = user.getProperty() != null ? user.getProperty().getId() : null;
    this.username = user.getPhone();
    this.password = user.getPassword();
    this.active = user.isActive();
    this.role = user.getRole();
  }

  public Long getUserId() {
    return userId;
  }

  public Long getPropertyId() {
    return propertyId;
  }

  public UserRole getRole() {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
