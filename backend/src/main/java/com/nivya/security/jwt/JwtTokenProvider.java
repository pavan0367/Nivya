package com.nivya.security.jwt;

import com.nivya.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT Token Provider for signing, parsing, and validating JSON Web Tokens using HMAC-SHA256.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${nivya.jwt.secret:4e6976796153656375726546616d696c795361666574794b6579323032363132}") String secret,
            @Value("${nivya.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getEmail())
                .claim("userId", userPrincipal.getId())
                .claim("uuid", userPrincipal.getUuid())
                .claim("name", userPrincipal.getName())
                .claim("role", userPrincipal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Number userId = (Number) getClaims(token).get("userId");
        return userId != null ? userId.longValue() : null;
    }

    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
