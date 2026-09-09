package com.nivya.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.consent.entity.Consent;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.location.dto.LocationTelemetryRequest;
import com.nivya.location.entity.LocationHistory;
import com.nivya.location.entity.LocationStatus;
import com.nivya.location.repository.LocationHistoryRepository;
import com.nivya.location.repository.LocationStatusRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationIntegrationTest {

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
    private LocationStatusRepository locationStatusRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private com.nivya.usage.repository.UsageAppRepository usageAppRepository;

    @Autowired
    private com.nivya.usage.repository.UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private com.nivya.network.repository.NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private com.nivya.network.repository.NetworkStatusRepository networkStatusRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRepository alertRepository;

    @Autowired
    private com.nivya.alerts.repository.NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRuleRepository alertRuleRepository;

    @Autowired
    private com.nivya.activity.repository.ActivityEventRepository activityEventRepository;

    @Autowired
    private com.nivya.history.repository.HistoryEventRepository historyEventRepository;

    @Autowired
    private com.nivya.auth.repository.RefreshTokenRepository refreshTokenRepository;

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
        usageAppRepository.deleteAll();
        usageSummaryRepository.deleteAll();
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
        RegisterRequest parentReq = new RegisterRequest("Location Parent", "parent.loc@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode pNode = objectMapper.readTree(pRes.getResponse().getContentAsString());
        parentToken = pNode.get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.loc@nivya.local").orElseThrow();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest("Location Child", "child.loc@nivya.local", "Password123!", RoleType.CHILD);
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode cNode = objectMapper.readTree(cRes.getResponse().getContentAsString());
        childToken = cNode.get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.loc@nivya.local").orElseThrow();

        // 3. Setup Family Relationship
        family = new Family("Location Test Family", parentUser);
        family = familyRepository.save(family);

        FamilyMember fmParent = new FamilyMember(family, parentUser, RoleType.PARENT);
        FamilyMember fmChild = new FamilyMember(family, childUser, RoleType.CHILD);
        familyMemberRepository.saveAll(List.of(fmParent, fmChild));

        // 4. Setup Child Device & DeviceStatus
        childDevice = new Device(childUser, family, "child-loc-uuid-1", "Child Pixel 8", "ANDROID");
        childDevice = deviceRepository.save(childDevice);

        DeviceStatus devStatus = new DeviceStatus(childDevice, true, 90, "WIFI", "EXCELLENT");
        deviceStatusRepository.save(devStatus);


        // 5. Setup Consent (location_consent = true)
        Consent consent = new Consent(childUser, family, "1.0", true, true);
        consentRepository.save(consent);
    }

    @Test
    @DisplayName("Submit Location Telemetry and verify status upsert")
    void testRecordLocationTelemetry() throws Exception {
        LocationTelemetryRequest request = new LocationTelemetryRequest(
                "child-loc-uuid-1",
                37.7749,
                -122.4194,
                12.5f,
                15.0,
                1.2f,
                180.0f,
                "gps",
                true,
                true,
                "GRANTED",
                true,
                "FOREGROUND",
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/location/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(37.7749))
                .andExpect(jsonPath("$.data.longitude").value(-122.4194))
                .andExpect(jsonPath("$.data.provider").value("gps"))
                .andExpect(jsonPath("$.data.gpsAvailable").value(true))
                .andExpect(jsonPath("$.data.stale").value(false));

        LocationStatus status = locationStatusRepository.findByDeviceId(childDevice.getId()).orElse(null);
        assertNotNull(status);
        assertEquals(37.7749, status.getLatitude(), 0.0001);
        assertEquals(-122.4194, status.getLongitude(), 0.0001);

        List<LocationHistory> history = locationHistoryRepository.findAll();
        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("Verify stationary noise suppression avoids duplicate history points")
    void testStationaryNoiseSuppression() throws Exception {
        LocationTelemetryRequest req1 = new LocationTelemetryRequest(
                "child-loc-uuid-1",
                37.7749,
                -122.4194,
                10.0f,
                null,
                0f,
                0f,
                "gps",
                true,
                true,
                "GRANTED",
                false,
                "FOREGROUND",
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/location/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // Same location 10 seconds later (stationary)
        LocationTelemetryRequest req2 = new LocationTelemetryRequest(
                "child-loc-uuid-1",
                37.7749001,
                -122.4194001,
                10.0f,
                null,
                0f,
                0f,
                "gps",
                true,
                true,
                "GRANTED",
                false,
                "FOREGROUND",
                Instant.now().plusSeconds(10)
        );

        mockMvc.perform(post("/api/v1/location/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        // Should still only have 1 history record due to stationary suppression
        List<LocationHistory> history = locationHistoryRepository.findAll();
        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("Parent retrieves current Child location")
    void testParentGetCurrentLocation() throws Exception {
        LocationStatus status = new LocationStatus(
                childDevice,
                40.7128,
                -74.0060,
                8.0f,
                10.0,
                0.5f,
                90.0f,
                "gps",
                true,
                true,
                "GRANTED",
                true,
                false,
                Instant.now()
        );
        locationStatusRepository.save(status);

        mockMvc.perform(get("/api/v1/location/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(40.7128))
                .andExpect(jsonPath("$.data.longitude").value(-74.0060))
                .andExpect(jsonPath("$.data.deviceName").value("Child Pixel 8"));
    }

    @Test
    @DisplayName("Parent queries location history when consent is granted vs revoked")
    void testLocationHistoryConsentGating() throws Exception {
        LocationHistory p1 = new LocationHistory(
                childDevice, 40.7128, -74.0060, 10f, null, 0f, "gps", "FOREGROUND", Instant.now().minus(Duration.ofMinutes(10))
        );
        LocationHistory p2 = new LocationHistory(
                childDevice, 40.7150, -74.0080, 12f, null, 1.5f, "gps", "FOREGROUND", Instant.now().minus(Duration.ofMinutes(5))
        );
        locationHistoryRepository.saveAll(List.of(p1, p2));

        // 1. With consent granted
        mockMvc.perform(get("/api/v1/location/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consentGranted").value(true))
                .andExpect(jsonPath("$.data.totalPoints").value(2));

        // 2. Revoke consent
        List<Consent> consents = consentRepository.findByUserId(childUser.getId());
        for (Consent c : consents) {
            c.setLocationConsent(false);
            consentRepository.save(c);
        }

        mockMvc.perform(get("/api/v1/location/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consentGranted").value(false))
                .andExpect(jsonPath("$.data.totalPoints").value(0));
    }

    @Test
    @DisplayName("Unauthorized family member access is rejected with 403 Forbidden")
    void testUnauthorizedFamilyAccessForbidden() throws Exception {
        RegisterRequest strangerReq = new RegisterRequest("Stranger Parent", "stranger.loc@nivya.local", "Password123!", RoleType.PARENT);


        MvcResult sRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strangerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String strangerToken = objectMapper.readTree(sRes.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/location/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Purge old location history records according to retention policy")
    void testPurgeOldLocations() throws Exception {
        LocationHistory oldPoint = new LocationHistory(
                childDevice, 40.0, -74.0, 10f, null, 0f, "gps", "FOREGROUND", Instant.now().minus(Duration.ofDays(35))
        );
        LocationHistory recentPoint = new LocationHistory(
                childDevice, 40.1, -74.1, 10f, null, 0f, "gps", "FOREGROUND", Instant.now().minus(Duration.ofDays(5))
        );
        locationHistoryRepository.saveAll(List.of(oldPoint, recentPoint));

        assertEquals(2, locationHistoryRepository.count());

        mockMvc.perform(post("/api/v1/location/purge?retentionDays=30")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        assertEquals(1, locationHistoryRepository.count());
    }
}
