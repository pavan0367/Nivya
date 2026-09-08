package com.nivya.role.service;

import com.nivya.auth.dto.AuthResponse;
import com.nivya.auth.dto.UserSummaryDto;
import com.nivya.auth.entity.RefreshToken;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.role.dto.RoleInfoResponse;
import com.nivya.role.dto.SelectRoleRequest;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service managing role selection, role verification, and session token updates.
 */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public RoleService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider tokenProvider,
            @Value("${nivya.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse selectRole(UserPrincipal principal, SelectRoleRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        if (user.getRole() != request.getRole()) {
            log.info("User {} changing role from {} to {}", user.getEmail(), user.getRole(), request.getRole());
            user.setRole(request.getRole());
            user = userRepository.save(user);
        }

        // Generate updated UserPrincipal with the new role authority
        UserPrincipal updatedPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(updatedPrincipal);

        // Generate updated refresh token
        String refreshTokenString = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);
        RefreshToken refreshToken = new RefreshToken(user, refreshTokenString, expiresAt, null);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                newAccessToken,
                refreshTokenString,
                tokenProvider.getAccessTokenExpirationMs() / 1000,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional(readOnly = true)
    public RoleInfoResponse getCurrentRoleInfo(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        return RoleInfoResponse.forRole(user.getRole());
    }
}
