package com.nivya.pairing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.audit.repository.AuditLogRepository;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.pairing.dto.ConnectPairingRequest;
import com.nivya.pairing.dto.DeviceInfoDto;
import com.nivya.pairing.dto.RevokePairingRequest;
import com.nivya.pairing.entity.PairingRequest;
import com.nivya.pairing.entity.PairingStatus;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.pairing.service.PairingRateLimiter;
import com.nivya.role.RoleType;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Integration Tests for the Nivya 10-Step Pairing System.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PairingIntegrationTest {

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
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        setUp();
    }

    private String registerAndGetToken(String name, String email, RoleType role) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(name, email, "StrongPass123!", role);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("1. Parent and Child can both generate valid NV-XXXX-XXXX pairing codes with 10-minute TTL")
    void testGeneratePairingCodes() throws Exception {
        String parentToken = registerAndGetToken("Alice Parent", "alice.p@nivya.local", RoleType.PARENT);
        String childToken = registerAndGetToken("Charlie Child", "charlie.c@nivya.local", RoleType.CHILD);

        // Parent generates code -> Target should be CHILD
        MvcResult parentResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesPattern("^NV-[A-Z0-9]{4}-[A-Z0-9]{4}$")))
                .andExpect(jsonPath("$.data.myRole").value("PARENT"))
                .andExpect(jsonPath("$.data.targetRole").value("CHILD"))
                .andExpect(jsonPath("$.data.ttlSeconds").value(600))
                .andReturn();

        // Child generates code -> Target should be PARENT
        mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(org.hamcrest.Matchers.matchesPattern("^NV-[A-Z0-9]{4}-[A-Z0-9]{4}$")))
                .andExpect(jsonPath("$.data.myRole").value("CHILD"))
                .andExpect(jsonPath("$.data.targetRole").value("PARENT"));
    }

    @Test
    @DisplayName("2. 10-Step Protocol: Parent enters Child's valid code establishing persistent Family and Device link")
    void testSuccessfulPairing_ParentEntersChildCode() throws Exception {
        String parentToken = registerAndGetToken("Parent Jane", "jane.parent@nivya.local", RoleType.PARENT);
        String childToken = registerAndGetToken("Child Timmy", "timmy.child@nivya.local", RoleType.CHILD);

        // Child generates code
        MvcResult childCodeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andReturn();

        String childCode = objectMapper.readTree(childCodeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        // Parent submits Child's code with Parent device info
        ConnectPairingRequest connectRequest = new ConnectPairingRequest(
                childCode,
                new DeviceInfoDto("dev-parent-uuid-1", "Jane's Galaxy S24", "ANDROID", "14", "1.0.0", "fcm-parent-token")
        );

        MvcResult connectResult = mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paired").value(true))
                .andExpect(jsonPath("$.data.familyCode").isNotEmpty())
                .andExpect(jsonPath("$.data.members.length()").value(2))
                .andReturn();

        JsonNode responseData = objectMapper.readTree(connectResult.getResponse().getContentAsString()).get("data");
        Long familyId = responseData.get("familyId").asLong();

        // Verify token invalidated (ACCEPTED)
        PairingRequest pairingRequest = pairingRequestRepository.findByConnectionCode(childCode).orElseThrow();
        assertEquals(PairingStatus.ACCEPTED, pairingRequest.getStatus());
        assertNotNull(pairingRequest.getAcceptedAt());

        // Verify Consent records created
        assertEquals(2, consentRepository.findByFamilyId(familyId).size());

        // Verify Audit logs created
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a -> a.getAction().equals("PAIRING_SUCCESSFUL")));
    }

    @Test
    @DisplayName("3. Bidirectional Pairing: Child enters Parent's code establishing same persistent link")
    void testSuccessfulPairing_ChildEntersParentCode() throws Exception {
        String parentToken = registerAndGetToken("Parent Mark", "mark.parent@nivya.local", RoleType.PARENT);
        String childToken = registerAndGetToken("Child Sarah", "sarah.child@nivya.local", RoleType.CHILD);

        // Parent generates code
        MvcResult parentCodeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        String parentCode = objectMapper.readTree(parentCodeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        // Child submits Parent's code with Child device info
        ConnectPairingRequest connectRequest = new ConnectPairingRequest(
                parentCode,
                new DeviceInfoDto("dev-child-uuid-2", "Sarah's Pixel 8", "ANDROID", "14", "1.0.0", "fcm-child-token")
        );

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paired").value(true))
                .andExpect(jsonPath("$.data.members.length()").value(2));
    }

    @Test
    @DisplayName("4. Security: User entering own pairing code is rejected (self-pairing prevention)")
    void testSelfPairingPrevention() throws Exception {
        String parentToken = registerAndGetToken("Parent Self", "self.parent@nivya.local", RoleType.PARENT);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        String ownCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        ConnectPairingRequest connectRequest = new ConnectPairingRequest(
                ownCode,
                new DeviceInfoDto("dev-self-uuid", "Self Device", "ANDROID", "14", "1.0.0", null)
        );

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot pair a device with itself."));
    }

    @Test
    @DisplayName("5. Security: Incompatible role pairing (Parent to Parent) is rejected")
    void testRoleIncompatibilityPrevention() throws Exception {
        String parentToken1 = registerAndGetToken("Parent One", "p1@nivya.local", RoleType.PARENT);
        String parentToken2 = registerAndGetToken("Parent Two", "p2@nivya.local", RoleType.PARENT);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken1))
                .andExpect(status().isOk())
                .andReturn();

        String parent1Code = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        ConnectPairingRequest connectRequest = new ConnectPairingRequest(parent1Code, null);

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Role incompatibility: Pairing requires one Parent and one Child device."));
    }

    @Test
    @DisplayName("6. Security: Expired pairing code is rejected")
    void testExpiredCodePrevention() throws Exception {
        String childToken = registerAndGetToken("Child Expire", "expire.child@nivya.local", RoleType.CHILD);
        String parentToken = registerAndGetToken("Parent Expire", "expire.parent@nivya.local", RoleType.PARENT);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andReturn();

        String code = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        // Artificially expire the code in DB
        PairingRequest req = pairingRequestRepository.findByConnectionCode(code).orElseThrow();
        req.setExpiresAt(Instant.now().minusSeconds(60));
        pairingRequestRepository.save(req);

        ConnectPairingRequest connectRequest = new ConnectPairingRequest(code, null);

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Pairing code has expired. Please request a new code."));
    }

    @Test
    @DisplayName("7. Security: Reusing an already accepted one-time code is rejected")
    void testCodeReusePrevention() throws Exception {
        String parentToken = registerAndGetToken("Parent Reuse", "reuse.parent@nivya.local", RoleType.PARENT);
        String childToken1 = registerAndGetToken("Child One", "c1.reuse@nivya.local", RoleType.CHILD);
        String childToken2 = registerAndGetToken("Child Two", "c2.reuse@nivya.local", RoleType.CHILD);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andReturn();

        String parentCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        ConnectPairingRequest connectRequest = new ConnectPairingRequest(parentCode, null);

        // Child 1 uses code -> Success
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk());

        // Child 2 attempts to reuse the same code -> Rejected
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("This pairing code has already been used or invalidated."));
    }

    @Test
    @DisplayName("8. Security: Rate limiting blocks brute-force attempts after 5 failures")
    void testRateLimitingBruteForce() throws Exception {
        String parentToken = registerAndGetToken("Parent Rate", "rate.parent@nivya.local", RoleType.PARENT);

        ConnectPairingRequest invalidRequest = new ConnectPairingRequest("NV-9999-9999", null);

        // Perform 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/pairing/connect")
                            .header("Authorization", "Bearer " + parentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        // 6th attempt should trigger HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many failed pairing attempts")));
    }

    @Test
    @DisplayName("9. Offline Indicator: Parent sees last-known device telemetry with offline/stale status when Child is offline")
    void testOfflineDeviceStatusIndication() throws Exception {
        String parentToken = registerAndGetToken("Parent State", "state.parent@nivya.local", RoleType.PARENT);
        String childToken = registerAndGetToken("Child State", "state.child@nivya.local", RoleType.CHILD);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andReturn();

        String childCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        // Connect Child device
        ConnectPairingRequest connectRequest = new ConnectPairingRequest(
                childCode,
                new DeviceInfoDto("child-tablet-uuid", "Timmy's Tablet", "ANDROID", "14", "1.0", "push-token")
        );

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk());

        // Mark the device offline and stale in database (simulating child disconnecting)
        Device device = deviceRepository.findByDeviceUuid("child-tablet-uuid").orElseThrow();
        DeviceStatus status = deviceStatusRepository.findByDeviceId(device.getId()).orElseThrow();
        status.setOnline(false);
        status.setBatteryPct(42);
        status.setNetworkType("CELLULAR");
        status.setNetworkQuality("POOR");
        status.setLastSyncAt(Instant.now().minusSeconds(600)); // 10 minutes ago
        deviceStatusRepository.saveAndFlush(status);

        device.setLastSeenAt(Instant.now().minusSeconds(600));
        deviceRepository.saveAndFlush(device);

        // Parent requests /api/v1/pairing/status
        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paired").value(true))
                .andExpect(jsonPath("$.data.devices[0].deviceUuid").value("child-tablet-uuid"))
                .andExpect(jsonPath("$.data.devices[0].online").value(false))
                .andExpect(jsonPath("$.data.devices[0].stale").value(true))
                .andExpect(jsonPath("$.data.devices[0].batteryPct").value(42))
                .andExpect(jsonPath("$.data.devices[0].networkType").value("CELLULAR"));
    }

    @Test
    @DisplayName("10. Parent can revoke a paired device link")
    void testRevokePairing() throws Exception {
        String parentToken = registerAndGetToken("Parent Revoker", "revoker.p@nivya.local", RoleType.PARENT);
        String childToken = registerAndGetToken("Child Revokee", "revokee.c@nivya.local", RoleType.CHILD);

        MvcResult codeResult = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andReturn();

        String childCode = objectMapper.readTree(codeResult.getResponse().getContentAsString())
                .get("data").get("code").asText();

        ConnectPairingRequest connectRequest = new ConnectPairingRequest(
                childCode,
                new DeviceInfoDto("child-phone-to-revoke", "Revoke Phone", "ANDROID", "14", "1.0", null)
        );

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectRequest)))
                .andExpect(status().isOk());

        Device device = deviceRepository.findByDeviceUuid("child-phone-to-revoke").orElseThrow();

        RevokePairingRequest revokeReq = new RevokePairingRequest(device.getId());

        mockMvc.perform(post("/api/v1/pairing/revoke")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Device link revoked successfully"));

        Device revokedDevice = deviceRepository.findById(device.getId()).orElseThrow();
        assertEquals("REVOKED", revokedDevice.getStatus());
        assertNull(revokedDevice.getFamily());
    }
}
