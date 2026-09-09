package com.nivya.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.alerts.dto.TriggerAlertRequest;
import com.nivya.alerts.dto.UpdateAlertRuleRequest;
import com.nivya.alerts.entity.Alert;
import com.nivya.alerts.entity.AlertRule;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.AlertRuleRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.alerts.service.AlertService;
import com.nivya.auth.dto.RegisterRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertIntegrationTest {

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
    private AlertRepository alertRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private NetworkStatusRepository networkStatusRepository;

    @Autowired
    private NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private LocationStatusRepository locationStatusRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private UsageAppRepository usageAppRepository;

    @Autowired
    private com.nivya.activity.repository.ActivityEventRepository activityEventRepository;

    @Autowired
    private com.nivya.history.repository.HistoryEventRepository historyEventRepository;

    @Autowired
    private DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User parentUser;
    private User childUser;
    private User otherParentUser;
    private Family family;
    private Device childDevice;
    private String parentToken;
    private String childToken;
    private String otherParentToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent User
        RegisterRequest pReq = new RegisterRequest("Alert Parent", "parent.alert@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pReq)))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.alert@nivya.local").orElseThrow();

        // 2. Register Child User
        RegisterRequest cReq = new RegisterRequest("Alert Child", "child.alert@nivya.local", "Password123!", RoleType.CHILD);
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cReq)))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.alert@nivya.local").orElseThrow();

        // 3. Register Unrelated Parent
        RegisterRequest oReq = new RegisterRequest("Other Parent", "other.alert@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult oRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oReq)))
                .andExpect(status().isCreated())
                .andReturn();
        otherParentToken = objectMapper.readTree(oRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        otherParentUser = userRepository.findByEmail("other.alert@nivya.local").orElseThrow();

        // 4. Create Family
        family = new Family("Alert Family", parentUser);
        family = familyRepository.save(family);

        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 5. Create Child Device
        childDevice = new Device(childUser, family, "dev-alert-child-101", "Pixel 8", "ANDROID");
        childDevice = deviceRepository.save(childDevice);
        deviceStatusRepository.save(new DeviceStatus(childDevice, true, 80, "WIFI", "EXCELLENT"));
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
        historyEventRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        consentRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Parent can view all family alerts and filter by severity or unread status")
    void testParentCanViewFamilyAlerts() throws Exception {
        // Trigger LOW_BATTERY alert (ALL) and OFFLINE alert (PARENT)
        alertService.triggerOrUpdateAlert(family, childDevice, "LOW_BATTERY", "WARNING",
                "Low Battery Alert", "Child battery is at 14%", "ALL");
        alertService.triggerOrUpdateAlert(family, childDevice, "OFFLINE", "CRITICAL",
                "Device Offline", "Child device is offline", "PARENT");

        // 1. Parent retrieves all alerts
        mockMvc.perform(get("/api/v1/alerts/family/" + family.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        // 2. Parent retrieves with severity=CRITICAL filter
        mockMvc.perform(get("/api/v1/alerts/family/" + family.getId())
                        .param("severity", "CRITICAL")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].alertType").value("OFFLINE"));

        // 3. Unrelated parent gets 403 Forbidden
        mockMvc.perform(get("/api/v1/alerts/family/" + family.getId())
                        .header("Authorization", "Bearer " + otherParentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Child can ONLY view alerts intended for Child, and never parent-only alerts")
    void testChildCanOnlyViewChildTargetedAlerts() throws Exception {
        // Create a Parent-only alert and an ALL alert
        alertService.triggerOrUpdateAlert(family, childDevice, "SECURITY_ALERT", "CRITICAL",
                "Security Warning", "Unauthorized login attempt", "PARENT");
        alertService.triggerOrUpdateAlert(family, childDevice, "LOW_BATTERY", "WARNING",
                "Charge Device", "Battery at 15%. Plug in soon.", "ALL");

        // Child queries /api/v1/alerts/my
        mockMvc.perform(get("/api/v1/alerts/my")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].alertType").value("LOW_BATTERY"))
                .andExpect(jsonPath("$.data[0].title").value("Charge Device"));
    }

    @Test
    @DisplayName("Child UI controls guardrail: Child cannot access Parent endpoints")
    void testChildForbiddenFromParentEndpoints() throws Exception {
        // 1. Child tries to view family alerts -> 403
        mockMvc.perform(get("/api/v1/alerts/family/" + family.getId())
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());

        // 2. Child tries to view alert rules -> 403
        mockMvc.perform(get("/api/v1/alerts/rules/" + family.getId())
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());

        // 3. Child tries to resolve alert -> 403
        Alert alert = alertRepository.save(new Alert(family, childDevice, "LOW_BATTERY", "WARNING", "Low Battery", "12%"));
        mockMvc.perform(post("/api/v1/alerts/" + alert.getId() + "/resolve")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Parent can resolve an alert incident successfully")
    void testParentCanResolveAlert() throws Exception {
        Alert alert = alertRepository.save(new Alert(family, childDevice, "LOW_STORAGE", "WARNING",
                "Storage Low", "Less than 10% space remaining", "ALL"));
        assertThat(alert.isResolved()).isFalse();

        mockMvc.perform(post("/api/v1/alerts/" + alert.getId() + "/resolve")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resolved").value(true))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        Alert updated = alertRepository.findById(alert.getId()).orElseThrow();
        assertThat(updated.isResolved()).isTrue();
        assertThat(updated.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("User can mark alert as read and unread count decrements")
    void testMarkAlertAsReadAndUnreadCount() throws Exception {
        Alert alert = alertRepository.save(new Alert(family, childDevice, "LOW_BATTERY", "WARNING",
                "Low Battery", "10%", "ALL"));
        assertThat(alert.isRead()).isFalse();

        // 1. Check unread count is 1
        mockMvc.perform(get("/api/v1/alerts/unread-count")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 2. Mark as read
        mockMvc.perform(post("/api/v1/alerts/" + alert.getId() + "/read")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        // 3. Check unread count is now 0
        mockMvc.perform(get("/api/v1/alerts/unread-count")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("Alert Rules: Parent can view default rules and update threshold/toggle")
    void testAlertRulesLifecycle() throws Exception {
        // 1. Initial get rules seeds default rules
        MvcResult res = mockMvc.perform(get("/api/v1/alerts/rules/" + family.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andReturn();

        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        Long ruleId = node.get("data").get(0).get("id").asLong();

        // 2. Parent updates rule
        UpdateAlertRuleRequest updateReq = new UpdateAlertRuleRequest("20", "CRITICAL", false);
        mockMvc.perform(put("/api/v1/alerts/rules/" + ruleId)
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thresholdValue").value("20"))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        AlertRule updatedRule = alertRuleRepository.findById(ruleId).orElseThrow();
        assertThat(updatedRule.getThresholdValue()).isEqualTo("20");
        assertThat(updatedRule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Trigger alert creates NotificationRecords and avoids duplicate active alerts")
    void testTriggerAlertAndSuppression() throws Exception {
        TriggerAlertRequest triggerReq = new TriggerAlertRequest(
                childDevice.getDeviceUuid(),
                "SECURITY_ALERT",
                "CRITICAL",
                "Unauthorized Pairing Attempt",
                "Unknown device attempted pairing code connection",
                "PARENT"
        );

        // 1. Trigger alert
        mockMvc.perform(post("/api/v1/alerts/trigger")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alertType").value("SECURITY_ALERT"))
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"));

        assertThat(alertRepository.count()).isEqualTo(1);
        assertThat(notificationRecordRepository.count()).isGreaterThanOrEqualTo(1);

        // 2. Trigger again for same type while unresolved -> Suppresses duplicate, updates message
        triggerReq.setMessage("Second attempt detected");
        mockMvc.perform(post("/api/v1/alerts/trigger")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Second attempt detected"));

        // Row count remains 1
        assertThat(alertRepository.count()).isEqualTo(1);
    }
}
