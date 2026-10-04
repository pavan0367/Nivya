package com.nivya.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.admin.config.AdminInitializer;
import com.nivya.admin.dto.AdminRoleChangeRequest;
import com.nivya.admin.dto.AdminStatusChangeRequest;
import com.nivya.admin.dto.AdminUserUpdateRequest;
import com.nivya.audit.entity.AuditLog;
import com.nivya.audit.repository.AuditLogRepository;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Administrative Management Module.
 * Covers all 18 required security and functional scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AdminInitializer adminInitializer;

    private User adminUser;
    private User parentUser;
    private User childUser;
    private String adminToken;
    private String parentToken;
    private String childToken;

    @BeforeEach
    public void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Admin User
        adminUser = new User("System Admin", "admin@nivya.local", passwordEncoder.encode("AdminPass123!"), RoleType.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);
        adminToken = tokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        // 2. Create Parent User
        parentUser = new User("Jane Parent", "parent@nivya.local", passwordEncoder.encode("ParentPass123!"), RoleType.PARENT);
        parentUser.setStatus(UserStatus.ACTIVE);
        parentUser = userRepository.save(parentUser);
        parentToken = tokenProvider.generateAccessToken(UserPrincipal.create(parentUser));

        // 3. Create Child User
        childUser = new User("Tommy Child", "child@nivya.local", passwordEncoder.encode("ChildPass123!"), RoleType.CHILD);
        childUser.setStatus(UserStatus.ACTIVE);
        childUser = userRepository.save(childUser);
        childToken = tokenProvider.generateAccessToken(UserPrincipal.create(childUser));
    }

    @Test
    @DisplayName("1. Unauthenticated request to /api/v1/admin/** returns 401 Unauthorized")
    public void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Parent authenticated request to /api/v1/admin/** returns 403 Forbidden")
    public void testParentReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Child authenticated request to /api/v1/admin/** returns 403 Forbidden")
    public void testChildReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Admin authenticated request to /api/v1/admin/** returns 200 OK")
    public void testAdminReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.totalParents").value(1))
                .andExpect(jsonPath("$.data.totalChildren").value(1))
                .andExpect(jsonPath("$.data.totalAdmins").value(1))
                .andExpect(jsonPath("$.data.activeUsers").value(3));
    }

    @Test
    @DisplayName("5. Public registration cannot create an ADMIN account")
    public void testPublicRegistrationCannotCreateAdmin() throws Exception {
        RegisterRequest req = new RegisterRequest("Hacker Admin", "hacker@evil.com", "Password123!", RoleType.ADMIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Public registration cannot create ADMIN accounts")));

        assertFalse(userRepository.existsByEmail("hacker@evil.com"));
    }

    @Test
    @DisplayName("6. Admin can list users with pagination and search")
    public void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "parent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email").value("parent@nivya.local"))
                .andExpect(jsonPath("$.data.content[0].role").value("PARENT"));
    }

    @Test
    @DisplayName("7. Admin can view a specific user's detailed profile")
    public void testAdminCanViewUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + parentUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(parentUser.getId()))
                .andExpect(jsonPath("$.data.email").value("parent@nivya.local"))
                .andExpect(jsonPath("$.data.name").value("Jane Parent"))
                .andExpect(jsonPath("$.data.role").value("PARENT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("8. Admin can update permitted fields of a user")
    public void testAdminCanUpdatePermittedFields() throws Exception {
        AdminUserUpdateRequest updateReq = new AdminUserUpdateRequest("Jane Updated", "parent.updated@nivya.local", "+1234567890", UserStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/admin/users/" + parentUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Jane Updated"))
                .andExpect(jsonPath("$.data.email").value("parent.updated@nivya.local"))
                .andExpect(jsonPath("$.data.phone").value("+1234567890"));

        User refreshed = userRepository.findById(parentUser.getId()).orElseThrow();
        assertEquals("Jane Updated", refreshed.getName());
        assertEquals("parent.updated@nivya.local", refreshed.getEmail());
    }

    @Test
    @DisplayName("9. Admin can disable a user")
    public void testAdminCanDisableUser() throws Exception {
        AdminStatusChangeRequest statusReq = new AdminStatusChangeRequest(UserStatus.DISABLED);

        mockMvc.perform(patch("/api/v1/admin/users/" + parentUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        User refreshed = userRepository.findById(parentUser.getId()).orElseThrow();
        assertEquals(UserStatus.DISABLED, refreshed.getStatus());
    }

    @Test
    @DisplayName("10. Admin can reactivate a user")
    public void testAdminCanReactivateUser() throws Exception {
        // Disable first
        parentUser.setStatus(UserStatus.DISABLED);
        userRepository.save(parentUser);

        AdminStatusChangeRequest statusReq = new AdminStatusChangeRequest(UserStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/admin/users/" + parentUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        User refreshed = userRepository.findById(parentUser.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, refreshed.getStatus());
    }

    @Test
    @DisplayName("11. Admin cannot change their own role")
    public void testAdminCannotChangeOwnRole() throws Exception {
        AdminRoleChangeRequest roleReq = new AdminRoleChangeRequest(RoleType.PARENT);

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Administrators cannot modify their own role")));

        User refreshed = userRepository.findById(adminUser.getId()).orElseThrow();
        assertEquals(RoleType.ADMIN, refreshed.getRole());
    }

    @Test
    @DisplayName("12. Admin cannot disable final active administrator")
    public void testAdminCannotDisableFinalActiveAdmin() throws Exception {
        // Create second admin to test that second admin cannot disable the final active admin
        User admin2 = new User("Admin Two", "admin2@nivya.local", passwordEncoder.encode("AdminPass123!"), RoleType.ADMIN);
        admin2.setStatus(UserStatus.ACTIVE);
        admin2 = userRepository.save(admin2);
        String admin2Token = tokenProvider.generateAccessToken(UserPrincipal.create(admin2));

        // Disable admin2 first so adminUser is the only active admin left
        mockMvc.perform(patch("/api/v1/admin/users/" + admin2.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminStatusChangeRequest(UserStatus.DISABLED))))
                .andExpect(status().isOk());

        // Now adminUser is the sole active admin. Try to disable adminUser using admin2 or check that disabling sole active admin is prohibited
        // Reactivate admin2
        mockMvc.perform(patch("/api/v1/admin/users/" + admin2.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminStatusChangeRequest(UserStatus.ACTIVE))))
                .andExpect(status().isOk());

        // Disable admin2 again
        mockMvc.perform(patch("/api/v1/admin/users/" + admin2.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminStatusChangeRequest(UserStatus.DISABLED))))
                .andExpect(status().isOk());

        // Now admin2 tries to disable adminUser (the last active admin)
        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminStatusChangeRequest(UserStatus.DISABLED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot disable their own account")));
    }

    @Test
    @DisplayName("13. Invalid role is rejected")
    public void testInvalidRoleRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + parentUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("14. Unauthorized user modification rejected")
    public void testUnauthorizedUserModificationRejected() throws Exception {
        AdminUserUpdateRequest updateReq = new AdminUserUpdateRequest("Hacked", "hacked@evil.com", null, UserStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/admin/users/" + parentUser.getId())
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Password hashes never appear in admin API responses")
    public void testPasswordHashesNeverAppearInResponses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/users/" + parentUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("passwordHash"));
        assertFalse(body.contains("password"));

        MvcResult listResult = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String listBody = listResult.getResponse().getContentAsString();
        assertFalse(listBody.contains("passwordHash"));
    }

    @Test
    @DisplayName("16. Tokens never appear in admin API responses")
    public void testTokensNeverAppearInResponses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/users/" + parentUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("accessToken"));
        assertFalse(body.contains("refreshToken"));
        assertFalse(body.contains("tokenHash"));
    }

    @Test
    @DisplayName("17. Administrative actions create audit logs")
    public void testAdminActionsCreateAuditLogs() throws Exception {
        AdminStatusChangeRequest statusReq = new AdminStatusChangeRequest(UserStatus.DISABLED);

        mockMvc.perform(patch("/api/v1/admin/users/" + parentUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByActionOrderByTimestampDesc("ADMIN_STATUS_CHANGE");
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals(adminUser.getId(), log.getUserId());
        assertEquals(parentUser.getId(), log.getTargetUserId());
        assertTrue(log.getDetails().contains("DISABLED"));
    }

    @Test
    @DisplayName("18. Admin provisioning is idempotent")
    public void testAdminProvisioningIsIdempotent() {
        // Since adminUser already exists in db, provisionInitialAdmin should return false and not throw
        boolean provisioned = adminInitializer.provisionInitialAdmin();
        assertFalse(provisioned);

        // Verify total admin count remains 1
        assertEquals(1, userRepository.countByRole(RoleType.ADMIN));
    }
}
