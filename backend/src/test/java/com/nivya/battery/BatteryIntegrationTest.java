package com.nivya.battery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.alerts.entity.Alert;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.battery.dto.BatteryTelemetryRequest;
import com.nivya.battery.entity.BatteryHistory;
import com.nivya.battery.entity.BatteryStatus;
import com.nivya.battery.repository.BatteryHistoryRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatteryIntegrationTest {

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
    private BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private AlertRepository alertRepository;

    private User parentUser;
    private User childUser;
    private Family family;
    private Device childDevice;
    private String childToken;
    private String parentToken;

    @Autowired
    private DeviceStatusRepository deviceStatusRepository;

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

    @BeforeEach
    void setUp() throws Exception {
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
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
        RegisterRequest parentReq = new RegisterRequest("Parent User", "parent.bat@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult parentRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = extractToken(parentRes);
        parentUser = userRepository.findByEmail("parent.bat@nivya.local").orElseThrow();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest("Child User", "child.bat@nivya.local", "Password123!", RoleType.CHILD);
        MvcResult childRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = extractToken(childRes);
        childUser = userRepository.findByEmail("child.bat@nivya.local").orElseThrow();

        // 3. Create Linked Family
        family = new Family("Battery Family", parentUser);
        familyRepository.save(family);
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 4. Enroll Child Device
        childDevice = new Device(childUser, family, "device-uuid-child-bat-01", "Alex's Tablet", "ANDROID");
        deviceRepository.save(childDevice);
    }

    @Test
    @DisplayName("Record telemetry updates battery_status and appends history")
    void testRecordBatteryTelemetry() throws Exception {
        BatteryTelemetryRequest req = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(),
                85,
                "DISCHARGING",
                "UNPLUGGED",
                "GOOD",
                31.5,
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryPct").value(85))
                .andExpect(jsonPath("$.data.chargingState").value("DISCHARGING"))
                .andExpect(jsonPath("$.data.temperatureCelsius").value(31.5))
                .andExpect(jsonPath("$.data.lowBattery").value(false));

        BatteryStatus status = batteryStatusRepository.findByDeviceId(childDevice.getId()).orElseThrow();
        assertEquals(85, status.getBatteryPct());
        assertEquals("DISCHARGING", status.getChargingState());

        List<BatteryHistory> history = batteryHistoryRepository.findTop50ByDeviceIdOrderByRecordedAtDesc(childDevice.getId());
        assertEquals(1, history.size());
        assertEquals(85, history.get(0).getBatteryPct());
    }

    @Test
    @DisplayName("Telemetry with battery <= 20% triggers LOW_BATTERY safety alert")
    void testLowBatteryAlertTriggering() throws Exception {
        BatteryTelemetryRequest lowReq = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(),
                15,
                "DISCHARGING",
                "UNPLUGGED",
                "GOOD",
                30.0,
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowBattery").value(true));

        List<Alert> alerts = alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(family.getId());
        assertEquals(1, alerts.size());
        assertEquals("LOW_BATTERY", alerts.get(0).getAlertType());
        assertEquals("WARNING", alerts.get(0).getSeverity());
    }

    @Test
    @DisplayName("Recharging device resolves previous low battery alert")
    void testRechargingResolvesAlert() throws Exception {
        // First trigger low battery
        BatteryTelemetryRequest lowReq = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(), 12, "DISCHARGING", "UNPLUGGED", "GOOD", 30.0, Instant.now()
        );
        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowReq)))
                .andExpect(status().isOk());

        assertEquals(1, alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(family.getId()).size());

        // Now connect charger
        BatteryTelemetryRequest chargingReq = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(), 14, "CHARGING", "AC", "GOOD", 31.0, Instant.now()
        );
        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargingReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lowBattery").value(false));

        // Alert should now be resolved
        List<Alert> unresolved = alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(family.getId());
        assertEquals(0, unresolved.size());
    }

    @Test
    @DisplayName("Parent can query current battery, history, and calculated trends")
    void testParentBatteryQueries() throws Exception {
        // Record two historical points
        Instant now = Instant.now();
        mockMvc.perform(post("/api/v1/battery/telemetry")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BatteryTelemetryRequest(
                        childDevice.getDeviceUuid(), 80, "DISCHARGING", "UNPLUGGED", "GOOD", 29.0, now.minusSeconds(3600)
                ))));

        mockMvc.perform(post("/api/v1/battery/telemetry")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BatteryTelemetryRequest(
                        childDevice.getDeviceUuid(), 72, "DISCHARGING", "UNPLUGGED", "GOOD", 30.0, now
                ))));

        // 1. Current battery query by Parent
        mockMvc.perform(get("/api/v1/battery/current/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryPct").value(72))
                .andExpect(jsonPath("$.data.deviceName").value("Alex's Tablet"));

        // 2. Battery history query by Parent
        mockMvc.perform(get("/api/v1/battery/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points.length()").value(2));

        // 3. Battery trends query by Parent
        mockMvc.perform(get("/api/v1/battery/trends/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBatteryPct").value(72))
                .andExpect(jsonPath("$.data.drainRatePctPerHour").value(8.0));
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
