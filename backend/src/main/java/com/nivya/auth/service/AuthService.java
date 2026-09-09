package com.nivya.auth.service;

import com.nivya.audit.service.AuditService;
import com.nivya.auth.dto.*;
import com.nivya.auth.entity.RefreshToken;
import com.nivya.auth.exception.DuplicateEmailException;
import com.nivya.auth.exception.InvalidTokenException;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.session.service.DeviceSessionService;
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
 * Authentication Service handling registration, login, token rotation, audit logging,
 * and rate-limiting brute-force protection.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthRateLimiter authRateLimiter;
    private final AuditService auditService;
    private final DeviceSessionService deviceSessionService;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuthRateLimiter authRateLimiter,
            AuditService auditService,
            DeviceSessionService deviceSessionService,
            @Value("${nivya.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authRateLimiter = authRateLimiter;
        this.auditService = auditService;
        this.deviceSessionService = deviceSessionService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, "127.0.0.1");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            auditService.logEvent(null, "AUTH_REGISTER_FAILED", "Duplicate email attempt: " + normalizedEmail, ipAddress);
            throw new DuplicateEmailException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), normalizedEmail, passwordHash, request.getRole());
        user.setPhone(request.getPhone());
        user = userRepository.save(user);

        log.info("Registered new user with ID: {}, role: {}", user.getId(), user.getRole());
        auditService.logEvent(user.getId(), "AUTH_REGISTER_SUCCESS", "Registered new user with role " + user.getRole(), ipAddress);

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
        return login(request, "127.0.0.1");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Rate-limiting check against brute-force attacks
        authRateLimiter.checkRateLimit(ipAddress, normalizedEmail);

        // 2. Validate user and credentials
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed authentication attempt for email: {}", normalizedEmail);
            authRateLimiter.recordFailedAttempt(ipAddress, normalizedEmail);
            auditService.logEvent(null, "AUTH_LOGIN_FAILED", "Invalid credentials for " + normalizedEmail, ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            auditService.logEvent(user.getId(), "AUTH_LOGIN_BLOCKED", "Inactive account login attempt", ipAddress);
            throw new IllegalStateException("Account is not active (status: " + user.getStatus() + ")");
        }

        // 3. Reset rate limiter and log success
        authRateLimiter.recordSuccess(ipAddress, normalizedEmail);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.logEvent(user.getId(), "AUTH_LOGIN_SUCCESS", "Login successful with role " + user.getRole(), ipAddress);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshTokenString = createRefreshToken(user, request.getDeviceFingerprint());

        deviceSessionService.recordLogin(
                user,
                request.getDeviceFingerprint(),
                request.getDeviceName(),
                request.getPlatform(),
                request.getOsVersion(),
                request.getAppVersion(),
                ipAddress
        );

        log.info("User {} logged in successfully with role {}", user.getEmail(), user.getRole());

        return new AuthResponse(
                accessToken,
                refreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshToken(request, "127.0.0.1");
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or non-existent"));

        if (token.isRevoked()) {
            log.warn("Attempted reuse of revoked refresh token for user ID: {}", token.getUser().getId());
            auditService.logEvent(token.getUser().getId(), "TOKEN_REUSE_DETECTED",
                    "Attempted reuse of revoked refresh token; all active tokens invalidated", ipAddress);
            // Potential token reuse attack: revoke all tokens for this user
            refreshTokenRepository.revokeAllUserTokens(token.getUser().getId(), Instant.now());
            throw new InvalidTokenException("Refresh token was previously revoked. Please re-authenticate.");
        }

        if (token.isExpired()) {
            token.revoke();
            refreshTokenRepository.save(token);
            auditService.logEvent(token.getUser().getId(), "TOKEN_EXPIRED", "Expired refresh token submitted", ipAddress);
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        // Token Rotation: revoke current token and create fresh one
        token.revoke();
        refreshTokenRepository.save(token);

        User user = token.getUser();
        String newRefreshTokenString = createRefreshToken(user, token.getDeviceFingerprint());
        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        auditService.logEvent(user.getId(), "TOKEN_ROTATED", "Successfully rotated refresh token", ipAddress);

        return new AuthResponse(
                newAccessToken,
                newRefreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public void logout(String refreshTokenString, UserPrincipal principal) {
        logout(refreshTokenString, principal, "127.0.0.1");
    }

    @Transactional
    public void logout(String refreshTokenString, UserPrincipal principal, String ipAddress) {
        if (refreshTokenString != null && !refreshTokenString.isBlank()) {
            refreshTokenRepository.findByTokenHash(refreshTokenString)
                    .ifPresent(token -> {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    });
        } else if (principal != null) {
            refreshTokenRepository.revokeAllUserTokens(principal.getId(), Instant.now());
        }

        if (principal != null) {
            userRepository.findById(principal.getId()).ifPresent(user ->
                    deviceSessionService.recordLogout(user, refreshTokenString, ipAddress));
        }

        Long userId = principal != null ? principal.getId() : null;
        auditService.logEvent(userId, "AUTH_LOGOUT", "User logged out", ipAddress);
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
