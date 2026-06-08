package com.example.pms.backendpms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pms.backendpms.model.Organization;
import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import com.example.pms.backendpms.repository.OrganizationRepository;
import com.example.pms.backendpms.repository.PasswordResetTokenRepository;
import com.example.pms.backendpms.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "pms.notifications.email.enabled=true",
    "pms.notifications.email.from=test@hotelpms.local",
    "pms.frontend.base-url=http://localhost:4200"
})
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private JavaMailSender javaMailSender;

  private UserAccount user;

  @BeforeEach
  void setUp() {
    String suffix = String.valueOf(System.nanoTime());
    Organization organization = new Organization();
    organization.setName("Integration Org " + suffix);
    organization.setActive(true);
    organization = organizationRepository.save(organization);

    user = new UserAccount();
    user.setOrganization(organization);
    user.setFullName("Integration Owner");
    user.setEmail("integration.owner+" + suffix + "@example.com");
    user.setPhone("91" + suffix.substring(Math.max(0, suffix.length() - 8)));
    user.setPassword(passwordEncoder.encode("owner12345"));
    user.setRole(UserRole.HOTEL_OWNER);
    user.setActive(true);
    user = userAccountRepository.save(user);
  }

  @Test
  void loginReturnsJwtSessionForValidCredentials() throws Exception {
    mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identifier": "%s",
                      "password": "owner12345"
                    }
                    """.formatted(user.getEmail())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.userId").value(user.getId()))
        .andExpect(jsonPath("$.email").value(user.getEmail()));
  }

  @Test
  void forgotPasswordThenResetAllowsLoginWithNewPassword() throws Exception {
    mockMvc.perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s"
                    }
                    """.formatted(user.getEmail())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));

    ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(javaMailSender).send(mailCaptor.capture());
    String emailBody = mailCaptor.getValue().getText();
    assertThat(emailBody).contains("http://localhost:4200/auth/reset-password?token=");

    String token = emailBody.substring(emailBody.indexOf("token=") + 6).trim().split("\\s+")[0];

    mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s",
                      "newPassword": "newOwner12345",
                      "confirmPassword": "newOwner12345"
                    }
                    """.formatted(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password has been reset successfully. Please sign in with your new password."));

    JsonNode loginResponse = objectMapper.readTree(
        mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "identifier": "%s",
                          "password": "newOwner12345"
                        }
                        """.formatted(user.getEmail())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
    );

    assertThat(loginResponse.get("accessToken").asText()).isNotBlank();
    assertThat(passwordResetTokenRepository.findAllByUserAccountIdAndUsedAtIsNull(user.getId())).isEmpty();
  }
}
