package com.nivya.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.battery.dto.BatteryTelemetryRequest;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.network.dto.NetworkTelemetryRequest;
import com.nivya.role.RoleType;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import com.nivya.websocket.dto.DeviceTelemetrySnapshotDto;
import com.nivya.websocket.redis.TransientStateStore;
import com.nivya.websocket.security.AuthChannelInterceptor;
import com.nivya.websocket.security.WebSocketAuthorizationService;
import com.nivya.websocket.service.RealtimeBroadcastService;
import com.nivya.websocket.service.TelemetrySnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RealtimeCommunicationIntegrationTest {

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
    private com.nivya.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryStatusRepository batteryStatusRepository;

    @Autowired
    private com.nivya.battery.repository.BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private com.nivya.network.repository.NetworkStatusRepository networkStatusRepository;

    @Autowired
    private com.nivya.network.repository.NetworkHistoryRepository networkHistoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Autowired
    private WebSocketAuthorizationService authorizationService;

    @Autowired
    private RealtimeBroadcastService realtimeBroadcastService;

    @Autowired
    private TransientStateStore transientStateStore;

    @Autowired
    private TelemetrySnapshotService snapshotService;

    private User parentUser;
    private User childUser;
    private User strangerUser;
    private Family family;
    private Device childDevice;
    private String parentToken;
    private String childToken;
    private String strangerToken;

    @Autowired
    private com.nivya.usage.repository.UsageAppRepository usageAppRepository;

    @Autowired
    private com.nivya.usage.repository.UsageSummaryRepository usageSummaryRepository;

    @Autowired
    private com.nivya.device.repository.DeviceHealthRepository deviceHealthRepository;

    @Autowired
    private com.nivya.location.repository.LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private com.nivya.location.repository.LocationStatusRepository locationStatusRepository;

    @Autowired
    private com.nivya.consent.repository.ConsentRepository consentRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRepository alertRepository;

    @Autowired
    private com.nivya.alerts.repository.AlertRuleRepository alertRuleRepository;

    @Autowired
    private com.nivya.alerts.repository.NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private com.nivya.activity.repository.ActivityEventRepository activityEventRepository;

    @Autowired
    private com.nivya.history.repository.HistoryEventRepository historyEventRepository;

    @Autowired
    private com.nivya.pairing.repository.PairingRequestRepository pairingRequestRepository;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent
        RegisterRequest parentReq = new RegisterRequest("Parent One", "parent.rt@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult parentRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = extractToken(parentRes);
        parentUser = userRepository.findByEmail("parent.rt@nivya.local").orElseThrow();

        // 2. Register Child
        RegisterRequest childReq = new RegisterRequest("Child One", "child.rt@nivya.local", "Password123!", RoleType.CHILD);
        MvcResult childRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = extractToken(childRes);
        childUser = userRepository.findByEmail("child.rt@nivya.local").orElseThrow();

        // 3. Register Stranger (Different Family)
        RegisterRequest strangerReq = new RegisterRequest("Stranger Parent", "stranger.rt@nivya.local", "Password123!", RoleType.PARENT);
        MvcResult strangerRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strangerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        strangerToken = extractToken(strangerRes);
        strangerUser = userRepository.findByEmail("stranger.rt@nivya.local").orElseThrow();

        // 4. Create Linked Family
        family = new Family("Realtime Test Family", parentUser);
        familyRepository.save(family);
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 5. Enroll Child Device
        childDevice = new Device(childUser, family, "device-uuid-rt-child-01", "Leo's Phone", "ANDROID");
        deviceRepository.save(childDevice);
    }

    @Test
    @DisplayName("STOMP CONNECT with valid Bearer JWT authenticates and populates accessor user")
    void testConnectAuthenticationWithValidJwt() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + parentToken);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = authChannelInterceptor.preSend(message, null);
        assertNotNull(result);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("parent.rt@nivya.local", resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("STOMP CONNECT with missing or invalid token is rejected with BadCredentialsException")
    void testConnectAuthenticationRejectedWithInvalidJwt() {
        // Missing header
        StompHeaderAccessor accessorNoAuth = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> msgNoAuth = MessageBuilder.createMessage(new byte[0], accessorNoAuth.getMessageHeaders());
        assertThrows(BadCredentialsException.class, () -> authChannelInterceptor.preSend(msgNoAuth, null));

        // Invalid token
        StompHeaderAccessor accessorInvalid = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessorInvalid.setNativeHeader("Authorization", "Bearer invalid-tampered-token");
        Message<?> msgInvalid = MessageBuilder.createMessage(new byte[0], accessorInvalid.getMessageHeaders());
        assertThrows(BadCredentialsException.class, () -> authChannelInterceptor.preSend(msgInvalid, null));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE destination authorization: Authorized parent access granted")
    void testSubscribeDestinationAuthorizationAuthorized() {
        UserPrincipal parentPrincipal = UserPrincipal.create(parentUser);

        // Parent authorized to child's telemetry topics
        assertDoesNotThrow(() -> authorizationService.authorizeSubscription(
                parentPrincipal, "/topic/battery/" + childDevice.getId()));

        assertDoesNotThrow(() -> authorizationService.authorizeSubscription(
                parentPrincipal, "/topic/network/" + childDevice.getId()));

        assertDoesNotThrow(() -> authorizationService.authorizeSubscription(
                parentPrincipal, "/topic/location/" + childDevice.getId()));

        assertDoesNotThrow(() -> authorizationService.authorizeSubscription(
                parentPrincipal, "/topic/activity/" + childDevice.getId()));

        assertDoesNotThrow(() -> authorizationService.authorizeSubscription(
                parentPrincipal, "/topic/alerts/" + family.getId()));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE destination authorization: Stranger denied from accessing other family device")
    void testSubscribeDestinationAuthorizationUnauthorizedDevice() {
        UserPrincipal strangerPrincipal = UserPrincipal.create(strangerUser);

        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeSubscription(
                strangerPrincipal, "/topic/battery/" + childDevice.getId()));

        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeSubscription(
                strangerPrincipal, "/topic/network/" + childDevice.getId()));

        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeSubscription(
                strangerPrincipal, "/topic/alerts/" + family.getId()));
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE destination authorization: Child blocked from parent-only activity topic")
    void testSubscribeDestinationAuthorizationChildBlockedFromParentActivity() {
        UserPrincipal childPrincipal = UserPrincipal.create(childUser);

        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeSubscription(
                childPrincipal, "/topic/activity/" + childDevice.getId()));
    }

    @Test
    @DisplayName("Telemetry snapshot endpoint returns complete state for reconnect rehydration")
    void testTelemetrySnapshotEndpoint() throws Exception {
        // Record telemetry for child device
        BatteryTelemetryRequest batReq = new BatteryTelemetryRequest(
                childDevice.getDeviceUuid(), 78, "DISCHARGING", "UNPLUGGED", "GOOD", 29.5, Instant.now()
        );
        mockMvc.perform(post("/api/v1/battery/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batReq)))
                .andExpect(status().isOk());

        NetworkTelemetryRequest netReq = new NetworkTelemetryRequest(
                childDevice.getDeviceUuid(), "WIFI", "Wi-Fi (Active)", true, true, 4, -50, "EXCELLENT"
        );
        mockMvc.perform(post("/api/v1/network/telemetry")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(netReq)))
                .andExpect(status().isOk());

        // Parent queries state snapshot after reconnect
        mockMvc.perform(get("/api/v1/telemetry/snapshot/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceId").value(childDevice.getId()))
                .andExpect(jsonPath("$.data.battery.batteryPct").value(78))
                .andExpect(jsonPath("$.data.network.networkType").value("WIFI"))
                .andExpect(jsonPath("$.data.isOnline").value(true));

        // Stranger is forbidden (403)
        mockMvc.perform(get("/api/v1/telemetry/snapshot/" + childDevice.getId())
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TransientStateStore gracefully survives in-memory failover and device status transitions")
    void testTransientStateStoreAndFailover() {
        Long devId = childDevice.getId();

        // 1. Mark device online
        transientStateStore.setDeviceOnline(devId, true);
        assertTrue(transientStateStore.isDeviceOnline(devId));

        // 2. Save snapshot
        DeviceTelemetrySnapshotDto dto = new DeviceTelemetrySnapshotDto(
                devId, "Leo's Phone", true, null, null, null, null, Instant.now()
        );
        transientStateStore.saveSnapshot(devId, dto);

        DeviceTelemetrySnapshotDto cached = transientStateStore.getSnapshot(devId, DeviceTelemetrySnapshotDto.class);
        assertNotNull(cached);
        assertEquals("Leo's Phone", cached.getDeviceName());
        assertTrue(cached.isOnline());

        // 3. Mark offline (simulating temporary network loss)
        transientStateStore.setDeviceOnline(devId, false);
        assertFalse(transientStateStore.isDeviceOnline(devId));

        // 4. Simulate reconnect: mark online
        transientStateStore.setDeviceOnline(devId, true);
        assertTrue(transientStateStore.isDeviceOnline(devId));
    }

    @Test
    @DisplayName("RealtimeBroadcastService broadcasts device status, convocation, and pairing events without error")
    void testRealtimeBroadcastService() {
        assertDoesNotThrow(() -> {
            realtimeBroadcastService.broadcastDeviceStatus(childDevice.getId(), family.getId(), true, "CONNECTED");
            realtimeBroadcastService.broadcastDeviceStatus(childDevice.getId(), family.getId(), false, "TIMEOUT_OFFLINE");
            realtimeBroadcastService.broadcastConvocationSeen(family.getId(), 555L, Instant.now().toString());
            realtimeBroadcastService.broadcastPairingEvent(family.getId(), Collections.singletonMap("status", "PAIRED"));
        });
    }

    @org.junit.jupiter.api.AfterEach
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
        activityEventRepository.deleteAll();
        historyEventRepository.deleteAll();
        notificationRecordRepository.deleteAll();
        alertRuleRepository.deleteAll();
        alertRepository.deleteAll();
        pairingRequestRepository.deleteAll();
        deviceStatusRepository.deleteAll();
        deviceRepository.deleteAll();
        consentRepository.deleteAll();
        familyMemberRepository.deleteAll();
        familyRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.path("data");
        if (data.has("accessToken")) {
            return data.path("accessToken").asText();
        }
        return data.path("token").path("accessToken").asText();
    }
}
