package com.nivya.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.network.dto.NetworkTelemetryRequest;
import com.nivya.network.entity.NetworkHistory;
import com.nivya.network.entity.NetworkStatus;
import com.nivya.network.repository.NetworkHistoryRepository;
import com.nivya.network.repository.NetworkStatusRepository;
import com.nivya.role.RoleType;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NetworkIntegrationTest {

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
    private NetworkStatusRepository networkStatusRepository;

    @Autowired
    private NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRepository alertRepository;

    @Autowired
    private com.nivya.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.nivya.location.repository.LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private com.nivya.location.repository.LocationStatusRepository locationStatusRepository;

    @Autowired
    private com.nivya.consent.repository.ConsentRepository consentRepository;

    @Autowired
    private com.nivya.alerts.repository.NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRuleRepository alertRuleRepository;

    @Autowired
    private com.nivya.activity.repository.ActivityEventRepository activityEventRepository;

    @Autowired
    private com.nivya.history.repository.HistoryEventRepository historyEventRepository;

    private User parentUser;
    private User childUser;
    private Family family;
    private Device childDevice;
    private String childToken;
    private String parentToken;

    @BeforeEach
    void setUp() throws Exception {
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
        networkHistoryRepository.deleteAll();
        networkStatusRepository.deleteAll();

        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRuleRepository.deleteAll();
        alertRepository.deleteAll();
        activityEventRepository.deleteAll();
        historyEventRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        consentRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();


        // 1. Register Parent
        RegisterRequest parentReq = new RegisterRequest("David Parent", "david.parent@nivya.local", "SecurePass123!", RoleType.PARENT);
        MvcResult parentRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode parentJson = objectMapper.readTree(parentRes.getResponse().getContentAsString());
        parentToken = parentJson.get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("david.parent@nivya.local").orElseThrow();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest("Emma Child", "emma.child@nivya.local", "SecurePass123!", RoleType.CHILD);
        MvcResult childRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode childJson = objectMapper.readTree(childRes.getResponse().getContentAsString());
        childToken = childJson.get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("emma.child@nivya.local").orElseThrow();

        // 3. Create Family & Link Members
        family = new Family("David's Family", parentUser);
        family = familyRepository.save(family);

        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 4. Create Child Device
        childDevice = new Device(childUser, family, "child-net-uuid-1", "Emma's Phone", "ANDROID");
        childDevice = deviceRepository.save(childDevice);

        DeviceStatus ds = new DeviceStatus(childDevice, false, 80, "NONE", "UNAVAILABLE");
        deviceStatusRepository.save(ds);
    }

    @Test
    @DisplayName("Child records network telemetry: status persisted, history logged, and deviceStatus synced")
    void testRecordNetworkTelemetry_Success() throws Exception {
        NetworkTelemetryRequest req = new NetworkTelemetryRequest(
                childDevice.getDeviceUuid(),
                "WIFI",
                "Wi-Fi (5GHz)",
                true,
                true,
                4,
                -52,
                "EXCELLENT"
        );
        req.setSsid("Home-Wi-Fi-5G");
        req.setIpAddress("192.168.1.105");

        mockMvc.perform(post("/api/v1/network/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.networkType").value("WIFI"))
                .andExpect(jsonPath("$.data.quality").value("EXCELLENT"))
                .andExpect(jsonPath("$.data.isInternetAvailable").value(true));

        // Verify NetworkStatus table
        NetworkStatus status = networkStatusRepository.findByDeviceId(childDevice.getId()).orElse(null);
        assertNotNull(status);
        assertEquals("WIFI", status.getNetworkType());
        assertEquals("EXCELLENT", status.getQuality());
        assertTrue(status.isInternetAvailable());
        assertEquals(-52, status.getSignalDbm());

        // Verify DeviceStatus synchronized
        DeviceStatus ds = deviceStatusRepository.findByDeviceId(childDevice.getId()).orElse(null);
        assertNotNull(ds);
        assertTrue(ds.isOnline());
        assertEquals("WIFI", ds.getNetworkType());
        assertEquals("EXCELLENT", ds.getNetworkQuality());

        // Verify NetworkHistory entry created
        List<NetworkHistory> history = networkHistoryRepository.findTop50ByDeviceIdOrderByRecordedAtDesc(childDevice.getId());
        assertEquals(1, history.size());
        assertEquals("WIFI", history.get(0).getNetworkType());
    }

    @Test
    @DisplayName("Parent retrieves current network status for linked child device")
    void testGetCurrentNetwork_Success() throws Exception {
        NetworkStatus status = new NetworkStatus(
                childDevice, "CELLULAR", "5G Sub-6", true, true, 3, -75, "GOOD", null, "10.0.0.1"
        );
        networkStatusRepository.save(status);

        mockMvc.perform(get("/api/v1/network/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.networkType").value("CELLULAR"))
                .andExpect(jsonPath("$.data.connectionType").value("5G Sub-6"))
                .andExpect(jsonPath("$.data.quality").value("GOOD"));
    }

    @Test
    @DisplayName("Parent retrieves chronological network history")
    void testGetNetworkHistory_Success() throws Exception {
        NetworkTelemetryRequest req1 = new NetworkTelemetryRequest(
                childDevice.getDeviceUuid(), "WIFI", "Wi-Fi (2.4GHz)", true, true, 4, -50, "EXCELLENT"
        );
        NetworkTelemetryRequest req2 = new NetworkTelemetryRequest(
                childDevice.getDeviceUuid(), "CELLULAR", "LTE", true, true, 2, -90, "WEAK"
        );

        mockMvc.perform(post("/api/v1/network/telemetry")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)));

        mockMvc.perform(post("/api/v1/network/telemetry")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)));

        mockMvc.perform(get("/api/v1/network/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.points.length()").value(2));
    }

    @Test
    @DisplayName("User from another family is denied access to child network telemetry")
    void testUnauthorizedAccess_OtherFamily() throws Exception {
        RegisterRequest strangerReq = new RegisterRequest("Stranger", "stranger@nivya.local", "SecurePass123!", RoleType.PARENT);
        MvcResult strangerRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strangerReq)))
                .andReturn();
        String strangerToken = objectMapper.readTree(strangerRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/network/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Offline network telemetry correctly sets isOnline=false and quality=UNAVAILABLE")
    void testOfflineNetworkTelemetry() throws Exception {
        NetworkTelemetryRequest offlineReq = new NetworkTelemetryRequest(
                childDevice.getDeviceUuid(),
                "NONE",
                "Offline",
                false,
                false,
                0,
                null,
                "UNAVAILABLE"
        );

        mockMvc.perform(post("/api/v1/network/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quality").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.isInternetAvailable").value(false));

        DeviceStatus ds = deviceStatusRepository.findByDeviceId(childDevice.getId()).orElse(null);
        assertNotNull(ds);
        assertFalse(ds.isOnline());
        assertEquals("UNAVAILABLE", ds.getNetworkQuality());
    }
}
