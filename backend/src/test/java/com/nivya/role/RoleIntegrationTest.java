package com.nivya.role;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.role.dto.SelectRoleRequest;
import com.nivya.security.jwt.JwtTokenProvider;
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
 * Integration Tests for Role Selection, Module Permission Boundaries, and Token Updating.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    private String registerAndGetToken(String email, RoleType role) throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Test User", email, "SecurePass123!", role);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("1. User can switch role from CHILD to PARENT and receive updated JWT token")
    void testSwitchToParentRole() throws Exception {
        String childToken = registerAndGetToken("switch.parent@nivya.local", RoleType.CHILD);

        SelectRoleRequest request = new SelectRoleRequest(RoleType.PARENT);
        MvcResult result = mockMvc.perform(post("/api/v1/role/select")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("PARENT"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = jsonNode.get("data").get("accessToken").asText();

        // Verify token claims updated to PARENT
        assertEquals("PARENT", jwtTokenProvider.getRoleFromToken(newAccessToken));

        // Verify DB updated to PARENT
        User user = userRepository.findByEmail("switch.parent@nivya.local").orElseThrow();
        assertEquals(RoleType.PARENT, user.getRole());
    }

    @Test
    @DisplayName("2. User can switch role from PARENT to CHILD and receive updated JWT token")
    void testSwitchToChildRole() throws Exception {
        String parentToken = registerAndGetToken("switch.child@nivya.local", RoleType.PARENT);

        SelectRoleRequest request = new SelectRoleRequest(RoleType.CHILD);
        MvcResult result = mockMvc.perform(post("/api/v1/role/select")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("CHILD"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = jsonNode.get("data").get("accessToken").asText();

        assertEquals("CHILD", jwtTokenProvider.getRoleFromToken(newAccessToken));

        User user = userRepository.findByEmail("switch.child@nivya.local").orElseThrow();
        assertEquals(RoleType.CHILD, user.getRole());
    }

    @Test
    @DisplayName("3. GET /api/v1/role/current for PARENT returns complete parent modules")
    void testCurrentRoleForParent() throws Exception {
        String parentToken = registerAndGetToken("current.parent@nivya.local", RoleType.PARENT);

        mockMvc.perform(get("/api/v1/role/current")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("PARENT"))
                .andExpect(jsonPath("$.data.nextScreen").value("CONNECTION_SCREEN"))
                .andExpect(jsonPath("$.data.availableModules").isArray())
                .andExpect(jsonPath("$.data.availableModules[?(@ == 'LiveActivity')]").exists())
                .andExpect(jsonPath("$.data.availableModules[?(@ == 'History')]").exists());
    }

    @Test
    @DisplayName("4. GET /api/v1/role/current for CHILD returns child modules and prohibited list")
    void testCurrentRoleForChild() throws Exception {
        String childToken = registerAndGetToken("current.child@nivya.local", RoleType.CHILD);

        mockMvc.perform(get("/api/v1/role/current")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("CHILD"))
                .andExpect(jsonPath("$.data.nextScreen").value("CONNECTION_SCREEN"))
                .andExpect(jsonPath("$.data.availableModules[?(@ == 'CleanUp')]").exists())
                .andExpect(jsonPath("$.data.availableModules[?(@ == 'ScreenTime')]").exists())
                .andExpect(jsonPath("$.data.prohibitedModules[?(@ == 'LiveActivity')]").exists())
                .andExpect(jsonPath("$.data.prohibitedModules[?(@ == 'History')]").exists());
    }

    @Test
    @DisplayName("5. Unauthenticated role requests return 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/role/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/v1/role/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SelectRoleRequest(RoleType.PARENT))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
