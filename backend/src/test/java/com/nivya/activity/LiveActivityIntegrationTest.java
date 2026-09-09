package com.nivya.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.activity.dto.LiveActivityRequest;
import com.nivya.activity.entity.ActivityEvent;
import com.nivya.activity.repository.ActivityEventRepository;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.AlertRuleRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.auth.entity.RefreshToken;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.battery.repository.BatteryHistoryRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.entity.Device;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveActivityIntegrationTest {

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



    private User parentUser;
    private User childUser;
    private User outsiderParentUser;
    private Family family;
    private Device childDevice;
    private String parentToken;
    private String childToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent User
        org.springframework.test.web.servlet.MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.nivya.auth.dto.RegisterRequest("Parent Activity", "activityparent@example.com", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("activityparent@example.com").orElseThrow();

        // 2. Register Child User
        org.springframework.test.web.servlet.MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.nivya.auth.dto.RegisterRequest("Child Activity", "activitychild@example.com", "Password@123", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("activitychild@example.com").orElseThrow();

        // 3. Register Outsider Parent
        org.springframework.test.web.servlet.MvcResult oRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.nivya.auth.dto.RegisterRequest("Outsider Parent", "outsider@example.com", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        outsiderToken = objectMapper.readTree(oRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        outsiderParentUser = userRepository.findByEmail("outsider@example.com").orElseThrow();

        // 4. Create Family and link members
        family = new Family("Activity Family", parentUser);
        family = familyRepository.save(family);
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 5. Create Child Device
        childDevice = new Device(childUser, family, "dev-child-act-1", "Samsung Galaxy A54", "ANDROID");
        childDevice = deviceRepository.save(childDevice);
        deviceStatusRepository.save(new DeviceStatus(childDevice, true, 85, "WIFI", "EXCELLENT"));
    }

    @AfterEach
    void tearDown() {
        cleanDatabases();
    }

    private void cleanDatabases() {
        notificationRecordRepository.deleteAll();
        alertRuleRepository.deleteAll();
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
        activityEventRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        consentRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Submit activity telemetry successfully from child device")
    void recordActivity_asChildDevice_success() throws Exception {
        LiveActivityRequest request = new LiveActivityRequest(
                childDevice.getDeviceUuid(),
                "com.whatsapp",
                "WhatsApp",
                "Chatting with Arun",
                "COMMUNICATION",
                120,
                true,
                Instant.now().minus(2, ChronoUnit.MINUTES),
                null
        );

        mockMvc.perform(post("/api/v1/activity/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.packageName").value("com.whatsapp"))
                .andExpect(jsonPath("$.data.appName").value("WhatsApp"))
                .andExpect(jsonPath("$.data.broadActivity").value("Chatting with Arun"))
                .andExpect(jsonPath("$.data.category").value("COMMUNICATION"))
                .andExpect(jsonPath("$.data.current").value(true));
    }

    @Test
    @DisplayName("Parent retrieves live activity and chronological timeline")
    void getLiveActivity_asParent_success() throws Exception {
        // Record initial event
        ActivityEvent event = new ActivityEvent(
                childDevice,
                "com.google.android.youtube",
                "YouTube",
                "Watching",
                "ENTERTAINMENT",
                600,
                true,
                Instant.now().minus(10, ChronoUnit.MINUTES),
                null
        );
        activityEventRepository.save(event);

        mockMvc.perform(get("/api/v1/activity/live/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value(childDevice.getId()))
                .andExpect(jsonPath("$.data.deviceName").value("Samsung Galaxy A54"))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.currentActivity.appName").value("YouTube"))
                .andExpect(jsonPath("$.data.currentActivity.broadActivity").value("Watching"))
                .andExpect(jsonPath("$.data.currentActivity.category").value("ENTERTAINMENT"))
                .andExpect(jsonPath("$.data.recentActivities", hasSize(1)));
    }

    @Test
    @DisplayName("Child user is strictly prohibited from accessing Parent-only Live Activity (403 Forbidden)")
    void getLiveActivity_asChild_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/activity/live/" + childDevice.getId())
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unrelated parent from different family is forbidden (403)")
    void getLiveActivity_asOutsiderParent_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/activity/live/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Transitioning apps closes previous activity and adds to chronological history")
    void recordActivity_transitionBetweenApps_closesPreviousActivity() throws Exception {
        // 1. WhatsApp activity
        LiveActivityRequest waReq = new LiveActivityRequest(
                childDevice.getDeviceUuid(),
                "com.whatsapp",
                "WhatsApp",
                "Chatting with Arun",
                "COMMUNICATION",
                180,
                true,
                Instant.now().minus(5, ChronoUnit.MINUTES),
                null
        );

        mockMvc.perform(post("/api/v1/activity/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waReq)))
                .andExpect(status().isOk());

        // 2. Transition to Chrome
        LiveActivityRequest chromeReq = new LiveActivityRequest(
                childDevice.getDeviceUuid(),
                "com.android.chrome",
                "Chrome",
                "Browsing",
                "BROWSING",
                60,
                true,
                Instant.now().minus(1, ChronoUnit.MINUTES),
                null
        );

        mockMvc.perform(post("/api/v1/activity/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chromeReq)))
                .andExpect(status().isOk());

        // 3. Parent queries live activity
        mockMvc.perform(get("/api/v1/activity/live/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentActivity.appName").value("Chrome"))
                .andExpect(jsonPath("$.data.currentActivity.broadActivity").value("Browsing"))
                .andExpect(jsonPath("$.data.recentActivities", hasSize(2)))
                .andExpect(jsonPath("$.data.recentActivities[0].appName").value("Chrome"))
                .andExpect(jsonPath("$.data.recentActivities[1].appName").value("WhatsApp"));

        // Verify database state: WhatsApp is closed
        ActivityEvent previousWa = activityEventRepository.findAll().stream()
                .filter(e -> e.getPackageName().equals("com.whatsapp"))
                .findFirst()
                .orElseThrow();
        assertFalse(previousWa.isCurrent());
        assertNotNull(previousWa.getEndedAt());
    }

    @Test
    @DisplayName("Fallback defaults when detailed activity is unavailable")
    void recordActivity_fallbackDefaults() throws Exception {
        LiveActivityRequest fallbackReq = new LiveActivityRequest(
                childDevice.getDeviceUuid(),
                "com.google.android.apps.docs",
                "Files",
                "Viewing report.pdf",
                "PRODUCTIVITY",
                null,
                true,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/activity/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fallbackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("Files"))
                .andExpect(jsonPath("$.data.broadActivity").value("Viewing report.pdf"));
    }
}
