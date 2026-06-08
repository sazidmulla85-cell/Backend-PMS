package com.example.pms.backendpms.service;

import com.example.pms.backendpms.dto.AuthDtos.ForgotPasswordRequest;
import com.example.pms.backendpms.dto.AuthDtos.MessageResponse;
import com.example.pms.backendpms.dto.AuthDtos.ResetPasswordRequest;
import com.example.pms.backendpms.exception.NotFoundException;
import com.example.pms.backendpms.model.AuditAction;
import com.example.pms.backendpms.model.AuditModule;
import com.example.pms.backendpms.model.PasswordResetToken;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.repository.PasswordResetTokenRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String GENERIC_RESET_REQUEST_MESSAGE =
      "If an account with that email exists, a password reset link has been sent.";
  private static final String RESET_SUCCESS_MESSAGE =
      "Password has been reset successfully. Please sign in with your new password.";

  private final UserAccountRepository userAccountRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogService auditLogService;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;
  private final boolean emailEnabled;
  private final String fromAddress;
  private final String frontendBaseUrl;
  private final long resetTokenExpiryMinutes;

  public PasswordResetService(
      UserAccountRepository userAccountRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordEncoder passwordEncoder,
      AuditLogService auditLogService,
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${pms.notifications.email.enabled:false}") boolean emailEnabled,
      @Value("${pms.notifications.email.from:no-reply@hotelpms.local}") String fromAddress,
      @Value("${pms.frontend.base-url:}") String frontendBaseUrl,
      @Value("${pms.security.password-reset.expiration-minutes:30}") long resetTokenExpiryMinutes
  ) {
    this.userAccountRepository = userAccountRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.auditLogService = auditLogService;
    this.mailSenderProvider = mailSenderProvider;
    this.emailEnabled = emailEnabled;
    this.fromAddress = fromAddress;
    this.frontendBaseUrl = frontendBaseUrl;
    this.resetTokenExpiryMinutes = resetTokenExpiryMinutes;
  }

  @Transactional
  public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
    userAccountRepository.findByEmailIgnoreCase(request.email().trim()).ifPresent(this::issueResetToken);
    return new MessageResponse(GENERIC_RESET_REQUEST_MESSAGE);
  }

  @Transactional
  public MessageResponse resetPassword(ResetPasswordRequest request) {
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new IllegalArgumentException("New password and confirm password must match.");
    }

    PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hashToken(request.token()))
        .orElseThrow(() -> new NotFoundException("Password reset token is invalid or has already been used."));

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new IllegalStateException("Password reset token has expired.");
    }

    UserAccount user = resetToken.getUserAccount();
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(user);

    LocalDateTime usedAt = LocalDateTime.now();
    passwordResetTokenRepository.findAllByUserAccountIdAndUsedAtIsNull(user.getId()).forEach(token -> {
      token.setUsedAt(usedAt);
      passwordResetTokenRepository.save(token);
    });

    auditLogService.log(
        user,
        user.getProperty(),
        AuditModule.AUTH,
        AuditAction.UPDATE,
        "UserAccount",
        String.valueOf(user.getId()),
        "Password reset completed for " + user.getEmail()
    );

    return new MessageResponse(RESET_SUCCESS_MESSAGE);
  }

  private void issueResetToken(UserAccount user) {
    if (user.getEmail() == null || user.getEmail().isBlank() || !user.isActive()) {
      return;
    }

    LocalDateTime invalidatedAt = LocalDateTime.now();
    passwordResetTokenRepository.findAllByUserAccountIdAndUsedAtIsNull(user.getId()).forEach(token -> {
      token.setUsedAt(invalidatedAt);
      passwordResetTokenRepository.save(token);
    });

    String rawToken = generateRawToken();
    PasswordResetToken resetToken = new PasswordResetToken();
    resetToken.setUserAccount(user);
    resetToken.setTokenHash(hashToken(rawToken));
    resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes));
    passwordResetTokenRepository.save(resetToken);

    sendPasswordResetEmail(user, rawToken);

    auditLogService.log(
        user,
        user.getProperty(),
        AuditModule.AUTH,
        AuditAction.UPDATE,
        "PasswordResetToken",
        String.valueOf(resetToken.getId()),
        "Password reset requested for " + user.getEmail()
    );
  }

  private void sendPasswordResetEmail(UserAccount user, String rawToken) {
    String resetUrl = buildResetUrl(rawToken);
    if (!emailEnabled) {
      log.info("Password reset email disabled. Generated reset link for {}: {}", user.getEmail(), resetUrl);
      return;
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.warn("Password reset email is enabled but no JavaMailSender is configured. Reset link for {}: {}", user.getEmail(), resetUrl);
      return;
    }

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(user.getEmail());
      message.setSubject("Reset Your Hotel PMS Password");
      message.setText("""
          Hello %s,

          We received a request to reset your Hotel PMS password.

          Use the link below to choose a new password:
          %s

          This link will expire in %d minutes.

          If you did not request this, you can ignore this email.

          Regards,
          Hotel PMS
          """.formatted(user.getFullName(), resetUrl, resetTokenExpiryMinutes));
      mailSender.send(message);
    } catch (Exception exception) {
      log.warn("Failed to send password reset email for {}: {}", user.getEmail(), exception.getMessage());
    }
  }

  private String buildResetUrl(String rawToken) {
    String baseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl + "/auth/reset-password?token="
        + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
  }

  private String generateRawToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(bytes.length * 2);
      for (byte currentByte : bytes) {
        builder.append(String.format("%02x", currentByte));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash password reset token.", exception);
    }
  }
}
