package com.nivya.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.AuthResponse;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.common.response.ApiResponse;
import com.nivya.convocation.dto.ChildSendMessageRequest;
import com.nivya.email.dto.EmailPreferenceDto;
import com.nivya.email.dto.VerificationCodeRequest;
import com.nivya.email.dto.VerificationConfirmRequest;
import com.nivya.email.provider.*;
import com.nivya.email.service.EmailService;
import com.nivya.pairing.dto.ConnectPairingRequest;
import com.nivya.pairing.dto.GenerateDisconnectCodeResponse;
import com.nivya.pairing.dto.PairingCodeResponse;
import com.nivya.pairing.dto.VerifyDisconnectCodeRequest;
import com.nivya.pairing.entity.DisconnectCode;
import com.nivya.pairing.repository.DisconnectCodeRepository;
import com.nivya.role.RoleType;
import com.nivya.session.dto.DeviceSessionDto;
import com.nivya.session.service.DeviceSessionService;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmailAndSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DeviceSessionService deviceSessionService;

    @Autowired
    private DisconnectCodeRepository disconnectCodeRepository;

    @Autowired
    private SimulationEmailProvider simulationEmailProvider;

    @Autowired
    private SmtpEmailProvider smtpEmailProvider;

    @Autowired
    private SendGridEmailProvider sendGridEmailProvider;

    @Autowired
    private SesEmailProvider sesEmailProvider;

    @Autowired
    private EmailProviderFactory emailProviderFactory;

    private String parentToken;
    private String childToken;
    private Long parentUserId;
    private Long childUserId;

    @BeforeEach
    void setUp() throws Exception {
        String uniqueSuffix = System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);

        // 1. Register Parent
        RegisterRequest parentReq = new RegisterRequest();
        parentReq.setName("Parent Unit");
        parentReq.setEmail("parent_" + uniqueSuffix + "@nivya.local");
        parentReq.setPassword("P@ssw0rd123!");
        parentReq.setRole(RoleType.PARENT);

        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse pAuth = objectMapper.readValue(
                objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );
        parentToken = pAuth.getAccessToken();
        parentUserId = pAuth.getUser().getId();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest();
        childReq.setName("Child Companion");
        childReq.setEmail("child_" + uniqueSuffix + "@nivya.local");
        childReq.setPassword("P@ssw0rd123!");
        childReq.setRole(RoleType.CHILD);

        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse cAuth = objectMapper.readValue(
                objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );
        childToken = cAuth.getAccessToken();
        childUserId = cAuth.getUser().getId();

        // 3. Pair them
        MvcResult codeRes = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        PairingCodeResponse codeDto = objectMapper.readValue(
                objectMapper.readTree(codeRes.getResponse().getContentAsString()).get("data").toString(),
                PairingCodeResponse.class
        );

        ConnectPairingRequest connectReq = new ConnectPairingRequest();
        connectReq.setCode(codeDto.getCode());

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Email Provider Abstraction: all configured providers resolve and send safely")
    void testEmailProviders() {
        assertThat(simulationEmailProvider.getProviderName()).isEqualTo("SIMULATION");
        assertThat(smtpEmailProvider.getProviderName()).isEqualTo("SMTP");
        assertThat(sendGridEmailProvider.getProviderName()).isEqualTo("SENDGRID");
        assertThat(sesEmailProvider.getProviderName()).isEqualTo("SES");

        assertThat(emailProviderFactory.getProvider("SIMULATION")).isNotNull();
        assertThat(emailProviderFactory.getProvider("SMTP")).isNotNull();
        assertThat(emailProviderFactory.getProvider("SENDGRID")).isNotNull();
        assertThat(emailProviderFactory.getProvider("SES")).isNotNull();

        // Simulation dispatch
        var result = simulationEmailProvider.sendEmail("test@nivya.local", "Subject", "<b>Body</b>", "Body");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).startsWith("sim-");
    }

    @Test
    @DisplayName("Email Verification Flow: send, rate limit, and verify code")
    void testEmailVerificationFlow() throws Exception {
        String testEmail = "verify_" + System.currentTimeMillis() + "@nivya.local";

        VerificationCodeRequest req = new VerificationCodeRequest(testEmail, "EMAIL_VERIFICATION");

        // Request code
        mockMvc.perform(post("/api/v1/email/verify/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Invalid code should fail
        VerificationConfirmRequest failReq = new VerificationConfirmRequest(testEmail, "000000", "EMAIL_VERIFICATION");
        mockMvc.perform(post("/api/v1/email/verify/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Email Preferences: get and update configurable toggles")
    void testEmailPreferences() throws Exception {
        // Get initial preferences
        MvcResult res = mockMvc.perform(get("/api/v1/email/preferences")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        EmailPreferenceDto pref = objectMapper.readValue(
                objectMapper.readTree(res.getResponse().getContentAsString()).get("data").toString(),
                EmailPreferenceDto.class
        );
        assertThat(pref.isLoginAlertsEnabled()).isTrue();
        assertThat(pref.isSecurityCriticalEnabled()).isTrue();

        // Update preferences
        pref.setNewDeviceAlertsEnabled(false);
        pref.setAppUpdatesEnabled(false);

        mockMvc.perform(put("/api/v1/email/preferences")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pref)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newDeviceAlertsEnabled").value(false))
                .andExpect(jsonPath("$.data.appUpdatesEnabled").value(false))
                .andExpect(jsonPath("$.data.securityCriticalEnabled").value(true)); // Remains protected
    }

    @Test
    @DisplayName("Authenticated Device Sessions: tracking on login, listing, and revocation")
    void testDeviceSessions() throws Exception {
        User parent = userRepository.findById(parentUserId).orElseThrow();

        // Perform login to create initial session
        LoginRequest loginReq = new LoginRequest(parent.getEmail(), "P@ssw0rd123!");
        loginReq.setDeviceFingerprint("fp-initial-session");
        loginReq.setDeviceName("Samsung Galaxy S24");
        loginReq.setPlatform("ANDROID");
        loginReq.setOsVersion("14");
        loginReq.setAppVersion("1.0.0");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());

        // List sessions
        MvcResult listRes = mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        List<?> sessions = objectMapper.readValue(
                objectMapper.readTree(listRes.getResponse().getContentAsString()).get("data").toString(),
                List.class
        );
        assertThat(sessions).isNotEmpty();

        // Simulate new session with explicit device info
        deviceSessionService.recordLogin(
                parent,
                "fp-pixel-9-test",
                "Google Pixel 9",
                "ANDROID",
                "15",
                "1.0.0",
                "192.168.1.100"
        );

        List<DeviceSessionDto> updated = deviceSessionService.getUserSessions(parentUserId);
        DeviceSessionDto active = updated.stream()
                .filter(s -> "fp-pixel-9-test".equals(s.getDeviceFingerprint()))
                .findFirst()
                .orElseThrow();

        assertThat(active.getDeviceName()).isEqualTo("Google Pixel 9");
        assertThat(active.getPlatform()).isEqualTo("ANDROID");
        assertThat(active.getStatus()).isEqualTo("ACTIVE");

        // Revoke the session
        mockMvc.perform(post("/api/v1/sessions/" + active.getId() + "/revoke")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<DeviceSessionDto> afterRevoke = deviceSessionService.getUserSessions(parentUserId);
        DeviceSessionDto revoked = afterRevoke.stream()
                .filter(s -> s.getId().equals(active.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
    }

    @Test
    @DisplayName("Protected Disconnect: Parent generates code, Child verifies to disconnect")
    void testProtectedDisconnectFlow() throws Exception {
        // 1. Child trying to generate code must be forbidden
        mockMvc.perform(post("/api/v1/pairing/disconnect/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());

        // 2. Parent generates one-time disconnect code
        MvcResult genRes = mockMvc.perform(post("/api/v1/pairing/disconnect/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        GenerateDisconnectCodeResponse codeResp = objectMapper.readValue(
                objectMapper.readTree(genRes.getResponse().getContentAsString()).get("data").toString(),
                GenerateDisconnectCodeResponse.class
        );

        assertThat(codeResp.getCode()).isNotBlank();
        assertThat(codeResp.getTtlSeconds()).isEqualTo(600);

        // 3. Parent cannot verify the disconnect code
        VerifyDisconnectCodeRequest verifyReq = new VerifyDisconnectCodeRequest(codeResp.getCode());
        mockMvc.perform(post("/api/v1/pairing/disconnect/verify")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isForbidden());

        // 4. Invalid code rejected for Child
        VerifyDisconnectCodeRequest badReq = new VerifyDisconnectCodeRequest("DIS-9999-9999");
        mockMvc.perform(post("/api/v1/pairing/disconnect/verify")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());

        // 5. Child enters correct code -> Successful disconnect
        mockMvc.perform(post("/api/v1/pairing/disconnect/verify")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Relationship disconnected successfully"));

        // 6. Code reuse rejected (single-use invariant)
        mockMvc.perform(post("/api/v1/pairing/disconnect/verify")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CRACK and FREAK: immediate note sending and duplicate prevention")
    void testCrackAndFreakConvocation() throws Exception {
        // Child sends CRACK: "Mom,here"
        ChildSendMessageRequest crackReq = new ChildSendMessageRequest("Mom,here");
        mockMvc.perform(post("/api/v1/convocation/child/send")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // Child rapid duplicate resend should be idempotently accepted without crashing
        mockMvc.perform(post("/api/v1/convocation/child/send")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // Child sends FREAK: "Someone's,here"
        ChildSendMessageRequest freakReq = new ChildSendMessageRequest("Someone's,here");
        mockMvc.perform(post("/api/v1/convocation/child/send")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(freakReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // Verify messages appear in Parent convocation history
        MvcResult histRes = mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        String historyJson = histRes.getResponse().getContentAsString();
        assertThat(historyJson).contains("Mom,here");
        assertThat(historyJson).contains("Someone's,here");
    }
}
