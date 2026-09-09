package com.nivya.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.battery.repository.BatteryHistoryRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.dto.DeviceHealthTelemetryRequest;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceHealth;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceHealthRepository;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.location.repository.LocationHistoryRepository;
import com.nivya.location.repository.LocationStatusRepository;
import com.nivya.network.repository.NetworkHistoryRepository;
import com.nivya.network.repository.NetworkStatusRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private LocationStatusRepository locationStatusRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private UsageAppRepository usageAppRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private NetworkStatusRepository networkStatusRepository;

    @Autowired
    private BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User parentUser;
    private User childUser;
    private User otherParentUser;
    private Family family;
    private Device childDevice;
    private String childToken;
    private String parentToken;
    private String otherParentToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent User
        RegisterRequest parentReq = new RegisterRequest("Health Parent", "parent.health@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode pNode = objectMapper.readTree(pRes.getResponse().getContentAsString());
        parentToken = pNode.get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.health@nivya.local").orElseThrow();

        // 2. Register Child User
        RegisterRequest childReq = new RegisterRequest("Health Child", "child.health@nivya.local", "Password123!", RoleType.CHILD);
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cNode = objectMapper.readTree(cRes.getResponse().getContentAsString());
        childToken = cNode.get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.health@nivya.local").orElseThrow();

        // 3. Register Unrelated Parent for cross-family isolation check
        RegisterRequest otherReq = new RegisterRequest("Other Parent", "other.parent@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult oRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode oNode = objectMapper.readTree(oRes.getResponse().getContentAsString());
        otherParentToken = oNode.get("data").get("accessToken").asText();
        otherParentUser = userRepository.findByEmail("other.parent@nivya.local").orElseThrow();

        // 4. Setup Family linking Parent and Child
        family = new Family("Health Family", parentUser);
        family = familyRepository.save(family);
        FamilyMember fmParent = new FamilyMember(family, parentUser, RoleType.PARENT);
        FamilyMember fmChild = new FamilyMember(family, childUser, RoleType.CHILD);
        familyMemberRepository.saveAll(java.util.List.of(fmParent, fmChild));

        // 5. Enroll Child Device
        childDevice = new Device(childUser, family, "dev-child-health-001", "Alex Galaxy A54", "ANDROID");
        childDevice.setOsVersion("14");
        childDevice = deviceRepository.save(childDevice);
        deviceStatusRepository.save(new DeviceStatus(childDevice, true, 80, "WIFI", "EXCELLENT"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabases();
    }

    private void cleanDatabases() {
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
        deviceHealthRepository.deleteAll();
        usageAppRepository.deleteAll();
        usageSummaryRepository.deleteAll();
        networkHistoryRepository.deleteAll();
        networkStatusRepository.deleteAll();
        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        alertRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        consentRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Child records healthy device telemetry: Storage, Memory, Battery, Permissions, Specs")
    void testRecordDeviceHealthTelemetry_Success() throws Exception {
        DeviceHealthTelemetryRequest req = new DeviceHealthTelemetryRequest();
        req.setDeviceUuid(childDevice.getDeviceUuid());
        req.setDeviceModel("Samsung SM-A546B");
        req.setDeviceManufacturer("Samsung");
        req.setOsVersion("14");
        req.setSdkVersion(34);
        req.setBatteryPct(82);
        req.setChargingState("NOT_CHARGING");
        req.setBatteryHealth("GOOD");
        req.setBatteryTempCelsius(30.5);
        req.setStorageTotalBytes(128_000_000_000L);
        req.setStorageUsedBytes(45_000_000_000L);
        req.setStorageFreeBytes(83_000_000_000L);
        req.setRamTotalBytes(8_000_000_000L);
        req.setRamUsedBytes(3_200_000_000L);
        req.setRamFreeBytes(4_800_000_000L);
        req.setIsLowRam(false);
        req.setNetworkType("WIFI");
        req.setIsOnline(true);
        req.setLocationPermission("GRANTED");
        req.setUsagePermission("GRANTED");
        req.setNotificationPermission("GRANTED");
        req.setBatteryOptimization("OPTIMIZED");
        req.setSyncState("SYNCED");
        req.setRecordedAt(Instant.now());

        mockMvc.perform(post("/api/v1/device/health/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value(childDevice.getId()))
                .andExpect(jsonPath("$.data.deviceModel").value("Samsung SM-A546B"))
                .andExpect(jsonPath("$.data.batteryPct").value(82))
                .andExpect(jsonPath("$.data.healthScore").value(100))
                .andExpect(jsonPath("$.data.healthStatus").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.storage.totalBytes").value(128_000_000_000L))
                .andExpect(jsonPath("$.data.memory.totalBytes").value(8_000_000_000L))
                .andExpect(jsonPath("$.data.permissionHealth.allHealthy").value(true))
                .andExpect(jsonPath("$.data.conditionSummary").value("Your phone is running great! 🎉"));

        // Verify entity persisted in repository
        Optional<DeviceHealth> savedOpt = deviceHealthRepository.findByDeviceId(childDevice.getId());
        assertTrue(savedOpt.isPresent());
        DeviceHealth saved = savedOpt.get();
        assertEquals(82, saved.getBatteryPct());
        assertEquals("Samsung SM-A546B", saved.getDeviceModel());
        assertEquals("14", saved.getOsVersion());
        assertTrue(saved.isAllPermissionsHealthy());
        assertEquals(100, saved.getHealthScore());

        // Verify DeviceStatus updated
        DeviceStatus ds = deviceStatusRepository.findByDeviceId(childDevice.getId()).orElseThrow();
        assertEquals(82, ds.getBatteryPct());
        assertEquals("WIFI", ds.getNetworkType());
        assertTrue(ds.isOnline());
    }

    @Test
    @DisplayName("Parent can query detailed child device health diagnostics")
    void testParentCanRetrieveChildDeviceHealth_Success() throws Exception {
        // Ingest telemetry first
        DeviceHealth health = new DeviceHealth(childDevice, Instant.now());
        health.setDeviceModel("Samsung Galaxy A54");
        health.setDeviceManufacturer("Samsung");
        health.setOsVersion("14");
        health.setSdkVersion(34);
        health.setBatteryPct(65);
        health.setChargingState("CHARGING_AC");
        health.setBatteryHealth("GOOD");
        health.setBatteryTempCelsius(32.0);
        health.setStorageTotalBytes(128_000_000_000L);
        health.setStorageUsedBytes(60_000_000_000L);
        health.setStorageFreeBytes(68_000_000_000L);
        health.setRamTotalBytes(8_000_000_000L);
        health.setRamUsedBytes(4_000_000_000L);
        health.setRamFreeBytes(4_000_000_000L);
        health.setLowRam(false);
        health.setNetworkType("CELLULAR");
        health.setOnline(true);
        health.setLocationPermission("GRANTED");
        health.setUsagePermission("GRANTED");
        health.setNotificationPermission("GRANTED");
        health.setBatteryOptimization("OPTIMIZED");
        health.setAllPermissionsHealthy(true);
        health.setHealthScore(95);
        health.setHealthStatus("EXCELLENT");
        health.setSyncState("SYNCED");
        deviceHealthRepository.save(health);

        mockMvc.perform(get("/api/v1/device/health/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value(childDevice.getId()))
                .andExpect(jsonPath("$.data.deviceModel").value("Samsung Galaxy A54"))
                .andExpect(jsonPath("$.data.batteryPct").value(65))
                .andExpect(jsonPath("$.data.chargingState").value("CHARGING_AC"))
                .andExpect(jsonPath("$.data.storage.usedBytes").value(60_000_000_000L))
                .andExpect(jsonPath("$.data.storage.freeBytes").value(68_000_000_000L))
                .andExpect(jsonPath("$.data.memory.totalBytes").value(8_000_000_000L))
                .andExpect(jsonPath("$.data.permissionHealth.locationPermission").value("GRANTED"))
                .andExpect(jsonPath("$.data.permissionHealth.allHealthy").value(true));
    }

    @Test
    @DisplayName("Child can query own device health via /my endpoint")
    void testChildCanRetrieveOwnDeviceHealth_Success() throws Exception {
        mockMvc.perform(get("/api/v1/device/health/my")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value(childDevice.getId()))
                .andExpect(jsonPath("$.data.conditionSummary").exists());
    }

    @Test
    @DisplayName("Cross-family isolation: Unrelated Parent cannot query child device health")
    void testCrossFamilyIsolation_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/device/health/" + childDevice.getId())
                        .header("Authorization", "Bearer " + otherParentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Telemetry with low storage and revoked permissions drops health score and creates alerts")
    void testPermissionAndStorageAlertsAndScoreDeduction() throws Exception {
        DeviceHealthTelemetryRequest req = new DeviceHealthTelemetryRequest();
        req.setDeviceUuid(childDevice.getDeviceUuid());
        req.setDeviceModel("Samsung SM-A546B");
        req.setBatteryPct(12); // Critical battery
        req.setChargingState("NOT_CHARGING");
        req.setStorageTotalBytes(100_000_000_000L);
        req.setStorageUsedBytes(98_000_000_000L);
        req.setStorageFreeBytes(2_000_000_000L); // 2% free -> critical storage
        req.setRamTotalBytes(4_000_000_000L);
        req.setRamUsedBytes(3_800_000_000L);
        req.setIsLowRam(true); // Low ram
        req.setLocationPermission("DENIED"); // Revoked location
        req.setUsagePermission("REQUIRED"); // Revoked usage
        req.setNotificationPermission("DENIED"); // Revoked notifications
        req.setRecordedAt(Instant.now());

        MvcResult res = mockMvc.perform(post("/api/v1/device/health/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.data.permissionHealth.allHealthy").value(false))
                .andExpect(jsonPath("$.data.storage.isLowStorage").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        int score = root.get("data").get("healthScore").asInt();
        assertTrue(score <= 50, "Expected degraded score <= 50, got: " + score);

        // Verify Alert persisted for Low Storage
        assertTrue(alertRepository.findFirstByDeviceIdAndAlertTypeAndResolvedFalse(childDevice.getId(), "LOW_STORAGE").isPresent());

        // Verify Alert persisted for Revoked Permissions
        assertTrue(alertRepository.findFirstByDeviceIdAndAlertTypeAndResolvedFalse(childDevice.getId(), "PERMISSION_REVOKED").isPresent());
    }
}
