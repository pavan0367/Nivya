package com.nivya.websocket.security;

import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.security.jwt.JwtTokenProvider;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring ChannelInterceptor validating JWT credentials upon STOMP CONNECT
 * and strictly enforcing family/device destination authorization upon STOMP SUBSCRIBE.
 */
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthChannelInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final WebSocketAuthorizationService authorizationService;

    public AuthChannelInterceptor(JwtTokenProvider jwtTokenProvider,
                                  UserRepository userRepository,
                                  WebSocketAuthorizationService authorizationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !accessor.isMutable()) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
            return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            // Check alternative headers e.g. token or passcode
            authHeader = accessor.getFirstNativeHeader("token");
            if (authHeader == null || authHeader.isBlank()) {
                authHeader = accessor.getPasscode();
            }
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("STOMP CONNECT rejected: Missing authorization credentials");
            throw new BadCredentialsException("Missing Authorization header in STOMP CONNECT frame");
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("STOMP CONNECT rejected: Invalid or expired JWT token");
            throw new BadCredentialsException("Invalid or expired JWT token in STOMP CONNECT frame");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        String email = jwtTokenProvider.getEmailFromToken(token);
        String roleStr = jwtTokenProvider.getRoleFromToken(token);

        UserPrincipal principal;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            principal = UserPrincipal.create(userOpt.get());
        } else {
            // Build minimal principal from claims if database query is not needed
            RoleType role = RoleType.valueOf(roleStr);
            java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getAuthority())
            );
            principal = new UserPrincipal(userId, "token-user", email, email, "", role, com.nivya.user.entity.UserStatus.ACTIVE, authorities);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        accessor.setUser(authentication);
        log.debug("STOMP CONNECT authenticated successfully for user [{}] with role [{}]",
                principal.getEmail(), principal.getRole());
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            log.warn("STOMP SUBSCRIBE rejected: Unauthenticated session");
            throw new AccessDeniedException("Unauthenticated user cannot subscribe to topics");
        }

        String destination = accessor.getDestination();
        authorizationService.authorizeSubscription(principal, destination);
    }
}
