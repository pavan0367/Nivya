package com.nivya.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.auth.dto.AuthResponse;
import com.nivya.auth.dto.LoginRequest;
import com.nivya.auth.dto.RegisterRequest;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
import com.nivya.user.dto.DeleteAccountRequest;
import com.nivya.user.dto.VerifyChildCodeRequest;
import com.nivya.user.entity.DeletionApprovalCode;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.DeletionApprovalCodeRepository;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountDeletionIntegrationTest {

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
    private DeletionApprovalCodeRepository approvalCodeRepository;

    @Autowired
    private com.nivya.email.repository.EmailNotificationRepository emailNotificationRepository;

    private void waitForEmail(String recipientEmail, String expectedSubjectPart) throws InterruptedException {
        long timeout = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < timeout) {
            var emails = emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail);
            for (var email : emails) {
                if (email.getSubject().contains(expectedSubjectPart)) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        var emails = emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail);
        assertThat(emails.stream().anyMatch(e -> e.getSubject().contains(expectedSubjectPart)))
                .withFailMessage("Expected email with subject containing '%s' to '%s', but got: %s",
                        expectedSubjectPart, recipientEmail, emails.stream().map(e -> e.getSubject()).toList())
                .isTrue();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        // Ensure active
        userRepository.findById(auth.getUser().getId()).ifPresent(u -> {
            u.setStatus(UserStatus.ACTIVE);
            userRepository.save(u);
        });

        return auth;
    }

    @Test
    @DisplayName("Parent Deletion: requires correct password, deletes parent account, and blocks subsequent login")
    void testParentAccountDeletion() throws Exception {
        String email = "parent_del_" + System.currentTimeMillis() + "@nivya.local";
        String password = "ParentP@ssword123!";
        AuthResponse auth = registerUser("Parent Test", email, password, RoleType.PARENT);
        String token = auth.getAccessToken();

        // 1. Wrong password should fail
        DeleteAccountRequest wrongReq = new DeleteAccountRequest("WrongPassword!", null);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongReq)))
                .andExpect(status().isBadRequest());

        // Account still exists
        assertThat(userRepository.findByEmail(email)).isPresent();

        // No deletion confirmation email sent on failure
        assertThat(emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email)
                .stream().noneMatch(e -> e.getSubject().contains("Permanently Deleted"))).isTrue();

        // 2. Correct password deletes account
        DeleteAccountRequest correctReq = new DeleteAccountRequest(password, null);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Account permanently removed
        assertThat(userRepository.findByEmail(email)).isEmpty();

        // Parent received permanent deletion confirmation email
        waitForEmail(email, "Nivya Account Permanently Deleted");

        // 3. Subsequent login fails
        LoginRequest loginReq = new LoginRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Parent Deletion Safety: deleting parent does NOT cascade-delete connected child")
    void testParentDeletionDoesNotDeleteChild() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String parentEmail = "parent_safe_" + suffix + "@nivya.local";
        String childEmail = "child_safe_" + suffix + "@nivya.local";
        String password = "Password123!";

        AuthResponse pAuth = registerUser("Safe Parent", parentEmail, password, RoleType.PARENT);
        AuthResponse cAuth = registerUser("Safe Child", childEmail, password, RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        User child = userRepository.findById(cAuth.getUser().getId()).orElseThrow();

        // Link in same family
        Family family = familyRepository.save(new Family("Safe Family", parent));
        familyMemberRepository.save(new FamilyMember(family, parent, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, child, RoleType.CHILD));

        // Delete parent
        DeleteAccountRequest delReq = new DeleteAccountRequest(password, null);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + pAuth.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delReq)))
                .andExpect(status().isOk());

        // Parent deleted
        assertThat(userRepository.findByEmail(parentEmail)).isEmpty();

        // CHILD STILL EXISTS AND IS ACTIVE!
        assertThat(userRepository.findByEmail(childEmail)).isPresent();
        User remainingChild = userRepository.findByEmail(childEmail).get();
        assertThat(remainingChild.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Child can still log in
        LoginRequest cLogin = new LoginRequest(childEmail, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cLogin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Connected Child Deletion: requires parent approval code, wrong code fails, valid code deletes child without deleting parent")
    void testConnectedChildDeletionFlow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String parentEmail = "p_flow_" + suffix + "@nivya.local";
        String childEmail = "c_flow_" + suffix + "@nivya.local";
        String password = "Password123!";

        AuthResponse pAuth = registerUser("Flow Parent", parentEmail, password, RoleType.PARENT);
        AuthResponse cAuth = registerUser("Flow Child", childEmail, password, RoleType.CHILD);

        User parent = userRepository.findById(pAuth.getUser().getId()).orElseThrow();
        User child = userRepository.findById(cAuth.getUser().getId()).orElseThrow();

        // Link them
        Family family = familyRepository.save(new Family("Flow Family", parent));
        familyMemberRepository.save(new FamilyMember(family, parent, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(family, child, RoleType.CHILD));

        String cToken = cAuth.getAccessToken();

        // 1. Check deletion status - must indicate connected to parent
        mockMvc.perform(get("/api/v1/account/deletion/status")
                        .header("Authorization", "Bearer " + cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.child").value(true))
                .andExpect(jsonPath("$.data.hasConnectedParent").value(true));

        // 2. Immediate deletion without code must fail
        DeleteAccountRequest emptyReq = new DeleteAccountRequest(null, null);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest());

        // 3. Child requests parent approval
        mockMvc.perform(post("/api/v1/account/deletion/request-child-approval")
                        .header("Authorization", "Bearer " + cToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresInMinutes").value(15));

        // 4. Manually set known approval code in repository for testing
        String testCode = "482619";
        DeletionApprovalCode codeRecord = approvalCodeRepository
                .findFirstByChildUserIdAndStatusOrderByCreatedAtDesc(child.getId(), "PENDING")
                .orElseThrow();
        codeRecord.setCodeHash(sha256(testCode));
        approvalCodeRepository.save(codeRecord);

        // 5. Wrong code verification fails
        VerifyChildCodeRequest badVerify = new VerifyChildCodeRequest("000000");
        mockMvc.perform(post("/api/v1/account/deletion/verify-child-code")
                        .header("Authorization", "Bearer " + cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badVerify)))
                .andExpect(status().isBadRequest());

        // 6. Correct code verification succeeds
        VerifyChildCodeRequest goodVerify = new VerifyChildCodeRequest(testCode);
        mockMvc.perform(post("/api/v1/account/deletion/verify-child-code")
                        .header("Authorization", "Bearer " + cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodVerify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(true));

        // 7. Final deletion with the approved code succeeds
        DeleteAccountRequest finalReq = new DeleteAccountRequest(null, testCode);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + cToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Child deleted
        assertThat(userRepository.findByEmail(childEmail)).isEmpty();

        // PARENT STILL EXISTS
        assertThat(userRepository.findByEmail(parentEmail)).isPresent();

        // Post-deletion emails: Child confirmation AND Connected Parent notification
        waitForEmail(childEmail, "Your Nivya Account Permanently Deleted");
        waitForEmail(parentEmail, "Your Child's Nivya Account Has Been Deleted");
    }

    @Test
    @DisplayName("Disconnected Child Deletion: child without parent deletes using password identity verification")
    void testDisconnectedChildDeletion() throws Exception {
        String email = "c_disc_" + System.currentTimeMillis() + "@nivya.local";
        String password = "ChildPassword123!";
        AuthResponse auth = registerUser("Solo Child", email, password, RoleType.CHILD);
        String token = auth.getAccessToken();

        // Status shows hasConnectedParent = false
        mockMvc.perform(get("/api/v1/account/deletion/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasConnectedParent").value(false));

        // Delete with password
        DeleteAccountRequest req = new DeleteAccountRequest(password, null);
        mockMvc.perform(post("/api/v1/account/delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertThat(userRepository.findByEmail(email)).isEmpty();

        // Disconnected child receives confirmation email
        waitForEmail(email, "Your Nivya Account Permanently Deleted");
    }
}
