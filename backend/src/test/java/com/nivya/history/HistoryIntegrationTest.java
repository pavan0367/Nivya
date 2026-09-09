package com.nivya.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.activity.repository.ActivityEventRepository;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.AlertRuleRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
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
import com.nivya.history.dto.RecordHistoryRequest;
import com.nivya.history.entity.HistoryEvent;
import com.nivya.history.repository.HistoryEventRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryIntegrationTest {

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
    private HistoryEventRepository historyEventRepository;

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
        MvcResult pRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("History Parent", "parent.hist@nivya.local", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        parentToken = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        parentUser = userRepository.findByEmail("parent.hist@nivya.local").orElseThrow();

        // 2. Register Child User
        MvcResult cRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("History Child", "child.hist@nivya.local", "Password@123", RoleType.CHILD))))
                .andExpect(status().isCreated())
                .andReturn();
        childToken = objectMapper.readTree(cRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        childUser = userRepository.findByEmail("child.hist@nivya.local").orElseThrow();

        // 3. Register Outsider Parent
        MvcResult oRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Outsider Hist", "outsider.hist@nivya.local", "Password@123", RoleType.PARENT))))
                .andExpect(status().isCreated())
                .andReturn();
        outsiderToken = objectMapper.readTree(oRes.getResponse().getContentAsString()).get("data").get("accessToken").asText();
        outsiderParentUser = userRepository.findByEmail("outsider.hist@nivya.local").orElseThrow();

        // 4. Create Family and link members
        family = new Family("History Family", parentUser);
        family = familyRepository.save(family);
        familyMemberRepository.save(new FamilyMember(family, parentUser, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, childUser, RoleType.CHILD));

        // 5. Create Child Device
        childDevice = new Device(childUser, family, "dev-child-hist-1", "Samsung Galaxy Tab", "ANDROID");
        childDevice = deviceRepository.save(childDevice);
        deviceStatusRepository.save(new DeviceStatus(childDevice, true, 90, "WIFI", "EXCELLENT"));
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
    @DisplayName("Parent retrieves paginated, chronological history events")
    void getHistory_asParent_returnsChronologicalAndPaginated() throws Exception {
        Instant now = Instant.now();

        // Save 5 events with staggered timestamps
        for (int i = 1; i <= 5; i++) {
            historyEventRepository.save(new HistoryEvent(
                    childDevice,
                    "com.app." + i,
                    "App " + i,
                    "Broad Activity " + i,
                    "Label " + i,
                    "GENERAL",
                    i * 60,
                    now.minus(i, ChronoUnit.HOURS),
                    "Details for event " + i
            ));
        }

        // Query page 0, size 2
        mockMvc.perform(get("/api/v1/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                // Newest event first (1 hour ago)
                .andExpect(jsonPath("$.data.items[0].appName").value("App 1"))
                .andExpect(jsonPath("$.data.items[1].appName").value("App 2"));
    }

    @Test
    @DisplayName("Parent filters history by application name")
    void getHistory_filterByApplication_returnsMatchingAppsOnly() throws Exception {
        Instant now = Instant.now();

        historyEventRepository.save(new HistoryEvent(childDevice, "com.whatsapp", "WhatsApp", "Chatting with Arun", "Arun", "COMMUNICATION", 300, now.minus(1, ChronoUnit.HOURS), "Chat"));
        historyEventRepository.save(new HistoryEvent(childDevice, "com.android.chrome", "Chrome", "Browsing", null, "BROWSING", 600, now.minus(2, ChronoUnit.HOURS), "Web"));
        historyEventRepository.save(new HistoryEvent(childDevice, "com.whatsapp", "WhatsApp", "Chatting with Mom", "Mom", "COMMUNICATION", 180, now.minus(3, ChronoUnit.HOURS), "Chat"));

        mockMvc.perform(get("/api/v1/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken)
                        .param("application", "WhatsApp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].appName").value("WhatsApp"))
                .andExpect(jsonPath("$.data.items[0].activityLabel").value("Arun"))
                .andExpect(jsonPath("$.data.items[1].appName").value("WhatsApp"))
                .andExpect(jsonPath("$.data.items[1].activityLabel").value("Mom"));
    }

    @Test
    @DisplayName("Parent filters history by date range")
    void getHistory_filterByDateRange_returnsEventsWithinBounds() throws Exception {
        Instant now = Instant.now();

        // 3 days ago
        historyEventRepository.save(new HistoryEvent(childDevice, "com.app.old", "OldApp", "Activity", null, "GENERAL", 60, now.minus(3, ChronoUnit.DAYS), "Old"));
        // Today
        historyEventRepository.save(new HistoryEvent(childDevice, "com.app.today", "TodayApp", "Activity", "Label", "GENERAL", 120, now.minus(2, ChronoUnit.HOURS), "Today"));

        Instant startDate = now.minus(1, ChronoUnit.DAYS);
        Instant endDate = now.plus(1, ChronoUnit.HOURS);

        mockMvc.perform(get("/api/v1/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + parentToken)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].appName").value("TodayApp"));
    }

    @Test
    @DisplayName("Child user is strictly prohibited from accessing Parent History (403 Forbidden)")
    void getHistory_asChild_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Outsider parent from another family is forbidden (403 Forbidden)")
    void getHistory_asOutsiderParent_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/history/" + childDevice.getId())
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Parent retrieves event details within consented scope")
    void getEventDetail_asParent_returnsDetailedMetadata() throws Exception {
        HistoryEvent event = historyEventRepository.save(new HistoryEvent(
                childDevice,
                "com.google.android.apps.docs",
                "Files",
                "Viewing report.pdf",
                "report.pdf",
                "PRODUCTIVITY",
                420,
                Instant.now().minus(30, ChronoUnit.MINUTES),
                "Consented view: Document review"
        ));

        mockMvc.perform(get("/api/v1/history/" + childDevice.getId() + "/events/" + event.getId())
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(event.getId()))
                .andExpect(jsonPath("$.data.appName").value("Files"))
                .andExpect(jsonPath("$.data.broadActivity").value("Viewing report.pdf"))
                .andExpect(jsonPath("$.data.activityLabel").value("report.pdf"))
                .andExpect(jsonPath("$.data.details").value("Consented view: Document review"))
                .andExpect(jsonPath("$.data.durationFormatted").value("7m"));
    }

    @Test
    @DisplayName("Parent retrieves distinct applications for filter dropdown")
    void getDistinctApplications_asParent_returnsAppList() throws Exception {
        Instant now = Instant.now();
        historyEventRepository.save(new HistoryEvent(childDevice, "com.whatsapp", "WhatsApp", "Chatting", null, "COMMUNICATION", 100, now, null));
        historyEventRepository.save(new HistoryEvent(childDevice, "com.android.chrome", "Chrome", "Browsing", null, "BROWSING", 200, now, null));
        historyEventRepository.save(new HistoryEvent(childDevice, "com.whatsapp", "WhatsApp", "Chatting", null, "COMMUNICATION", 300, now, null));

        mockMvc.perform(get("/api/v1/history/" + childDevice.getId() + "/applications")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data", containsInAnyOrder("Chrome", "WhatsApp")));
    }

    @Test
    @DisplayName("Record history event from child device sync")
    void recordHistoryEvent_asChildDevice_success() throws Exception {
        RecordHistoryRequest request = new RecordHistoryRequest(
                childDevice.getDeviceUuid(),
                "com.google.android.youtube",
                "YouTube",
                "Watching",
                "Educational Physics",
                "ENTERTAINMENT",
                1200,
                Instant.now(),
                "Background sync sync_v1"
        );

        mockMvc.perform(post("/api/v1/history/events")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("YouTube"))
                .andExpect(jsonPath("$.data.broadActivity").value("Watching"))
                .andExpect(jsonPath("$.data.activityLabel").value("Educational Physics"));
    }
}
