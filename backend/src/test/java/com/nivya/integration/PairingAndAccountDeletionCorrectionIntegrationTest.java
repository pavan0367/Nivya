package com.nivya.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.admin.config.AdminInitializer;
import com.nivya.auth.dto.AuthResponse;
import com.nivya.auth.dto.RefreshTokenRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.pairing.entity.PairingRequest;
import com.nivya.pairing.entity.PairingStatus;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.session.repository.DeviceSessionRepository;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.DeletionApprovalCodeRepository;
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

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PairingAndAccountDeletionCorrectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PairingRequestRepository pairingRequestRepository;

    @Autowired
    private DeletionApprovalCodeRepository approvalCodeRepository;

    @Autowired
    private DeviceSessionRepository deviceSessionRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminInitializer adminInitializer;

    private User getOrCreateAdmin() {
        return userRepository.findAllByRole(RoleType.ADMIN).stream().findFirst().orElseGet(() -> {
            User admin = new User("System Admin", "admin.test@nivya.local", passwordEncoder.encode("AdminPass123!"), RoleType.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            return userRepository.save(admin);
        });
    }

    private String getAdminToken() {
        User admin = getOrCreateAdmin();
        return tokenProvider.generateAccessToken(UserPrincipal.create(admin));
    }

    private AuthResponse registerUser(String name, String email, String password, RoleType role) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, password, role);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                objectMapper.readTree(res.getResponse().getContentAsString()).get("data").toString(),
                AuthResponse.class
        );

        userRepository.findById(auth.getUser().getId()).ifPresent(u -> {
            u.setStatus(UserStatus.ACTIVE);
            userRepository.save(u);
        });

        return auth;
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("1. Authenticated user can exist without pairing")
    void test1_authenticatedUserExistsWithoutPairing() throws Exception {
        String email = "unpaired.user." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Unpaired Parent", email, "Password123!", RoleType.PARENT);

        assertThat(auth.getAccessToken()).isNotBlank();
        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(familyMemberRepository.findByUserId(auth.getUser().getId())).isEmpty();
        assertThat(deviceRepository.findByUserId(auth.getUser().getId())).isEmpty();
    }

    @Test
    @DisplayName("2. Unpaired pairing status returns valid NOT_PAIRED state without 500")
    void test2_unpairedPairingStatusReturnsValidState() throws Exception {
        String email = "status.check." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Unpaired Child", email, "Password123!", RoleType.CHILD);

        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paired").value(false))
                .andExpect(jsonPath("$.data.userRole").value("CHILD"));
    }

    @Test
    @DisplayName("3. Pairing code generation works while unpaired")
    void test3_pairingCodeGenerationWorksWhileUnpaired() throws Exception {
        String email = "gen.code." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Unpaired Parent", email, "Password123!", RoleType.PARENT);

        mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceFingerprint\": \"test-fingerprint-agent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.myRole").value("PARENT"))
                .andExpect(jsonPath("$.data.targetRole").value("CHILD"));
    }

    @Test
    @DisplayName("4. Wrong or unrelated pairing code rejected")
    void test4_wrongPairingCodeRejected() throws Exception {
        String email = "wrong.code." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Unpaired Child", email, "Password123!", RoleType.CHILD);

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"NV-XXXX-YYYY\", \"deviceInfo\": {\"deviceUuid\": \"child-dev-1\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("5. Expired pairing code rejected")
    void test5_expiredPairingCodeRejected() throws Exception {
        String pEmail = "parent.exp." + System.currentTimeMillis() + "@nivya.local";
        String cEmail = "child.exp." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse pAuth = registerUser("Parent Exp", pEmail, "Password123!", RoleType.PARENT);
        AuthResponse cAuth = registerUser("Child Exp", cEmail, "Password123!", RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        String expiredCode = "NV-EXPI-RED1";
        PairingRequest req = new PairingRequest(parent, expiredCode, RoleType.CHILD, Instant.now().minusSeconds(3600), "fp");
        pairingRequestRepository.save(req);

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + cAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + expiredCode + "\", \"deviceInfo\": {\"deviceUuid\": \"child-dev-2\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @DisplayName("6. Reused pairing code rejected")
    void test6_reusedPairingCodeRejected() throws Exception {
        String pEmail = "parent.reused." + System.currentTimeMillis() + "@nivya.local";
        String cEmail = "child.reused." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse pAuth = registerUser("Parent Reused", pEmail, "Password123!", RoleType.PARENT);
        AuthResponse cAuth = registerUser("Child Reused", cEmail, "Password123!", RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        String usedCode = "NV-USED-CODE";
        PairingRequest req = new PairingRequest(parent, usedCode, RoleType.CHILD, Instant.now().plusSeconds(600), "fp");
        req.setStatus(PairingStatus.ACCEPTED);
        pairingRequestRepository.save(req);

        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + cAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"" + usedCode + "\", \"deviceInfo\": {\"deviceUuid\": \"child-dev-3\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already been used")));
    }

    @Test
    @DisplayName("7. Successful pairing creates persistent relationship")
    void test7_successfulPairingCreatesPersistentRelationship() throws Exception {
        String pEmail = "parent.pair." + System.currentTimeMillis() + "@nivya.local";
        String cEmail = "child.pair." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse pAuth = registerUser("Parent Pair", pEmail, "Password123!", RoleType.PARENT);
        AuthResponse cAuth = registerUser("Child Pair", cEmail, "Password123!", RoleType.CHILD);

        // Parent generates code
        MvcResult res = mockMvc.perform(post("/api/v1/pairing/code")
                        .header("Authorization", "Bearer " + pAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceFingerprint\": \"p-fingerprint\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String code = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("code").asText();

        // Child connects using parent's code
        mockMvc.perform(post("/api/v1/pairing/connect")
                        .header("Authorization", "Bearer " + cAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code,
                                "deviceInfo", Map.of(
                                        "deviceUuid", "child-phone-" + System.currentTimeMillis(),
                                        "deviceName", "Child Phone",
                                        "platform", "ANDROID"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paired").value(true));

        // Both see paired status persistently
        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + pAuth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paired").value(true));

        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + cAuth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paired").value(true));
    }

    @Test
    @DisplayName("8. Deletion status with no request returns controlled response without 500")
    void test8_deletionStatusWithNoRequestReturnsControlledResponse() throws Exception {
        String email = "del.status." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Status User", email, "Password123!", RoleType.PARENT);

        mockMvc.perform(get("/api/v1/account/deletion/status")
                        .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("PARENT"));
    }

    @Test
    @DisplayName("9. Child deletion approval request works when paired")
    void test9_childDeletionApprovalRequestWorksWhenPaired() throws Exception {
        String pEmail = "parent.req." + System.currentTimeMillis() + "@nivya.local";
        String cEmail = "child.req." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse pAuth = registerUser("Parent Req", pEmail, "Password123!", RoleType.PARENT);
        AuthResponse cAuth = registerUser("Child Req", cEmail, "Password123!", RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        User child = userRepository.findById(cAuth.getUser().getId()).orElseThrow();

        // Establish family
        var family = familyRepository.save(new com.nivya.family.entity.Family("Req Family", parent));
        familyMemberRepository.save(new com.nivya.family.entity.FamilyMember(family, parent, RoleType.PARENT));
        familyMemberRepository.save(new com.nivya.family.entity.FamilyMember(family, child, RoleType.CHILD));

        // Request approval
        mockMvc.perform(post("/api/v1/account/deletion/request-child-approval")
                        .header("Authorization", "Bearer " + cAuth.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiresInMinutes").value(15));
    }

    @Test
    @DisplayName("10. Child deletion request while unpaired returns controlled business response (not 500)")
    void test10_childDeletionRequestWhileUnpairedReturnsControlledResponse() throws Exception {
        String email = "unpaired.child." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Unpaired Child", email, "Password123!", RoleType.CHILD);

        // Must return controlled error response (400 Bad Request), NOT 500
        mockMvc.perform(post("/api/v1/account/deletion/request-child-approval")
                        .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("connected")));
    }

    @Test
    @DisplayName("11. Admin can permanently delete normal Parent")
    void test11_adminCanPermanentlyDeleteNormalParent() throws Exception {
        String email = "parent.admin.del." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Del Parent", email, "Password123!", RoleType.PARENT);
        Long userId = auth.getUser().getId();
        String adminToken = getAdminToken();

        mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("12. Admin can permanently delete normal Child")
    void test12_adminCanPermanentlyDeleteNormalChild() throws Exception {
        String email = "child.admin.del." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Del Child", email, "Password123!", RoleType.CHILD);
        Long userId = auth.getUser().getId();
        String adminToken = getAdminToken();

        mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("13. Admin cannot delete self")
    void test13_adminCannotDeleteSelf() throws Exception {
        User admin = getOrCreateAdmin();
        String adminToken = tokenProvider.generateAccessToken(UserPrincipal.create(admin));

        mockMvc.perform(delete("/api/v1/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot delete their own account")));

        assertThat(userRepository.findById(admin.getId())).isPresent();
    }

    @Test
    @DisplayName("14. Final active admin protection remains")
    void test14_finalActiveAdminProtectionRemains() throws Exception {
        // Ensure only 1 active admin
        var admins = userRepository.findAllByRole(RoleType.ADMIN);
        User soleAdmin = admins.get(0);
        soleAdmin.setStatus(UserStatus.ACTIVE);
        userRepository.save(soleAdmin);

        // Delete any extra admins created during other tests
        for (int i = 1; i < admins.size(); i++) {
            userRepository.delete(admins.get(i));
        }

        // Create temporary second admin who will attempt to delete the final other admin
        User tempAdmin = new User("Temp Admin", "temp.admin." + System.currentTimeMillis() + "@nivya.local",
                passwordEncoder.encode("Pass123!"), RoleType.ADMIN);
        tempAdmin.setStatus(UserStatus.DISABLED); // Not active!
        tempAdmin = userRepository.save(tempAdmin);

        // Generate token for tempAdmin
        String tempToken = tokenProvider.generateAccessToken(UserPrincipal.create(tempAdmin));

        // Attempting to delete soleAdmin (the only ACTIVE admin) should be prohibited
        mockMvc.perform(delete("/api/v1/admin/users/" + soleAdmin.getId())
                        .header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("final active administrator")));

        assertThat(userRepository.findById(soleAdmin.getId())).isPresent();
    }

    @Test
    @DisplayName("15. Deleted email can register again as brand-new account")
    void test15_deletedEmailCanRegisterAgain() throws Exception {
        String reusableEmail = "reusable.email." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse original = registerUser("Original User", reusableEmail, "Password123!", RoleType.PARENT);
        Long originalId = original.getUser().getId();
        String adminToken = getAdminToken();

        // Admin permanently deletes the user
        mockMvc.perform(delete("/api/v1/admin/users/" + originalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(originalId)).isEmpty();

        // Brand-new registration with the SAME email address
        AuthResponse fresh = registerUser("Fresh User", reusableEmail, "NewPassword123!", RoleType.PARENT);
        Long freshId = fresh.getUser().getId();

        assertThat(freshId).isNotEqualTo(originalId);
        assertThat(fresh.getUser().getEmail()).isEqualTo(reusableEmail);
    }

    @Test
    @DisplayName("16. Old refresh token cannot authenticate after deletion")
    void test16_oldRefreshTokenCannotAuthenticateAfterDeletion() throws Exception {
        String email = "token.del." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse auth = registerUser("Token User", email, "Password123!", RoleType.PARENT);
        String oldRefreshToken = auth.getRefreshToken();
        Long userId = auth.getUser().getId();
        String adminToken = getAdminToken();

        // Delete user
        mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Old refresh token must be rejected
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(oldRefreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("17. Old device and session relationship is not inherited by new account")
    void test17_oldDeviceRelationshipNotInherited() throws Exception {
        String email = "device.del." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse original = registerUser("Device Parent", email, "Password123!", RoleType.PARENT);
        Long originalId = original.getUser().getId();
        String adminToken = getAdminToken();

        // Admin deletes user
        mockMvc.perform(delete("/api/v1/admin/users/" + originalId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Re-register with same email
        AuthResponse fresh = registerUser("Fresh Parent", email, "Password123!", RoleType.PARENT);
        Long freshId = fresh.getUser().getId();

        assertThat(deviceRepository.findByUserId(freshId)).isEmpty();
        assertThat(deviceSessionRepository.findAll().stream().filter(s -> s.getUser().getId().equals(freshId))).isEmpty();
    }

    @Test
    @DisplayName("18. Old pairing relationship is not inherited by new account")
    void test18_oldPairingRelationshipNotInherited() throws Exception {
        String pEmail = "p.pair.del." + System.currentTimeMillis() + "@nivya.local";
        String cEmail = "c.pair.del." + System.currentTimeMillis() + "@nivya.local";
        AuthResponse pAuth = registerUser("Old Parent", pEmail, "Password123!", RoleType.PARENT);
        AuthResponse cAuth = registerUser("Old Child", cEmail, "Password123!", RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        User child = userRepository.findById(cAuth.getUser().getId()).orElseThrow();

        // Establish pairing
        var family = familyRepository.save(new com.nivya.family.entity.Family("Del Family", parent));
        familyMemberRepository.save(new com.nivya.family.entity.FamilyMember(family, parent, RoleType.PARENT));
        familyMemberRepository.save(new com.nivya.family.entity.FamilyMember(family, child, RoleType.CHILD));

        String adminToken = getAdminToken();

        // Delete parent
        mockMvc.perform(delete("/api/v1/admin/users/" + parent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Re-register parent with the same email
        AuthResponse freshParent = registerUser("New Parent", pEmail, "Password123!", RoleType.PARENT);

        // Fresh parent must NOT be paired!
        mockMvc.perform(get("/api/v1/pairing/status")
                        .header("Authorization", "Bearer " + freshParent.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paired").value(false));
    }
}
