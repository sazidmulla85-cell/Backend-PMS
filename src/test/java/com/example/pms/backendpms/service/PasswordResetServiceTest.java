package com.example.pms.backendpms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pms.backendpms.dto.AuthDtos.ForgotPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.MessageResponse;
import com.example.pms.backendpms.dto.AuthDtos.ResetPasswordRequest;
import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.PasswordResetToken;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.PasswordResetTokenRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuditLogService auditLogService;
  @Mock private ObjectProvider<org.springframework.mail.javamail.JavaMailSender> mailSenderProvider;

  private PasswordResetService passwordResetService;

  private UserAccount user;

  @BeforeEach
  void setUp() {
    passwordResetService = new PasswordResetService(
        userAccountRepository,
        passwordResetTokenRepository,
        passwordEncoder,
        auditLogService,
        mailSenderProvider,
        false,
        "no-reply@hotelpms.local",
        "http://localhost:4200",
        30
    );

    Organization organization = new Organization();
    organization.setId(12L);
    organization.setName("Org");

    user = new UserAccount();
    user.setId(42L);
    user.setOrganization(organization);
    user.setFullName("Owner User");
    user.setEmail("owner@example.com");
    user.setPhone("9999999999");
    user.setPassword("old-password-hash");
    user.setRole(UserRole.HOTEL_OWNER);
    user.setActive(true);

    lenient().doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void requestPasswordResetReturnsGenericMessageForUnknownEmail() {
    when(userAccountRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    MessageResponse response = passwordResetService.requestPasswordReset(
        new ForgotPasswordRequest("missing@example.com")
    );

    assertEquals(
        "If an account with that email exists, a password reset link has been sent.",
        response.message()
    );
    verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
  }

  @Test
  void requestPasswordResetInvalidatesOldTokensAndCreatesNewOne() {
    PasswordResetToken existingToken = new PasswordResetToken();
    existingToken.setId(9L);
    existingToken.setUserAccount(user);

    when(userAccountRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
    when(passwordResetTokenRepository.findAllByUserAccountIdAndUsedAtIsNull(user.getId()))
        .thenReturn(List.of(existingToken));

    MessageResponse response = passwordResetService.requestPasswordReset(
        new ForgotPasswordRequest(user.getEmail())
    );

    ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(passwordResetTokenRepository, org.mockito.Mockito.atLeast(2)).save(tokenCaptor.capture());
    PasswordResetToken newToken = tokenCaptor.getAllValues().get(tokenCaptor.getAllValues().size() - 1);

    assertEquals(
        "If an account with that email exists, a password reset link has been sent.",
        response.message()
    );
    assertEquals(user, newToken.getUserAccount());
    assertNotNull(newToken.getTokenHash());
    assertEquals(64, newToken.getTokenHash().length());
    assertNotNull(newToken.getExpiresAt());
    assertNotNull(existingToken.getUsedAt());
  }

  @Test
  void resetPasswordRejectsMismatchedPasswords() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword(
            new ResetPasswordRequest("token", "new-password", "different-password")
        )
    );

    assertEquals("New password and confirm password must match.", exception.getMessage());
  }

  @Test
  void resetPasswordUpdatesPasswordAndMarksOpenTokensUsed() {
    PasswordResetToken activeToken = new PasswordResetToken();
    activeToken.setId(11L);
    activeToken.setUserAccount(user);
    activeToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

    PasswordResetToken secondaryToken = new PasswordResetToken();
    secondaryToken.setId(12L);
    secondaryToken.setUserAccount(user);
    secondaryToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

    when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(any()))
        .thenReturn(Optional.of(activeToken));
    when(passwordResetTokenRepository.findAllByUserAccountIdAndUsedAtIsNull(user.getId()))
        .thenReturn(List.of(activeToken, secondaryToken));
    when(passwordEncoder.encode("new-password-123")).thenReturn("encoded-password");

    MessageResponse response = passwordResetService.resetPassword(
        new ResetPasswordRequest("raw-token", "new-password-123", "new-password-123")
    );

    assertEquals(
        "Password has been reset successfully. Please sign in with your new password.",
        response.message()
    );
    assertEquals("encoded-password", user.getPassword());
    assertNotNull(activeToken.getUsedAt());
    assertNotNull(secondaryToken.getUsedAt());
    verify(userAccountRepository).save(user);
  }
}
