package com.example.pms.backendpms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

  @Value("${pms.security.jwt.secret}")
  private String jwtSecret;

  @Value("${pms.seed.enabled:false}")
  private boolean seedEnabled;

  @Value("${spring.datasource.username}")
  private String databaseUsername;

  @Value("${spring.datasource.password}")
  private String databasePassword;

  @Value("${pms.notifications.email.enabled:false}")
  private boolean emailEnabled;

  @Value("${pms.notifications.email.from:no-reply@hotelpms.local}")
  private String emailFrom;

  @Value("${spring.mail.username:}")
  private String emailUsername;

  @Value("${spring.mail.password:}")
  private String emailPassword;

  @Value("${pms.frontend.base-url}")
  private String frontendBaseUrl;

  @Value("${pms.cors.allowed-origins}")
  private String corsAllowedOrigins;

  @PostConstruct
  void validate() {
    requireNonBlank(jwtSecret, "PMS_JWT_SECRET must be configured in production.");
    requireNonBlank(databaseUsername, "PMS_DB_USERNAME must be configured in production.");
    requireNonBlank(databasePassword, "PMS_DB_PASSWORD must be configured in production.");
    requireNonBlank(frontendBaseUrl, "PMS_FRONTEND_BASE_URL must be configured in production.");
    requireNonBlank(corsAllowedOrigins, "PMS_CORS_ALLOWED_ORIGINS must be configured in production.");
    requireStrongJwtSecret();
    requireStrongDatabasePassword();
    requireNonRootDatabaseUser();
    requireRealPublicUrls();

    if (seedEnabled) {
      throw new IllegalStateException("Production startup blocked: pms.seed.enabled must remain false.");
    }

    if (emailEnabled) {
      requireNonBlank(emailFrom, "PMS_GMAIL_FROM must be configured when email notifications are enabled.");
      requireNonBlank(emailUsername, "PMS_GMAIL_USERNAME must be configured when email notifications are enabled.");
      requireNonBlank(emailPassword, "PMS_GMAIL_APP_PASSWORD must be configured when email notifications are enabled.");
    }
  }

  private void requireNonBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(message);
    }
  }

  private void requireStrongJwtSecret() {
    if (jwtSecret.length() < 32 || containsPlaceholder(jwtSecret)) {
      throw new IllegalStateException("Production startup blocked: PMS_JWT_SECRET must be a strong non-placeholder value.");
    }
  }

  private void requireStrongDatabasePassword() {
    if (databasePassword.length() < 16 || containsPlaceholder(databasePassword)) {
      throw new IllegalStateException("Production startup blocked: PMS_DB_PASSWORD must be a strong non-placeholder value.");
    }
  }

  private void requireNonRootDatabaseUser() {
    if ("root".equalsIgnoreCase(databaseUsername)) {
      throw new IllegalStateException("Production startup blocked: use a dedicated non-root MySQL user for PMS.");
    }
  }

  private void requireRealPublicUrls() {
    if (containsInvalidPublicUrl(frontendBaseUrl)) {
      throw new IllegalStateException("Production startup blocked: PMS_FRONTEND_BASE_URL must point to the real public frontend URL.");
    }

    if (containsInvalidPublicUrl(corsAllowedOrigins)) {
      throw new IllegalStateException("Production startup blocked: PMS_CORS_ALLOWED_ORIGINS must list the real public frontend origin(s).");
    }
  }

  private boolean containsInvalidPublicUrl(String value) {
    return value != null && (
        value.contains("localhost")
            || value.contains("127.0.0.1")
            || value.contains("example.com")
    );
  }

  private boolean containsPlaceholder(String value) {
    String normalized = value.toLowerCase();
    return normalized.contains("change-this")
        || normalized.contains("dev-only")
        || normalized.contains("replace-with");
  }
}
