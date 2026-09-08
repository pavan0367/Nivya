package com.nivya.auth.service;

import com.nivya.auth.dto.*;
import com.nivya.auth.entity.RefreshToken;
import com.nivya.auth.exception.DuplicateEmailException;
import com.nivya.auth.exception.InvalidTokenException;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication Service handling registration, login, token rotation, and logout.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${nivya.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), normalizedEmail, passwordHash, request.getRole());
        user.setPhone(request.getPhone());
        user = userRepository.save(user);

        log.info("Registered new user with ID: {}, role: {}", user.getId(), user.getRole());

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshTokenString = createRefreshToken(user, null);

        return new AuthResponse(
                accessToken,
                refreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed authentication attempt for email: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active (status: " + user.getStatus() + ")");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshTokenString = createRefreshToken(user, request.getDeviceFingerprint());

        log.info("User {} logged in successfully with role {}", user.getEmail(), user.getRole());

        return new AuthResponse(
                accessToken,
                refreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or non-existent"));

        if (token.isRevoked()) {
            log.warn("Attempted reuse of revoked refresh token for user ID: {}", token.getUser().getId());
            // Potential token reuse attack: revoke all tokens for this user
            refreshTokenRepository.revokeAllUserTokens(token.getUser().getId(), Instant.now());
            throw new InvalidTokenException("Refresh token was previously revoked. Please re-authenticate.");
        }

        if (token.isExpired()) {
            token.revoke();
            refreshTokenRepository.save(token);
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        // Token Rotation: revoke current token and create fresh one
        token.revoke();
        refreshTokenRepository.save(token);

        User user = token.getUser();
        String newRefreshTokenString = createRefreshToken(user, token.getDeviceFingerprint());
        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        return new AuthResponse(
                newAccessToken,
                newRefreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public void logout(String refreshTokenString, UserPrincipal principal) {
        if (refreshTokenString != null && !refreshTokenString.isBlank()) {
            refreshTokenRepository.findByTokenHash(refreshTokenString)
                    .ifPresent(token -> {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    });
        } else if (principal != null) {
            refreshTokenRepository.revokeAllUserTokens(principal.getId(), Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("User session is invalid"));
        return UserSummaryDto.fromEntity(user);
    }

    private String createRefreshToken(User user, String deviceFingerprint) {
        String tokenString = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = new RefreshToken(user, tokenString, expiresAt, deviceFingerprint);
        refreshTokenRepository.save(refreshToken);
        return tokenString;
    }
}
