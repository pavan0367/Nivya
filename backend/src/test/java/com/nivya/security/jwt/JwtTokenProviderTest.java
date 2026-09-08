package com.nivya.security.jwt;

import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for JwtTokenProvider.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "4e6976796153656375726546616d696c795361666574794b6579323032363132";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("generateAccessToken creates valid token with expected claims")
    void testGenerateAndValidateToken() {
        User user = new User("John Parent", "parent@nivya.local", "hashedPassword", RoleType.PARENT);
        user.setId(42L);
        UserPrincipal principal = UserPrincipal.create(user);

        String token = tokenProvider.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("parent@nivya.local", tokenProvider.getEmailFromToken(token));
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
        assertEquals("PARENT", tokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("validateToken rejects tampered or malformed tokens")
    void testTamperedToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.invalidSignature";
        assertFalse(tokenProvider.validateToken(invalidToken));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("validateToken rejects expired tokens")
    void testExpiredToken() {
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(secret, -1000); // Already expired
        User user = new User("Jane Child", "child@nivya.local", "hashedPassword", RoleType.CHILD);
        user.setId(7L);
        UserPrincipal principal = UserPrincipal.create(user);

        String token = expiredTokenProvider.generateAccessToken(principal);

        assertFalse(tokenProvider.validateToken(token));
    }
}
