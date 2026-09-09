package com.nivya.usage;

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
import com.nivya.role.RoleType;
import com.nivya.usage.dto.UsageTelemetryRequest;
import com.nivya.usage.entity.UsageSummary;
import com.nivya.usage.repository.UsageAppRepository;
import com.nivya.usage.repository.UsageSummaryRepository;
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
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsageIntegrationTest {

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
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsageAppRepository usageAppRepository;

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
        RegisterRequest parentReq = new RegisterRequest("Sarah Parent", "sarah.usage@nivya.local", "SecurePass123!", RoleType.PARENT);
        MvcResult parentRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode parentJson = objectMapper.readTree(parentRes.getResponse().getContentAsString());
        parentToken = parentJson.get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("sarah.usage@nivya.local").orElseThrow();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest("Liam Child", "liam.usage@nivya.local", "SecurePass123!", RoleType.CHILD);
        MvcResult childRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode childJson = objectMapper.readTree(childRes.getResponse().getContentAsString());
        childToken = childJson.get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("liam.usage@nivya.local").orElseThrow();

        // 3. Create Family & Link Members
        family = new Family("Sarah's Family", parentUser);
        family = familyRepository.save(family);

        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 4. Create Child Device
        childDevice = new Device(childUser, family, "child-usage-uuid-1", "Liam's Pixel", "ANDROID");
        childDevice = deviceRepository.save(childDevice);

        DeviceStatus ds = new DeviceStatus(childDevice, true, 90, "WIFI", "EXCELLENT");
        deviceStatusRepository.save(ds);
    }

    @Test
    @DisplayName("Child submits screen time telemetry with app breakdown: daily summary and category aggregates computed")
    void testRecordUsage_Success() throws Exception {
        UsageTelemetryRequest.AppUsageItemDto app1 = new UsageTelemetryRequest.AppUsageItemDto(
                "com.duolingo", "Duolingo", "EDUCATION", 2700L, Instant.now() // 45m
        );
        UsageTelemetryRequest.AppUsageItemDto app2 = new UsageTelemetryRequest.AppUsageItemDto(
                "com.mojang.minecraftpe", "Minecraft", "GAMES", 2400L, Instant.now() // 40m
        );
        UsageTelemetryRequest.AppUsageItemDto app3 = new UsageTelemetryRequest.AppUsageItemDto(
                "com.google.android.apps.messaging", "Messages", "SOCIAL", 900L, Instant.now() // 15m
        );

        UsageTelemetryRequest req = new UsageTelemetryRequest(
                childDevice.getDeviceUuid(),
                LocalDate.now(),
                6000L, // 1h 40m
                15,
                Arrays.asList(app1, app2, app3)
        );

        mockMvc.perform(post("/api/v1/usage/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalForegroundSeconds").value(6000))
                .andExpect(jsonPath("$.data.educationalSeconds").value(2700))
                .andExpect(jsonPath("$.data.recreationalSeconds").value(2400))
                .andExpect(jsonPath("$.data.socialSeconds").value(900));

        // Verify database persistence
        UsageSummary summary = usageSummaryRepository.findByDeviceIdAndDate(childDevice.getId(), LocalDate.now()).orElse(null);
        assertNotNull(summary);
        assertEquals(6000L, summary.getTotalForegroundSeconds());
        assertEquals(2700L, summary.getEducationalSeconds());
        assertEquals(2400L, summary.getRecreationalSeconds());
        assertEquals(900L, summary.getSocialSeconds());
        assertEquals(15, summary.getScreenUnlocks());
    }

    @Test
    @DisplayName("Parent retrieves daily screen time summary")
    void testGetDailySummary_Success() throws Exception {
        UsageSummary summary = new UsageSummary(
                childDevice, LocalDate.now(), 7200L, 18, 3600L, 2400L, 1200L, 0L
        );
        usageSummaryRepository.save(summary);

        mockMvc.perform(get("/api/v1/usage/summary/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalForegroundSeconds").value(7200))
                .andExpect(jsonPath("$.data.educationalSeconds").value(3600))
                .andExpect(jsonPath("$.data.formattedTotalTime").value("2h"));
    }

    @Test
    @DisplayName("Parent retrieves detailed ranked application usage breakdown")
    void testGetAppUsage_Success() throws Exception {
        UsageTelemetryRequest.AppUsageItemDto app1 = new UsageTelemetryRequest.AppUsageItemDto(
                "com.duolingo", "Duolingo", "EDUCATION", 3600L, Instant.now()
        );
        UsageTelemetryRequest.AppUsageItemDto app2 = new UsageTelemetryRequest.AppUsageItemDto(
                "com.khanacademy", "Khan Academy", "EDUCATION", 1800L, Instant.now()
        );

        UsageTelemetryRequest req = new UsageTelemetryRequest(
                childDevice.getDeviceUuid(),
                LocalDate.now(),
                5400L,
                10,
                Arrays.asList(app1, app2)
        );

        mockMvc.perform(post("/api/v1/usage/telemetry")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/v1/usage/apps/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apps.length()").value(2))
                .andExpect(jsonPath("$.data.apps[0].appName").value("Duolingo"))
                .andExpect(jsonPath("$.data.apps[0].percentageOfTotal").value(0.67))
                .andExpect(jsonPath("$.data.apps[1].appName").value("Khan Academy"))
                .andExpect(jsonPath("$.data.apps[1].percentageOfTotal").value(0.33));
    }

    @Test
    @DisplayName("Parent retrieves weekly usage trends and percentage comparison")
    void testGetUsageTrends_Success() throws Exception {
        LocalDate today = LocalDate.now();
        // Today
        usageSummaryRepository.save(new UsageSummary(childDevice, today, 7200L, 10, 3600L, 2400L, 1200L, 0L));
        // Yesterday
        usageSummaryRepository.save(new UsageSummary(childDevice, today.minusDays(1), 5400L, 8, 2700L, 1800L, 900L, 0L));

        mockMvc.perform(get("/api/v1/usage/trends/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dailyPoints.length()").value(2))
                .andExpect(jsonPath("$.data.currentWeekTotalSeconds").value(12600));
    }

    @Test
    @DisplayName("User from another family is forbidden from viewing usage telemetry")
    void testUnauthorizedAccess_OtherFamily() throws Exception {
        RegisterRequest strangerReq = new RegisterRequest("Stranger User", "stranger.usage@nivya.local", "SecurePass123!", RoleType.PARENT);
        MvcResult strangerRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strangerReq)))
                .andReturn();
        String strangerToken = objectMapper.readTree(strangerRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/usage/summary/" + childDevice.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }
}
