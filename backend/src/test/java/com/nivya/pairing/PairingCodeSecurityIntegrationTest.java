package com.nivya.pairing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.audit.entity.AuditLog;
import com.nivya.audit.repository.AuditLogRepository;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.pairing.dto.ConnectPairingRequest;
import com.nivya.pairing.dto.DeviceInfoDto;
import com.nivya.pairing.entity.PairingRequest;
import com.nivya.pairing.entity.PairingStatus;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.pairing.service.PairingRateLimiter;
import com.nivya.role.RoleType;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.nivya.NivyaApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PairingCodeSecurityIntegrationTest {

    private static final Pattern PAIRING_CODE_PATTERN = Pattern.compile("^NV-[A-Z0-9]{4}-[A-Z0-9]{4}$");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private PairingRequestRepository pairingRequestRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PairingRateLimiter rateLimiter;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User parentUser;
    private User childUser;
    private String parentToken;
    private String childToken;

    @BeforeEach
    void setUp() {
        rateLimiter.clearAll();
        auditLogRepository.deleteAll();
        consentRepository.deleteAll();
        pairingRequestRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create and persist active parent user
        parentUser = new User();
        parentUser.setName("Sec Parent");
        parentUser.setEmail("sec.parent@nivya.local");
        parentUser.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        parentUser.setRole(RoleType.PARENT);
        parentUser.setStatus(UserStatus.ACTIVE);
        parentUser = userRepository.save(parentUser);
        parentToken = tokenProvider.generateAccessToken(UserPrincipal.create(parentUser));

        // Create and persist active child user
        childUser = new User();
        childUser.setName("Sec Child");
        childUser.setEmail("sec.child@nivya.local");
        childUser.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        childUser.setRole(RoleType.CHILD);
        childUser.setStatus(UserStatus.ACTIVE);
        childUser = userRepository.save(childUser);
        childToken = tokenProvider.generateAccessToken(UserPrincipal.create(childUser));
    }

    @AfterEach
    void tearDown() {
        rateLimiter.clearAll();
    }

    private String generatePairingCode(String token) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(matchesPattern(PAIRING_CODE_PATTERN.pattern())))
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.get("data").get("code").asText();
    }

    @Test
    @DisplayName("Requirements 1 & 2 & 3: Generate code A, refresh to get code B, verify code B != code A and both match NV-XXXX-XXXX")
    void testGenerateCodeA_andCodeB_VerifyDifferentAndFormat() throws Exception {
        // 1. Generate code A
        String codeA = generatePairingCode(parentToken);
        assertNotNull(codeA);
        assertTrue(PAIRING_CODE_PATTERN.matcher(codeA).matches(), "Code A must match format NV-XXXX-XXXX: " + codeA);

        // 2. Generate/refresh again (code B)
        String codeB = generatePairingCode(parentToken);
        assertNotNull(codeB);
        assertTrue(PAIRING_CODE_PATTERN.matcher(codeB).matches(), "Code B must match format NV-XXXX-XXXX: " + codeB);

        // 3. Verify code B != code A
        assertNotEquals(codeA, codeB, "Code B must not be equal to Code A on generation/refresh");
    }

    @Test
    @DisplayName("Requirement 4: Verify previous active pending code A is invalidated when code B is generated, and code A is rejected")
    void testPreviousCodeA_RejectedAfterCodeBGenerated() throws Exception {
        String codeA = generatePairingCode(parentToken);
        String codeB = generatePairingCode(parentToken);
        assertNotEquals(codeA, codeB);

        // Verify in DB that code A has status REVOKED
        PairingRequest reqA = pairingRequestRepository.findByConnectionCode(codeA).orElseThrow();
        assertEquals(PairingStatus.REVOKED, reqA.getStatus(), "Code A must be marked REVOKED in database");

        // Verify in DB that code B has status PENDING
        PairingRequest reqB = pairingRequestRepository.findByConnectionCode(codeB).orElseThrow();
        assertEquals(PairingStatus.PENDING, reqB.getStatus(), "Code B must be marked PENDING in database");

        // 4. Attempt pairing with code A -> MUST be rejected
        ConnectPairingRequest connectWithA = new ConnectPairingRequest(codeA, new DeviceInfoDto("child-dev-1", "Child Phone", "ANDROID", "14", "1.0", null));
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectWithA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This pairing code has already been used or invalidated."));
    }

    @Test
    @DisplayName("Requirement 5: Verify code B works only for the intended pairing flow")
    void testCodeB_WorksOnlyForIntendedPairingFlow() throws Exception {
        generatePairingCode(parentToken); // Code A
        String codeB = generatePairingCode(parentToken); // Code B

        // 5. Connect child device using code B
        ConnectPairingRequest connectWithB = new ConnectPairingRequest(codeB, new DeviceInfoDto("child-dev-2", "Child Phone", "ANDROID", "14", "1.0", null));
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectWithB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paired").value(true));

        // Verify pairing request is now ACCEPTED
        PairingRequest reqB = pairingRequestRepository.findByConnectionCode(codeB).orElseThrow();
        assertEquals(PairingStatus.ACCEPTED, reqB.getStatus());
        assertNotNull(reqB.getAcceptedAt());
        assertEquals(childUser.getId(), reqB.getAcceptedBy().getId());
    }

    @Test
    @DisplayName("Requirement 6: Verify code B becomes invalid immediately after successful use (one-time use)")
    void testCodeB_BecomesInvalidImmediatelyAfterSuccessfulUse() throws Exception {
        String codeB = generatePairingCode(parentToken);

        // First use: Connect child device
        ConnectPairingRequest connectWithB = new ConnectPairingRequest(codeB, new DeviceInfoDto("child-dev-3", "Child Phone", "ANDROID", "14", "1.0", null));
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectWithB)))
                .andExpect(status().isOk());

        // Second use attempt with same code B -> MUST be rejected
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectWithB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This pairing code has already been used or invalidated."));
    }

    @Test
    @DisplayName("Requirement 7: Verify expired code is rejected")
    void testExpiredCode_IsRejected() throws Exception {
        String code = generatePairingCode(parentToken);

        // Manually expire the code in database
        PairingRequest req = pairingRequestRepository.findByConnectionCode(code).orElseThrow();
        req.setExpiresAt(Instant.now().minusSeconds(60));
        pairingRequestRepository.save(req);

        // Attempt connection with expired code -> MUST be rejected
        ConnectPairingRequest connectReq = new ConnectPairingRequest(code, new DeviceInfoDto("child-dev-4", "Child Phone", "ANDROID", "14", "1.0", null));
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Pairing code has expired. Please request a new code."));
    }

    @Test
    @DisplayName("Requirement 8: Generate multiple successive codes and verify they are unique, never reused, and non-deterministic")
    void testMultipleSuccessiveCodes_NeverReused() throws Exception {
        int count = 20;
        Set<String> generatedCodes = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String code = generatePairingCode(parentToken);
            assertTrue(PAIRING_CODE_PATTERN.matcher(code).matches(), "Code must match format NV-XXXX-XXXX");
            assertFalse(generatedCodes.contains(code), "Generated code must be unique and never reused: " + code);
            generatedCodes.add(code);
        }

        assertEquals(count, generatedCodes.size(), "All " + count + " generated codes must be distinct");

        // Verify that the first 19 codes were automatically revoked
        List<PairingRequest> allRequests = pairingRequestRepository.findAll();
        long revokedCount = allRequests.stream().filter(r -> r.getStatus() == PairingStatus.REVOKED).count();
        long pendingCount = allRequests.stream().filter(r -> r.getStatus() == PairingStatus.PENDING).count();

        assertEquals(count - 1, revokedCount, "All previous codes must be REVOKED");
        assertEquals(1, pendingCount, "Only the last generated code must remain PENDING");
    }

    @Test
    @DisplayName("Security requirement: Plaintext pairing code is never logged in audit log entries")
    void testPlaintextCodeNeverLogged() throws Exception {
        String code = generatePairingCode(parentToken);

        // Trigger invalid code attempt
        ConnectPairingRequest invalidReq = new ConnectPairingRequest("NV-9999-9999", new DeviceInfoDto("child-dev-5", "Child Phone", "ANDROID", "14", "1.0", null));
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        // Verify audit logs do not contain plaintext code
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        for (AuditLog log : auditLogs) {
            String details = log.getDetails();
            assertFalse(details.contains(code), "Audit log must never log plaintext code: " + details);
            assertFalse(details.contains("NV-9999-9999"), "Audit log must not record raw attempted code: " + details);
        }
    }
}
