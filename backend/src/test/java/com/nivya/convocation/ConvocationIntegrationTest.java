package com.nivya.convocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.convocation.dto.ChildSendMessageRequest;
import com.nivya.convocation.dto.ParentSendMessageRequest;
import com.nivya.convocation.entity.ConvocationMessage;
import com.nivya.convocation.repository.ConvocationMessageRepository;
import com.nivya.convocation.repository.ConvocationViewRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConvocationIntegrationTest {

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
    private com.nivya.alerts.repository.NotificationRecordRepository notificationRecordRepository;

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
    private com.nivya.device.repository.DeviceRepository deviceRepository;

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

    private User parentUser;
    private User childUser;
    private User outsiderChildUser;
    private Family family;
    private String parentToken;
    private String childToken;
    private String outsiderChildToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabases();

        // 1. Register Parent User
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Convocation Parent", "parent.conv@nivya.local", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.conv@nivya.local").orElseThrow();

        // 2. Register Child User
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Convocation Child", "child.conv@nivya.local", "Password@123", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.conv@nivya.local").orElseThrow();

        // 3. Register Outsider Child User
        MvcResult oRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Outsider Child", "outsider.conv@nivya.local", "Password@123", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        outsiderChildToken = objectMapper.readTree(oRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        outsiderChildUser = userRepository.findByEmail("outsider.conv@nivya.local").orElseThrow();

        // 4. Create Family and link Parent & Child
        family = new Family("Convocation Test Family", parentUser);
        family = familyRepository.save(family);

        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));
    }

    @AfterEach
    void tearDown() {
        cleanDatabases();
    }

    private void cleanDatabases() {
        convocationMessageRepository.deleteAll();
        convocationViewRepository.deleteAll();
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
    @DisplayName("Scenario 1: Parent sends one message successfully")
    void test1_ParentSendsOneMessage() throws Exception {
        ParentSendMessageRequest request = new ParentSendMessageRequest(childUser.getId(), "Please wrap up dinner by 8 PM.");

        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Please wrap up dinner by 8 PM."))
                .andExpect(jsonPath("$.data.seen").value(false))
                .andExpect(jsonPath("$.data.childOriginated").value(false));

        List<ConvocationMessage> messages = convocationMessageRepository.findByFamilyIdOrderByCreatedAtAsc(family.getId());
        assertEquals(1, messages.size());
        assertEquals("UNREAD", messages.get(0).getStatus());
        assertNull(messages.get(0).getSeenAt());
    }

    @Test
    @DisplayName("Scenarios 2, 3, 4: Parent sends multiple messages; Child does not open; all remain unread")
    void test2_3_4_ParentSendsMultipleMessages_ChildDoesNotOpen_AllRemainUnread() throws Exception {
        // Parent sends Message 1
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Message 1"))))
                .andExpect(status().isOk());

        // Parent sends Message 2
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Message 2"))))
                .andExpect(status().isOk());

        // Parent sends Message 3
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Message 3"))))
                .andExpect(status().isOk());

        // Child checks unread messages (without opening viewing session)
        mockMvc.perform(get("/api/v1/convocation/child/unread")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].message").value("Message 1"))
                .andExpect(jsonPath("$.data[1].message").value("Message 2"))
                .andExpect(jsonPath("$.data[2].message").value("Message 3"));

        // Verify in DB all 3 remain UNREAD
        List<ConvocationMessage> unreadInDb = convocationMessageRepository
                .findByReceiverUserIdAndFamilyIdAndStatusOrderByCreatedAtAsc(childUser.getId(), family.getId(), "UNREAD");
        assertEquals(3, unreadInDb.size());
    }

    @Test
    @DisplayName("Scenarios 5, 6, 7, 8, 9: Child turns ON, all unread visible together, marked Seen for Parent, Child never sees Seen")
    void test5_6_7_8_9_ChildTurnsOn_AllVisibleTogether_ParentSeesSeen_ChildNeverSeesSeen() throws Exception {
        // 1. Parent sends 3 messages
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/convocation/parent/send")
                            .header("Authorization", "Bearer " + parentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Task " + i))))
                    .andExpect(status().isOk());
        }

        // 2. Child turns ON Convocation (starts viewing session)
        MvcResult viewResult = mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages", hasSize(3)))
                .andExpect(jsonPath("$.data.messages[0].message").value("Task 1"))
                .andExpect(jsonPath("$.data.messages[1].message").value("Task 2"))
                .andExpect(jsonPath("$.data.messages[2].message").value("Task 3"))
                .andExpect(jsonPath("$.data.remainingSeconds", greaterThan(0)))
                // Child response MUST NOT have "seen" field
                .andExpect(jsonPath("$.data.messages[0].seen").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].seenAt").doesNotExist())
                .andReturn();

        // 3. Parent verifies Seen status
        mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].seen").value(true))
                .andExpect(jsonPath("$.data[0].seenAt", notNullValue()))
                .andExpect(jsonPath("$.data[1].seen").value(true))
                .andExpect(jsonPath("$.data[2].seen").value(true));

        // Parent seen map endpoint check
        mockMvc.perform(get("/api/v1/convocation/parent/seen")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.*", everyItem(is(true))));
    }

    @Test
    @DisplayName("Scenarios 10, 11, 12: Two-minute viewing expiration and attempted access after expiration")
    void test10_11_12_TwoMinuteExpiration_EnforcedServerAuthoritatively() throws Exception {
        // Parent sends a message
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Expiring task"))))
                .andExpect(status().isOk());

        // Child starts viewing
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(1)));

        // Simulate 2 minutes expiring by adjusting visibilityExpiresAt in database to past
        List<ConvocationMessage> messages = convocationMessageRepository.findAll();
        for (ConvocationMessage m : messages) {
            m.setVisibilityExpiresAt(Instant.now().minus(5, ChronoUnit.SECONDS));
        }
        convocationMessageRepository.saveAll(messages);

        // Child queries visibility state after expiration (simulating app process killed or restarted)
        mockMvc.perform(get("/api/v1/convocation/child/visibility")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewingActive").value(false))
                .andExpect(jsonPath("$.data.remainingSeconds").value(0));

        // Child attempts to start viewing or fetch unread - expired messages are not returned
        mockMvc.perform(get("/api/v1/convocation/child/unread")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Scenarios 13, 14: Parent sends new message after previous view; old message does NOT return")
    void test13_14_ParentSendsNewMessageAfterView_OldMessageDoesNotReturn() throws Exception {
        // 1. Parent sends Message A
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Message A"))))
                .andExpect(status().isOk());

        // 2. Child views Message A
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].message").value("Message A"));

        // Expire Message A's 2-minute window
        List<ConvocationMessage> list = convocationMessageRepository.findAll();
        list.forEach(m -> m.setVisibilityExpiresAt(Instant.now().minus(10, ChronoUnit.SECONDS)));
        convocationMessageRepository.saveAll(list);

        // 3. Parent sends Message B later
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "Message B"))))
                .andExpect(status().isOk());

        // 4. Child checks unread: ONLY Message B must be returned! Message A must NOT return.
        mockMvc.perform(get("/api/v1/convocation/child/unread")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].message").value("Message B"));
    }

    @Test
    @DisplayName("Scenarios 15, 16, 17: Child sends message; disappears from Child view; Parent retains in complete history")
    void test15_16_17_ChildSendsMessage_DisappearsFromChild_ParentRetains() throws Exception {
        ChildSendMessageRequest request = new ChildSendMessageRequest("I finished my homework!");

        // 1. Child sends message
        mockMvc.perform(post("/api/v1/convocation/child/send")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // 2. Child checks unread / visibility: child-sent message is NOT present
        mockMvc.perform(get("/api/v1/convocation/child/unread")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 3. Parent history query contains the Child-originated message
        mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].message").value("I finished my homework!"))
                .andExpect(jsonPath("$.data[0].childOriginated").value(true));
    }

    @Test
    @DisplayName("Scenario 18: One-hour child visibility absolute expiration")
    void test18_OneHourChildVisibilityExpiration() throws Exception {
        // Parent sends message
        mockMvc.perform(post("/api/v1/convocation/parent/send")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParentSendMessageRequest(childUser.getId(), "One-hour test"))))
                .andExpect(status().isOk());

        // Child views message
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk());

        // Set childVisibilityExpiresAt to 5 seconds ago
        List<ConvocationMessage> list = convocationMessageRepository.findAll();
        list.forEach(m -> m.setChildVisibilityExpiresAt(Instant.now().minus(5, ChronoUnit.SECONDS)));
        convocationMessageRepository.saveAll(list);

        // Child attempts to view: completely empty
        mockMvc.perform(post("/api/v1/convocation/child/view/start")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(0)));

        // Parent history remains unaffected
        mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].message").value("One-hour test"));
    }

    @Test
    @DisplayName("Scenario 19: Unauthorized Child cannot access Parent history")
    void test19_UnauthorizedChildCannotAccessParentHistory() throws Exception {
        mockMvc.perform(get("/api/v1/convocation/parent/history")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 20: Cross-family isolation and module independence verification")
    void test20_CrossFamilyIsolationAndIndependence() throws Exception {
        // Outsider child cannot read this family's messages
        mockMvc.perform(get("/api/v1/convocation/child/unread")
                        .header("Authorization", "Bearer " + outsiderChildToken))
                .andExpect(status().isForbidden());
    }
}
