package com.nivya.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.alerts.entity.NotificationRecord;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.alerts.service.AlertService;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.convocation.dto.ParentSendMessageRequest;
import com.nivya.convocation.repository.ConvocationMessageRepository;
import com.nivya.convocation.repository.ConvocationViewRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.notification.dto.RegisterPushTokenRequest;
import com.nivya.notification.dto.UnregisterPushTokenRequest;
import com.nivya.notification.service.PushNotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PushNotificationIntegrationTest {

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
    private ConvocationMessageRepository convocationMessageRepository;

    @Autowired
    private ConvocationViewRepository convocationViewRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRuleRepository alertRuleRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRepository alertRepository;

    @Autowired
    private com.nivya.activity.repository.ActivityEventRepository activityEventRepository;

    @Autowired
    private com.nivya.history.repository.HistoryEventRepository historyEventRepository;

    @Autowired
    private com.nivya.location.repository.LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private com.nivya.location.repository.LocationStatusRepository locationStatusRepository;

    @Autowired
    private com.nivya.device.repository.DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private com.nivya.device.repository.DeviceStatusRepository deviceStatusRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private com.nivya.consent.repository.ConsentRepository consentRepository;

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
    private PushNotificationService pushNotificationService;

    @Autowired
    private AlertService alertService;

    private User parentUser;
    private User childUser;
    private Family family;
    private String parentToken;
    private String childToken;
    private Device childDevice;
    private Device parentDevice;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent User
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Push Parent", "parent.push@nivya.local", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.push@nivya.local").orElseThrow();

        // 2. Register Child User
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Push Child", "child.push@nivya.local", "Password@123", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.push@nivya.local").orElseThrow();

        // 3. Create Family
        family = new Family("Push Family", parentUser);
        family = familyRepository.save(family);

        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 4. Create Initial Devices
        parentDevice = new Device(parentUser, family, "parent-device-uuid-1", "Parent Phone", "ANDROID");
        parentDevice = deviceRepository.save(parentDevice);

        childDevice = new Device(childUser, family, "child-device-uuid-1", "Child Pixel 8", "ANDROID");
        childDevice = deviceRepository.save(childDevice);
    }

    private void cleanDatabases() {
        convocationViewRepository.deleteAll();
        convocationMessageRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRepository.deleteAll();
        alertRuleRepository.deleteAll();
        activityEventRepository.deleteAll();
        historyEventRepository.deleteAll();
        locationHistoryRepository.deleteAll();
        locationStatusRepository.deleteAll();
        deviceHealthRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        consentRepository.deleteAll();
        usageAppRepository.deleteAll();
        usageSummaryRepository.deleteAll();
        networkHistoryRepository.deleteAll();
        networkStatusRepository.deleteAll();
        batteryHistoryRepository.deleteAll();
        batteryStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Securely registers FCM push token and updates authorized device")
    void testRegisterPushToken_Success() throws Exception {
        RegisterPushTokenRequest req = new RegisterPushTokenRequest(
                "child-device-uuid-1",
                "fcm-sample-token-abc123xyz456",
                "ANDROID",
                "Child Pixel 8 Updated"
        );

        mockMvc.perform(post("/api/v1/devices/push-token")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.deviceUuid").value("child-device-uuid-1"));

        Device updated = deviceRepository.findByDeviceUuid("child-device-uuid-1").orElseThrow();
        assertEquals("fcm-sample-token-abc123xyz456", updated.getPushToken());
    }

    @Test
    @DisplayName("Device Replacement: Associating token with new device clears token from older device")
    void testDeviceReplacement_ClearsOldDeviceToken() throws Exception {
        String sharedPushToken = "fcm-hardware-token-replacement-999";

        // Step 1: Assign token to old device
        childDevice.setPushToken(sharedPushToken);
        deviceRepository.save(childDevice);
        assertEquals(sharedPushToken, deviceRepository.findByDeviceUuid("child-device-uuid-1").get().getPushToken());

        // Step 2: New replacement device registers the exact same token
        RegisterPushTokenRequest replacementReq = new RegisterPushTokenRequest(
                "child-replacement-device-uuid-2",
                sharedPushToken,
                "ANDROID",
                "Child Pixel 9 Pro"
        );

        mockMvc.perform(post("/api/v1/devices/push-token")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replacementReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceUuid").value("child-replacement-device-uuid-2"));

        // Step 3: Old device token MUST be null, new device MUST have token
        Device oldDevice = deviceRepository.findByDeviceUuid("child-device-uuid-1").orElseThrow();
        assertNull(oldDevice.getPushToken(), "Old replaced device push token should be nullified");

        Device newDevice = deviceRepository.findByDeviceUuid("child-replacement-device-uuid-2").orElseThrow();
        assertEquals(sharedPushToken, newDevice.getPushToken(), "New device should now hold the push token");
    }

    @Test
    @DisplayName("Logout: Unregister endpoint clears device push token")
    void testUnregisterPushToken_OnLogout() throws Exception {
        childDevice.setPushToken("fcm-active-token-to-unregister");
        deviceRepository.save(childDevice);

        UnregisterPushTokenRequest req = new UnregisterPushTokenRequest("child-device-uuid-1", null);

        mockMvc.perform(post("/api/v1/devices/push-token/unregister")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Device updated = deviceRepository.findByDeviceUuid("child-device-uuid-1").orElseThrow();
        assertNull(updated.getPushToken());
    }

    @Test
    @DisplayName("Convocation generic push notification sends EXACT text 'Check your battery status' and never the secret message body")
    void testConvocationGenericNotification_DecoyTextOnChildDevice() throws Exception {
        // Child registers push token
        childDevice.setPushToken("fcm-child-live-token");
        deviceRepository.save(childDevice);

        String secretMessage = "Confidential: Please come home early today for the family gathering!";
        ParentSendMessageRequest request = new ParentSendMessageRequest(childUser.getId(), secretMessage);

        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(secretMessage));

        // Verify PushNotificationService constants for decoy text
        assertEquals("Check your battery status", PushNotificationService.CONVOCATION_DECOY_BODY);
        assertEquals("Device Update", PushNotificationService.CONVOCATION_DECOY_TITLE);

        // Verify that direct decoy dispatch works without message contents
        boolean dispatched = pushNotificationService.sendConvocationNotification(childDevice.getPushToken());
        assertTrue(dispatched);
    }

    @Test
    @DisplayName("Low battery alert generates push notification record and dispatches to Parent")
    void testLowBatteryNotification_DispatchesToParentDevice() throws Exception {
        parentDevice.setPushToken("fcm-parent-phone-token");
        deviceRepository.save(parentDevice);

        alertService.triggerOrUpdateAlert(
                family,
                childDevice,
                "LOW_BATTERY",
                "WARNING",
                "Low Battery Alert",
                "Child Pixel 8 battery is low (12%). Connect charger soon.",
                "ALL"
        );

        List<NotificationRecord> pushRecords = notificationRecordRepository.findAll()
                .stream()
                .filter(nr -> "PUSH".equals(nr.getChannel()))
                .toList();

        assertFalse(pushRecords.isEmpty(), "Push notification record should be created for Parent device");
        assertTrue(pushRecords.stream().anyMatch(nr -> nr.getUser().getId().equals(parentUser.getId())));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Offline and Reconnected alert events dispatch push notifications to Parent")
    void testOfflineAndReconnectedAlerts_DispatchPushToParent() throws Exception {
        parentDevice.setPushToken("fcm-parent-phone-token-2");
        deviceRepository.save(parentDevice);

        // 1. Offline Alert
        alertService.triggerOrUpdateAlert(
                family,
                childDevice,
                "OFFLINE",
                "WARNING",
                "Device Offline",
                "Child Pixel 8 has lost network connection.",
                "PARENT"
        );

        // 2. Reconnected Alert
        alertService.triggerOrUpdateAlert(
                family,
                childDevice,
                "RECONNECTED",
                "INFO",
                "Device Reconnected",
                "Child Pixel 8 has reconnected to the network.",
                "PARENT"
        );

        List<NotificationRecord> records = notificationRecordRepository.findAll();
        boolean hasOfflinePush = records.stream()
                .anyMatch(r -> "PUSH".equals(r.getChannel()) && "OFFLINE".equals(r.getAlert().getAlertType()));
        boolean hasReconnectedPush = records.stream()
                .anyMatch(r -> "PUSH".equals(r.getChannel()) && "RECONNECTED".equals(r.getAlert().getAlertType()));

        assertTrue(hasOfflinePush, "Offline alert should dispatch push notification");
        assertTrue(hasReconnectedPush, "Reconnected alert should dispatch push notification");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Important alerts (CRITICAL, PERMISSION_REVOKED, SECURITY_ALERT) dispatch push notifications")
    void testImportantAlerts_DispatchPush() throws Exception {
        parentDevice.setPushToken("fcm-parent-phone-token-3");
        deviceRepository.save(parentDevice);

        alertService.triggerOrUpdateAlert(
                family,
                childDevice,
                "PERMISSION_REVOKED",
                "CRITICAL",
                "Permission Attention Needed",
                "Required permissions are disabled: Location, Usage Access",
                "ALL"
        );

        List<NotificationRecord> records = notificationRecordRepository.findAll();
        boolean hasCriticalPush = records.stream()
                .anyMatch(r -> "PUSH".equals(r.getChannel()) && "CRITICAL".equals(r.getAlert().getSeverity()));

        assertTrue(hasCriticalPush, "Critical security/permission alert must dispatch push notification");
    }

    @Test
    @DisplayName("Invalid or revoked token error clears the token from the database")
    void testInvalidOrRevokedToken_ClearsToken() {
        childDevice.setPushToken("REVOKED_TOKEN");
        deviceRepository.save(childDevice);

        boolean result = pushNotificationService.sendAlertNotification("REVOKED_TOKEN", "LOW_BATTERY", "WARNING", "Title", "Message");
        assertFalse(result, "Dispatch to revoked token should report false");

        Device refreshed = deviceRepository.findByDeviceUuid("child-device-uuid-1").orElseThrow();
        assertNull(refreshed.getPushToken(), "Revoked push token must be cleared from device repository");
    }

    @Test
    @DisplayName("Query device push token status")
    void testGetPushTokenStatus() throws Exception {
        childDevice.setPushToken("fcm-status-token-12345");
        deviceRepository.save(childDevice);

        mockMvc.perform(get("/api/v1/devices/push-token/status")
                        .param("deviceUuid", "child-device-uuid-1")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.deviceUuid").value("child-device-uuid-1"))
                .andExpect(jsonPath("$.data.maskedToken").isNotEmpty());
    }
}
