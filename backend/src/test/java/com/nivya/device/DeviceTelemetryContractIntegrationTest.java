package com.nivya.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.battery.dto.BatteryTelemetryRequest;
import com.nivya.device.dto.DeviceHeartbeatRequest;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
import com.nivya.device.repository.DeviceHealthRepository;
import com.nivya.location.repository.LocationStatusRepository;
import com.nivya.network.repository.NetworkStatusRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceTelemetryContractIntegrationTest {

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
    private BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private LocationStatusRepository locationStatusRepository;

    @Autowired
    private NetworkStatusRepository networkStatusRepository;

    @Autowired
    private DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User parentUser;
    private User childUser;
    private User outsiderUser;
    private Family family;
    private Device childDevice;
    private String parentToken;
    private String childToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() throws Exception {
        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        locationStatusRepository.deleteAll();
        networkStatusRepository.deleteAll();
        deviceHealthRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register Parent
        MvcResult parentRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Parent User", "parent.telemetry@nivya.local", "Password123!", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = extractToken(parentRes);
        parentUser = userRepository.findByEmail("parent.telemetry@nivya.local").orElseThrow();

        // 2. Register Child
        MvcResult childRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Child User", "child.telemetry@nivya.local", "Password123!", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = extractToken(childRes);
        childUser = userRepository.findByEmail("child.telemetry@nivya.local").orElseThrow();

        // 3. Register Outsider Parent
        MvcResult outsiderRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Outsider User", "outsider.telemetry@nivya.local", "Password123!", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        outsiderToken = extractToken(outsiderRes);
        outsiderUser = userRepository.findByEmail("outsider.telemetry@nivya.local").orElseThrow();

        // 4. Create Family
        family = new Family("Telemetry Family", parentUser);
        familyRepository.save(family);
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 5. Enroll Child Device
        childDevice = new Device(childUser, family, "device-child-telemetry-01", "Alex Android Phone", "ANDROID");
        deviceRepository.save(childDevice);
    }

    @Test
    @DisplayName("Unrecorded telemetry endpoints return 200 OK with null data (not 404)")
    void testUnrecordedTelemetryEndpointsReturn200WithNullData() throws Exception {
        // Battery
        mockMvc.perform(get("/api/v1/battery/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("No battery telemetry recorded yet"));

        // Location
        mockMvc.perform(get("/api/v1/location/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("No location data recorded yet"));

        // Network
        mockMvc.perform(get("/api/v1/network/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("No network telemetry recorded yet"));

        // Health
        mockMvc.perform(get("/api/v1/device/health/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("No device health telemetry recorded yet"));
    }

    @Test
    @DisplayName("Non-existent device ID returns 404 for telemetry queries")
    void testNonExistentDeviceIdReturns404() throws Exception {
        long nonExistentId = 888888L;
        mockMvc.perform(get("/api/v1/battery/current/" + nonExistentId)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/location/current/" + nonExistentId)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/network/current/" + nonExistentId)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/device/health/" + nonExistentId)
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-family access to child device telemetry is denied with 403 Forbidden")
    void testCrossFamilyAccessReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/battery/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/location/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/network/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/device/health/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Device Heartbeat updates lastSeenAt and online status in database and pairing status")
    void testHeartbeatIngestionAndStatus() throws Exception {
        DeviceHeartbeatRequest hb = new DeviceHeartbeatRequest(
                childDevice.getId(),
                childDevice.getDeviceUuid(),
                87,
                "WIFI",
                "EXCELLENT",
                true
        );

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hb)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.batteryPct").value(87))
                .andExpect(jsonPath("$.data.networkType").value("WIFI"));

        Device refreshed = deviceRepository.findById(childDevice.getId()).orElseThrow();
        assertNotNull(refreshed.getLastSeenAt());

        // Verify pairing status reflects online
        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.devices[0].online").value(true))
                .andExpect(jsonPath("$.data.devices[0].stale").value(false));
    }

    @Test
    @DisplayName("Real telemetry persistence: when real battery is sent, current endpoint returns real values")
    void testRealTelemetryPersistenceAndRetrieval() throws Exception {
        BatteryTelemetryRequest batReq = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(),
                63,
                "CHARGING",
                "AC",
                "GOOD",
                27.5,
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batReq)))
                .andExpect(status().isOk());

        // Parent retrieves real values
        mockMvc.perform(get("/api/v1/battery/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryPct").value(63))
                .andExpect(jsonPath("$.data.chargingState").value("CHARGING"))
                .andExpect(jsonPath("$.data.temperatureCelsius").value(27.5));
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
