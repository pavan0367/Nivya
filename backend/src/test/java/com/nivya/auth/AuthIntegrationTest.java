package com.nivya.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RefreshTokenRequest;
import com.nivya.auth.dto.RegisterRequest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Tests for Authentication, Token Rotation, and Role-Based Authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.nivya.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE;");
        jdbcTemplate.execute("TRUNCATE TABLE consents;");
        jdbcTemplate.execute("TRUNCATE TABLE pairing_requests;");
        jdbcTemplate.execute("TRUNCATE TABLE device_status;");
        jdbcTemplate.execute("TRUNCATE TABLE devices;");
        jdbcTemplate.execute("TRUNCATE TABLE family_members;");
        jdbcTemplate.execute("TRUNCATE TABLE families;");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens;");
        jdbcTemplate.execute("TRUNCATE TABLE users;");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE;");
    }

    @Test
    @DisplayName("1. Successful Registration creates user with hashed password and tokens")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice Parent", "alice@nivya.local", "Password123!", RoleType.PARENT);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("alice@nivya.local"))
                .andExpect(jsonPath("$.data.user.role").value("PARENT"))
                .andReturn();

        // Verify password is NOT stored in plaintext
        User savedUser = userRepository.findByEmail("alice@nivya.local").orElseThrow();
        assertNotEquals("Password123!", savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    @DisplayName("2. Duplicate email registration returns 400 Bad Request with error message")
    void testDuplicateEmailRegistrationFails() throws Exception {
        RegisterRequest request = new RegisterRequest("Bob Parent", "bob@nivya.local", "Password123!", RoleType.PARENT);

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration must fail
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Email Conflict"));
    }

    @Test
    @DisplayName("3. Login with valid credentials returns 200 OK with tokens")
    void testSuccessfulLogin() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Carol Parent", "carol@nivya.local", "SecurePass123!", RoleType.PARENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("carol@nivya.local", "SecurePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("carol@nivya.local"));
    }

    @Test
    @DisplayName("4. Login with invalid password returns 401 Unauthorized with safe error message")
    void testLoginWithInvalidPasswordFails() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("David Parent", "david@nivya.local", "CorrectPassword1!", RoleType.PARENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest wrongLoginReq = new LoginRequest("david@nivya.local", "WrongPassword999!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("5. Access token protects private endpoints (/api/v1/auth/me)")
    void testAccessTokenProtectsEndpoint() throws Exception {
        // Access without token must be 401
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // Access with malformed token must be 401
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isUnauthorized());

        // Register and obtain valid token
        RegisterRequest registerReq = new RegisterRequest("Eva Parent", "eva@nivya.local", "ValidPassword123!", RoleType.PARENT);
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = jsonNode.get("data").get("accessToken").asText();

        // Access with valid token must succeed
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("eva@nivya.local"))
                .andExpect(jsonPath("$.data.role").value("PARENT"));
    }

    @Test
    @DisplayName("6. Refresh token rotation issues new tokens and revokes old token")
    void testRefreshTokenRotation() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Frank Child", "frank@nivya.local", "ChildPass123!", RoleType.CHILD);
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String initialRefreshToken = jsonNode.get("data").get("refreshToken").asText();

        // 1. Refresh using initial refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJsonNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJsonNode.get("data").get("refreshToken").asText();
        assertNotEquals(initialRefreshToken, newRefreshToken);

        // 2. New refresh token works (verifying successful token rotation)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Attempting to reuse the revoked initial token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("7. Logout invalidates active refresh token")
    void testLogout() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Grace Parent", "grace@nivya.local", "GracePass123!", RoleType.PARENT);
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = jsonNode.get("data").get("accessToken").asText();
        String refreshToken = jsonNode.get("data").get("refreshToken").asText();

        // Call logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Refreshing with logged-out token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Role-based authorization: PARENT cannot access child-only, CHILD cannot access parent-only")
    void testRoleAuthorization() throws Exception {
        // Register PARENT
        RegisterRequest parentReq = new RegisterRequest("Peter Parent", "peter@nivya.local", "ParentPass123!", RoleType.PARENT);
        MvcResult parentResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String parentToken = objectMapper.readTree(parentResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Register CHILD
        RegisterRequest childReq = new RegisterRequest("Charlie Child", "charlie@nivya.local", "ChildPass123!", RoleType.CHILD);
        MvcResult childResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String childToken = objectMapper.readTree(childResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // 1. Parent accesses parent probe -> 200 OK
        mockMvc.perform(get("/api/v1/parent/probe")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("peter@nivya.local"));

        // 2. Parent attempts to access child probe -> 403 Forbidden
        mockMvc.perform(get("/api/v1/child/probe")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));

        // 3. Child accesses child probe -> 200 OK
        mockMvc.perform(get("/api/v1/child/probe")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("charlie@nivya.local"));

        // 4. Child attempts to access parent probe -> 403 Forbidden
        mockMvc.perform(get("/api/v1/parent/probe")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(403));

        // 5. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/parent/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("9. Child and Parent login returns authoritative persisted role")
    void testChildAndParentLoginReturnsAuthoritativeRole() throws Exception {
        // Register Child
        RegisterRequest childReq = new RegisterRequest("Luna Child", "luna@nivya.local", "ChildPass123!", RoleType.CHILD);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("CHILD"));

        // Login as Child
        LoginRequest childLogin = new LoginRequest("luna@nivya.local", "ChildPass123!");
        MvcResult childLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("CHILD"))
                .andExpect(jsonPath("$.data.user.email").value("luna@nivya.local"))
                .andReturn();

        String childToken = objectMapper.readTree(childLoginRes.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Verify /api/v1/auth/me returns persisted role CHILD
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("CHILD"))
                .andExpect(jsonPath("$.data.email").value("luna@nivya.local"));

        // Register Parent
        RegisterRequest parentReq = new RegisterRequest("Mark Parent", "mark@nivya.local", "ParentPass123!", RoleType.PARENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("PARENT"));

        // Login as Parent
        LoginRequest parentLogin = new LoginRequest("mark@nivya.local", "ParentPass123!");
        MvcResult parentLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("PARENT"))
                .andExpect(jsonPath("$.data.user.email").value("mark@nivya.local"))
                .andReturn();

        String parentToken = objectMapper.readTree(parentLoginRes.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Verify /api/v1/auth/me returns persisted role PARENT
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("PARENT"))
                .andExpect(jsonPath("$.data.email").value("mark@nivya.local"));
    }

    @Test
    @DisplayName("10. EMAIL_VERIFICATION_REQUIRED=false does not alter role behavior or persistence")
    void testEmailVerificationBypassDoesNotAlterRoleBehavior() throws Exception {
        RegisterRequest childReq = new RegisterRequest("Bypass Child", "bypass.child@nivya.local", "Password123!", RoleType.CHILD);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("CHILD"));

        // Directly authenticate without verifying email
        LoginRequest loginReq = new LoginRequest("bypass.child@nivya.local", "Password123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("CHILD"));
    }

    @Test
    @DisplayName("11. Unknown email returns 401 Unauthorized with safe generic error message")
    void testLoginWithUnknownEmailReturns401() throws Exception {
        LoginRequest unknownUser = new LoginRequest("nonexistent.user@nivya.local", "SomePassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("12. Email normalization handles case insensitivity consistently")
    void testEmailNormalizationWorksAcrossRegisterAndLogin() throws Exception {
        // Register with mixed case: Normalized.Parent@Nivya.LOCAL
        RegisterRequest regReq = new RegisterRequest("Norm User", "Normalized.Parent@Nivya.LOCAL", "SecurePass123!", RoleType.PARENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value("normalized.parent@nivya.local"));

        // Login with lowercase: normalized.parent@nivya.local
        LoginRequest loginReq1 = new LoginRequest("normalized.parent@nivya.local", "SecurePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("normalized.parent@nivya.local"));

        // Login with uppercase: NORMALIZED.PARENT@NIVYA.LOCAL
        LoginRequest loginReq2 = new LoginRequest("NORMALIZED.PARENT@NIVYA.LOCAL", "SecurePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("normalized.parent@nivya.local"));
    }

    @Test
    @DisplayName("13. PasswordEncoder configured bean correctly validates registration and login")
    void testPasswordEncoderCompatibility() throws Exception {
        String rawPassword = "Complex_P@ssw0rd!#2026";
        RegisterRequest reg = new RegisterRequest("BCrypt User", "bcrypt.test@nivya.local", rawPassword, RoleType.PARENT);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("bcrypt.test@nivya.local").orElseThrow();
        // Assert password was encoded with standard BCrypt $2a$ or $2b$
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
        assertEquals(60, user.getPasswordHash().length());

        // Assert login with exact password succeeds
        LoginRequest validLogin = new LoginRequest("bcrypt.test@nivya.local", rawPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk());

        // Assert login with wrong password fails with 401
        LoginRequest wrongLogin = new LoginRequest("bcrypt.test@nivya.local", "Wrong_Password!#2026");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
