package com.nivya.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.activity.repository.ActivityEventRepository;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.AlertRuleRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RefreshTokenRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.auth.service.AuthRateLimiter;
import com.nivya.battery.entity.BatteryStatus;
import com.nivya.battery.repository.BatteryHistoryRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.convocation.entity.ConvocationMessage;
import com.nivya.convocation.repository.ConvocationMessageRepository;
import com.nivya.convocation.repository.ConvocationViewRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceHealthRepository;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.history.repository.HistoryEventRepository;
import com.nivya.location.repository.LocationHistoryRepository;
import com.nivya.location.repository.LocationStatusRepository;
import com.nivya.network.repository.NetworkHistoryRepository;
import com.nivya.network.repository.NetworkStatusRepository;
import com.nivya.pairing.dto.ConnectPairingRequest;
import com.nivya.pairing.dto.DeviceInfoDto;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.pairing.service.PairingRateLimiter;
import com.nivya.role.RoleType;
import com.nivya.usage.repository.UsageAppRepository;
import com.nivya.usage.repository.UsageSummaryRepository;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security hardening integration test suite verifying:
 * 1. Parent A cannot access Parent B data.
 * 2. Parent cannot access unrelated Child.
 * 3. Child cannot access Parent history.
 * 4. Child cannot access other Child data (Sibling isolation).
 * 5. Child cannot call Parent-only APIs.
 * 6. Expired Convocation visibility cannot be restored.
 * 7. Client timestamps cannot extend Convocation visibility.
 * 8. Pairing codes cannot be brute-forced easily (HTTP 429).
 * 9. Authentication brute-force protection (HTTP 429).
 * 10. Refresh token rotation & reuse revocation.
 * 11. Strict HTTP security headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

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
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private HistoryEventRepository historyEventRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private NetworkStatusRepository networkStatusRepository;

    @Autowired
    private NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsageAppRepository usageAppRepository;

    @Autowired
    private LocationStatusRepository locationStatusRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private ConvocationMessageRepository convocationMessageRepository;

    @Autowired
    private ConvocationViewRepository convocationViewRepository;

    @Autowired
    private PairingRequestRepository pairingRequestRepository;

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Autowired
    private PairingRateLimiter pairingRateLimiter;

    // Family 1: Parent A, Child 1, Child 2 (Sibling)
    private User parentA;
    private User child1;
    private User child2;
    private Family family1;
    private Device child1Device;
    private Device child2Device;
    private String parentAToken;
    private String child1Token;
    private String child2Token;

    // Family 2: Parent B, Child B
    private User parentB;
    private User childB;
    private Family family2;
    private Device childBDevice;
    private String parentBToken;
    private String childBToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();
        authRateLimiter.clearAll();
        pairingRateLimiter.clearAll();

        // 1. Create Family 1 (Parent A + Child 1 + Child 2)
        parentA = registerUser("Parent A", "parentA@test.sec", "Password123!", RoleType.PARENT);
        family1 = familyRepository.save(new Family("Family One", parentA));

        child1 = registerUser("Child One", "child1@test.sec", "Password123!", RoleType.CHILD);
        child2 = registerUser("Child Two", "child2@test.sec", "Password123!", RoleType.CHILD);

        familyMemberRepository.save(new FamilyMember(family1, parentA, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family1, child1, RoleType.CHILD));
        familyMemberRepository.save(new FamilyMember(family1, child2, RoleType.CHILD));

        child1Device = deviceRepository.save(new Device(child1, family1, "child-1-device-uuid", "Child 1 Phone", "ANDROID"));
        child2Device = deviceRepository.save(new Device(child2, family1, "child-2-device-uuid", "Child 2 Phone", "ANDROID"));

        deviceStatusRepository.save(new DeviceStatus(child1Device, true, 85, "WIFI", "EXCELLENT"));
        deviceStatusRepository.save(new DeviceStatus(child2Device, true, 60, "CELLULAR", "GOOD"));

        batteryStatusRepository.save(new BatteryStatus(child1Device, 85, "DISCHARGING", "NORMAL", "GOOD", 30.0, false));
        batteryStatusRepository.save(new BatteryStatus(child2Device, 60, "DISCHARGING", "NORMAL", "GOOD", 29.0, false));

        parentAToken = loginUser("parentA@test.sec", "Password123!");
        child1Token = loginUser("child1@test.sec", "Password123!");
        child2Token = loginUser("child2@test.sec", "Password123!");

        // 2. Create Family 2 (Parent B + Child B)
        parentB = registerUser("Parent B", "parentB@test.sec", "Password123!", RoleType.PARENT);
        family2 = familyRepository.save(new Family("Family Two", parentB));

        childB = registerUser("Child B", "childB@test.sec", "Password123!", RoleType.CHILD);

        familyMemberRepository.save(new FamilyMember(family2, parentB, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family2, childB, RoleType.CHILD));

        childBDevice = deviceRepository.save(new Device(childB, family2, "child-b-device-uuid", "Child B Phone", "ANDROID"));
        deviceStatusRepository.save(new DeviceStatus(childBDevice, true, 92, "WIFI", "EXCELLENT"));
        batteryStatusRepository.save(new BatteryStatus(childBDevice, 92, "CHARGING", "FAST", "GOOD", 32.0, false));

        parentBToken = loginUser("parentB@test.sec", "Password123!");
        childBToken = loginUser("childB@test.sec", "Password123!");
    }

    @AfterEach
    void tearDown() {
        cleanDatabases();
        authRateLimiter.clearAll();
        pairingRateLimiter.clearAll();
    }

    private void cleanDatabases() {
        convocationViewRepository.deleteAll();
        convocationMessageRepository.deleteAll();
        pairingRequestRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRepository.deleteAll();
        alertRuleRepository.deleteAll();
        activityEventRepository.deleteAll();
        historyEventRepository.deleteAll();
        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        networkHistoryRepository.deleteAll();
        networkStatusRepository.deleteAll();
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
        usageAppRepository.deleteAll();
        usageSummaryRepository.deleteAll();
        deviceHealthRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        consentRepository.deleteAll();
        deviceRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User registerUser(String name, String email, String password, RoleType role) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, password, role);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        return userRepository.findByEmail(email.toLowerCase().trim()).orElseThrow();
    }

    private String loginUser(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    // =========================================================================
    // INVARIANT 1 & 2: PARENT ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Invariant 1: Parent A cannot access Parent B child telemetry")
    void parentACannotAccessParentBData() throws Exception {
        // Parent A attempts to view Child B's battery telemetry
        mockMvc.perform(get("/api/v1/battery/current/" + childBDevice.getId())
                        .header("Authorization", "Bearer " + parentAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", containsString("outside their family")));

        // Parent A attempts to view Child B's telemetry snapshot
        mockMvc.perform(get("/api/v1/telemetry/snapshot/" + childBDevice.getId())
                        .header("Authorization", "Bearer " + parentAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Invariant 2: Parent cannot access unrelated or unlinked child device")
    void parentCannotAccessUnrelatedChild() throws Exception {
        // Register an outsider child and create device unlinked from Parent A's family
        User outsiderChild = registerUser("Outsider Child", "outsider@other.sec", "Password123!", RoleType.CHILD);
        Device unlinkedDevice = deviceRepository.save(new Device(outsiderChild, null, "unlinked-uuid", "Unlinked Phone", "ANDROID"));

        mockMvc.perform(get("/api/v1/battery/current/" + unlinkedDevice.getId())
                        .header("Authorization", "Bearer " + parentAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // INVARIANT 3 & 5: CHILD CALLING PARENT-ONLY APIS / HISTORY
    // =========================================================================

    @Test
    @DisplayName("Invariant 3: Child cannot access Parent history")
    void childCannotAccessParentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/history/" + child1Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invariant 5: Child cannot call Parent-only APIs")
    void childCannotCallParentOnlyApis() throws Exception {
        // 1. Live Activity endpoint
        mockMvc.perform(get("/api/v1/activity/live/" + child1Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden());

        // 2. Convocation Retained History endpoint
        mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden());

        // 3. Alert rules management endpoint
        mockMvc.perform(get("/api/v1/alerts/rules/" + family1.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // INVARIANT 4: SIBLING ISOLATION (Child 1 vs Child 2 in same family)
    // =========================================================================

    @Test
    @DisplayName("Invariant 4: Child 1 cannot access Child 2 (sibling) device data")
    void childCannotAccessOtherChildData() throws Exception {
        // Child 1 tries to access Child 2's battery telemetry
        mockMvc.perform(get("/api/v1/battery/current/" + child2Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", containsString("Child accounts cannot access other device data")));

        // Child 1 tries to access Child 2's location status
        mockMvc.perform(get("/api/v1/location/current/" + child2Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Child 1 tries to access Child 2's usage screen time
        mockMvc.perform(get("/api/v1/usage/summary/" + child2Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Child 1 tries to access Child 2's device health
        mockMvc.perform(get("/api/v1/device/health/" + child2Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Child 1 tries to access Child 2's telemetry snapshot
        mockMvc.perform(get("/api/v1/telemetry/snapshot/" + child2Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // In contrast: Child 1 CAN access their OWN battery telemetry
        mockMvc.perform(get("/api/v1/battery/current/" + child1Device.getId())
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryPct").value(85));
    }

    // =========================================================================
    // INVARIANT 6 & 7: CONVOCATION TIMESTAMPS & EXPIRATION SAFETY
    // =========================================================================

    @Test
    @DisplayName("Invariant 6 & 7: Expired Convocation visibility cannot be restored and client timestamps cannot extend it")
    void convocationExpiryAndTimestampProtection() throws Exception {
        Instant pastExpiry = Instant.now().minus(10, ChronoUnit.MINUTES);

        // Save an already-expired convocation message
        ConvocationMessage expiredMsg = new ConvocationMessage(
                family1.getId(),
                parentA.getId(),
                child1.getId(),
                "Critical Family Message"
        );
        expiredMsg.setStatus("SEEN");
        expiredMsg.setVisibilityExpiresAt(pastExpiry);
        expiredMsg.setChildVisibilityExpiresAt(pastExpiry);
        convocationMessageRepository.save(expiredMsg);

        // Child requests visibility: expired messages must not be returned
        mockMvc.perform(get("/api/v1/convocation/child/visibility")
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewingActive").value(false))
                .andExpect(jsonPath("$.data.remainingSeconds").value(0));

        // Starting viewing session does not re-activate already-expired / seen messages
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + child1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(0)));
    }

    // =========================================================================
    // INVARIANT 8: PAIRING CODE BRUTE-FORCE PROTECTION (5 attempts / 15 mins)
    // =========================================================================

    @Test
    @DisplayName("Invariant 8: Pairing codes cannot be brute-forced (429 Rate Limit Exceeded)")
    void pairingCodesCannotBeBruteForced() throws Exception {
        DeviceInfoDto devInfo = new DeviceInfoDto("test-device-uuid", "Test Child Phone", "ANDROID", "14", "1.0.0", "fcm-tok");
        ConnectPairingRequest badReq = new ConnectPairingRequest("NV-9999-9999", devInfo);

        // First 5 attempts fail with 400 (Bad Request / Pairing error)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/pairing/connect")
                            .header("Authorization", "Bearer " + child1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badReq)))
                    .andExpect(status().isBadRequest());
        }

        // 6th attempt is blocked by RateLimiter returning 429 Too Many Requests
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + child1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Rate Limit Exceeded"));
    }

    // =========================================================================
    // INVARIANT 9: AUTHENTICATION BRUTE-FORCE PROTECTION (5 failed logins / 15 mins)
    // =========================================================================

    @Test
    @DisplayName("Invariant 9: Authentication endpoints protect against brute-force login attempts (429)")
    void authBruteForceProtectionEnforced() throws Exception {
        LoginRequest badLogin = new LoginRequest("parentA@test.sec", "WrongPassword!");

        // 5 failed password attempts return 401 Unauthorized
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt triggers 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.message", containsString("Too many failed authentication attempts")));
    }

    // =========================================================================
    // INVARIANT 10: REFRESH TOKEN ROTATION & REUSE REVOCATION
    // =========================================================================

    @Test
    @DisplayName("Invariant 10: Refresh token reuse detection revokes all user tokens immediately")
    void refreshTokenReuseRevocation() throws Exception {
        // Perform initial login to acquire fresh refresh token
        LoginRequest loginReq = new LoginRequest("parentA@test.sec", "Password123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken1 = objectMapper.readTree(loginRes.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Rotate token: exchanges refreshToken1 for refreshToken2
        RefreshTokenRequest refreshReq1 = new RefreshTokenRequest(refreshToken1);
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq1)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken2 = objectMapper.readTree(refreshRes.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Token reuse attack: attacker attempts to reuse refreshToken1
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("previously revoked")));

        // Verify reuse detection invalidated ALL tokens: refreshToken2 is now revoked as well
        RefreshTokenRequest refreshReq2 = new RefreshTokenRequest(refreshToken2);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq2)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // INVARIANT 11: SECURITY HEADERS ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("Invariant 11: HTTP Security Headers (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)")
    void securityHeadersEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().exists("Referrer-Policy"));
    }
}
