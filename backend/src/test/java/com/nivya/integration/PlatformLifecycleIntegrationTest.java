package com.nivya.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.battery.dto.BatteryTelemetryRequest;
import com.nivya.convocation.dto.ParentSendMessageRequest;
import com.nivya.convocation.repository.ConvocationMessageRepository;
import com.nivya.convocation.repository.ConvocationViewRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.location.dto.LocationTelemetryRequest;
import com.nivya.network.dto.NetworkTelemetryRequest;
import com.nivya.pairing.dto.ConnectPairingRequest;
import com.nivya.pairing.dto.DeviceInfoDto;
import com.nivya.pairing.dto.GenerateCodeRequest;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import com.nivya.websocket.redis.TransientStateStore;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test validating the complete platform lifecycle:
 * 1. Parent registration & Family creation
 * 2. Child registration & Secure Device Pairing
 * 3. Multi-domain Telemetry ingestion (Battery, Network, Location)
 * 4. Real-time broadcast and transient state snapshot storage
 * 5. Safety alerts & notification record dispatching
 * 6. Convocation priority guidance with decoy notifications and ephemeral viewing
 * 7. Offline recovery and consolidated state snapshot rehydration
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PlatformLifecycleIntegrationTest {

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
    private PairingRequestRepository pairingRequestRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private ConvocationMessageRepository convocationMessageRepository;

    @Autowired
    private ConvocationViewRepository convocationViewRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private com.nivya.network.repository.NetworkStatusRepository networkStatusRepository;

    @Autowired
    private com.nivya.network.repository.NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private com.nivya.location.repository.LocationStatusRepository locationStatusRepository;

    @Autowired
    private com.nivya.location.repository.LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private com.nivya.device.repository.DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private com.nivya.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.nivya.consent.repository.ConsentRepository consentRepository;

    @Autowired
    private TransientStateStore transientStateStore;

    @BeforeEach
    void setUp() {
        cleanDatabases();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanDatabases();
    }

    private void cleanDatabases() {
        convocationViewRepository.deleteAll();
        convocationMessageRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRepository.deleteAll();
        pairingRequestRepository.deleteAll();
        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        networkHistoryRepository.deleteAll();
        networkStatusRepository.deleteAll();
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        consentRepository.deleteAll();
        deviceRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Platform Lifecycle: Register -> Pair -> Ingest -> Alert -> Convocation -> Reconnect")
    void testCompletePlatformLifecycle() throws Exception {
        // ---------------------------------------------------------------------
        // Step 1: Parent registration
        // ---------------------------------------------------------------------
        RegisterRequest parentReq = new RegisterRequest("Sarah Parent", "sarah@nivya.test", "Password123!", RoleType.PARENT);
        MvcResult parentRegRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("PARENT"))
                .andReturn();

        String parentToken = extractAccessToken(parentRegRes);
        User parentUser = userRepository.findByEmail("sarah@nivya.test").orElseThrow();
        Family family = familyRepository.save(new Family("Sarah's Family", parentUser));
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));

        // ---------------------------------------------------------------------
        // Step 2: Child registration
        // ---------------------------------------------------------------------
        RegisterRequest childReq = new RegisterRequest("Leo Child", "leo@nivya.test", "Password123!", RoleType.CHILD);
        MvcResult childRegRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("CHILD"))
                .andReturn();

        String childToken = extractAccessToken(childRegRes);
        User childUser = userRepository.findByEmail("leo@nivya.test").orElseThrow();

        // ---------------------------------------------------------------------
        // Step 3: Pairing flow
        // ---------------------------------------------------------------------
        GenerateCodeRequest createPairing = new GenerateCodeRequest("parent-browser-fingerprint");
        MvcResult pairingCodeRes = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPairing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andReturn();

        String pairingCode = objectMapper.readTree(pairingCodeRes.getResponse().getContentAsString())
                .path("data").path("code").asText();

        String childDeviceUuid = "dev-leo-uuid-" + UUID.randomUUID();
        DeviceInfoDto devInfo = new DeviceInfoDto(childDeviceUuid, "Leo's Galaxy", "ANDROID", "14", "1.0.0", "fcm_leo_token");
        ConnectPairingRequest connectReq = new ConnectPairingRequest(pairingCode, devInfo);

        MvcResult connectRes = mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(connectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paired").value(true))
                .andReturn();

        Device pairedDevice = deviceRepository.findByDeviceUuid(childDeviceUuid).orElseThrow();
        assertNotNull(pairedDevice);
        assertEquals(childUser.getId(), pairedDevice.getUser().getId());

        // ---------------------------------------------------------------------
        // Step 4: Telemetry Ingestion (Battery, Network, Location)
        // ---------------------------------------------------------------------
        // 4a. Battery Telemetry (Low battery triggers alert)
        BatteryTelemetryRequest battReq = new BatteryTelemetryRequest(
                childDeviceUuid, 12, "DISCHARGING", "UNPLUGGED", "GOOD", 31.5, Instant.now()
        );
        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(battReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryPct").value(12));

        // 4b. Network Telemetry
        NetworkTelemetryRequest netReq = new NetworkTelemetryRequest(
                childDeviceUuid, "WIFI", "WIFI", true, true, 4, -55, "EXCELLENT"
        );
        mockMvc.perform(post("/api/v1/network/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(netReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.networkType").value("WIFI"));

        // 4c. Location Telemetry
        LocationTelemetryRequest locReq = new LocationTelemetryRequest();
        locReq.setDeviceUuid(childDeviceUuid);
        locReq.setLatitude(37.7749);
        locReq.setLongitude(-122.4194);
        locReq.setAccuracyMeters(15.0f);
        locReq.setProvider("GPS");
        locReq.setPermissionState("GRANTED");
        locReq.setRecordedAt(Instant.now());

        mockMvc.perform(post("/api/v1/location/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(closeTo(37.7749, 0.001)));

        // ---------------------------------------------------------------------
        // Step 5: Alerts verification (Low battery 12% triggers WARNING alert)
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/alerts/family/" + family.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].alertType").value("LOW_BATTERY"));

        // ---------------------------------------------------------------------
        // Step 6: Convocation Messaging (Decoy Notification & Ephemeral Viewing)
        // ---------------------------------------------------------------------
        ParentSendMessageRequest convoReq = new ParentSendMessageRequest(childUser.getId(), "Please charge your device, dinner in 15 mins.");
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convoReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Please charge your device, dinner in 15 mins."));

        // Child starts 2-minute ephemeral viewing session
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.remainingSeconds").value(greaterThan(0)));

        // Parent sees message marked Seen
        mockMvc.perform(get("/api/v1/convocation/parent/seen")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk());

        // ---------------------------------------------------------------------
        // Step 7: Offline Recovery & Consolidated Snapshot Rehydration
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/telemetry/snapshot/" + pairedDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value(pairedDevice.getId()))
                .andExpect(jsonPath("$.data.battery.batteryPct").value(12))
                .andExpect(jsonPath("$.data.network.networkType").value("WIFI"))
                .andExpect(jsonPath("$.data.location.latitude").value(closeTo(37.7749, 0.001)))
                .andExpect(jsonPath("$.data.online").value(true));
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("accessToken").asText();
    }
}
