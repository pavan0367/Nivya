package com.nivya.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.email.repository.EmailVerificationCodeRepository;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EmailVerificationGateConfigurationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @TestPropertySource(properties = "nivya.email.verification-required=false")
    @DisplayName("Verification Disabled Mode (EMAIL_VERIFICATION_REQUIRED=false)")
    class VerificationDisabledMode {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private EmailVerificationCodeRepository codeRepository;

        @Test
        @DisplayName("Registration creates ACTIVE user and allows immediate login without OTP")
        void testRegistrationAndImmediateLoginWithoutOtp() throws Exception {
            String email = "dev.bypass." + System.currentTimeMillis() + "@nivya.local";
            String password = "DevPassword123!";

            RegisterRequest regReq = new RegisterRequest("Dev User", email, password, RoleType.PARENT);

            // 1. Registration succeeds and sets status=ACTIVE
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));

            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // 2. OTP generation is still preserved in repository
            assertThat(codeRepository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, "EMAIL_VERIFICATION"))
                    .isPresent();

            // 3. Login succeeds immediately without OTP
            LoginRequest loginReq = new LoginRequest(email, password);
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @TestPropertySource(properties = "nivya.email.verification-required=true")
    @DisplayName("Verification Enabled Mode (EMAIL_VERIFICATION_REQUIRED=true)")
    class VerificationEnabledMode {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Test
        @DisplayName("Registration creates PENDING user and blocks login until activated")
        void testRegistrationAndBlockedLoginWhenEnforced() throws Exception {
            String email = "prod.enforced." + System.currentTimeMillis() + "@nivya.local";
            String password = "ProdPassword123!";

            RegisterRequest regReq = new RegisterRequest("Prod User", email, password, RoleType.PARENT);

            // 1. Registration creates user with status=PENDING
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.status").value("PENDING"));

            User user = userRepository.findByEmail(email).orElseThrow();
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);

            // 2. Login is strictly blocked with 401 Unauthorized
            LoginRequest loginReq = new LoginRequest(email, password);
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Account email is not verified. Please verify your email before logging in."));

            // 3. Once activated, login succeeds
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
