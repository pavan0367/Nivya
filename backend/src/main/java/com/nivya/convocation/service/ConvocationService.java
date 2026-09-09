package com.nivya.convocation.service;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.convocation.dto.*;
import com.nivya.convocation.entity.ConvocationMessage;
import com.nivya.convocation.entity.ConvocationView;
import com.nivya.convocation.repository.ConvocationMessageRepository;
import com.nivya.convocation.repository.ConvocationViewRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.notification.service.PushNotificationService;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating family Convocation messages, permanent Parent history,
 * server-authoritative 2-minute viewing sessions, 1-hour child visibility expiration,
 * and ephemeral Child-originated messages.
 *
 * Strictly isolated: Zero coupling to Battery, Network, Location, Screen Time, Usage,
 * History, Live Activity, Device Health, Alerts, or Clean Up.
 */
@Service
public class ConvocationService {

    private static final Logger log = LoggerFactory.getLogger(ConvocationService.class);

    private final ConvocationMessageRepository messageRepository;
    private final ConvocationViewRepository viewRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PushNotificationService pushNotificationService;

    public ConvocationService(ConvocationMessageRepository messageRepository,
                              ConvocationViewRepository viewRepository,
                              FamilyMemberRepository familyMemberRepository,
                              UserRepository userRepository,
                              DeviceRepository deviceRepository,
                              PushNotificationService pushNotificationService) {
        this.messageRepository = messageRepository;
        this.viewRepository = viewRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.pushNotificationService = pushNotificationService;
    }

    // =========================================================================
    // PARENT OPERATIONS
    // =========================================================================

    /**
     * Parent sends priority Convocation message to Child.
     * Triggers decoy notification: "Check your battery status".
     */
    @Transactional
    public ParentConvocationMessageDto parentSendMessage(UserPrincipal principal, ParentSendMessageRequest request) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        // Resolve Child recipient
        Long receiverUserId = request.getReceiverUserId();
        if (receiverUserId == null) {
            receiverUserId = findChildInFamily(familyId).getUser().getId();
        } else {
            validateFamilyMember(familyId, receiverUserId, RoleType.CHILD);
        }

        ConvocationMessage message = new ConvocationMessage(
                familyId,
                principal.getId(),
                receiverUserId,
                request.getMessage().trim()
        );

        message = messageRepository.save(message);
        log.info("Parent {} sent convocation message {} to child {}", principal.getId(), message.getId(), receiverUserId);

        // Dispatch decoy push notification ("Check your battery status") to Child device(s)
        // STRICT REQUIREMENT: Never send the actual message in the notification payload
        List<Device> childDevices = deviceRepository.findByUserId(receiverUserId);
        for (Device childDev : childDevices) {
            if (childDev.getPushToken() != null && !childDev.getPushToken().isBlank()) {
                pushNotificationService.sendConvocationNotification(childDev.getPushToken());
            }
        }

        return toParentDto(message, principal.getName());
    }

    /**
     * Parent retrieves complete, permanent retained Convocation history for family.
     * Includes Parent sent messages, Child-originated messages, and Seen indicators.
     */
    @Transactional(readOnly = true)
    public List<ParentConvocationMessageDto> parentGetRetainedHistory(UserPrincipal principal) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        List<ConvocationMessage> messages = messageRepository.findByFamilyIdOrderByCreatedAtAsc(familyId);
        Map<Long, String> userNames = new HashMap<>();

        return messages.stream().map(m -> {
            String senderName = userNames.computeIfAbsent(m.getSenderUserId(), id ->
                    userRepository.findById(id).map(User::getName).orElse("Family Member"));
            return toParentDto(m, senderName);
        }).collect(Collectors.toList());
    }

    /**
     * Parent retrieves current Seen state map for sent messages.
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> parentGetSeenState(UserPrincipal principal) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        List<ConvocationMessage> messages = messageRepository.findByFamilyIdOrderByCreatedAtAsc(familyId);
        Map<Long, Boolean> seenMap = new HashMap<>();
        for (ConvocationMessage m : messages) {
            boolean isSeen = m.getSeenAt() != null || "SEEN".equalsIgnoreCase(m.getStatus());
            seenMap.put(m.getId(), isSeen);
        }
        return seenMap;
    }

    // =========================================================================
    // CHILD OPERATIONS
    // =========================================================================

    /**
     * Child retrieves currently unread messages (when viewing session is not yet active).
     */
    @Transactional(readOnly = true)
    public List<ChildConvocationMessageDto> childGetUnreadMessages(UserPrincipal principal) {
        validateChildRole(principal);
        FamilyMember childMember = getFamilyMember(principal.getId());
        Long familyId = childMember.getFamily().getId();

        List<ConvocationMessage> unread = messageRepository
                .findByReceiverUserIdAndFamilyIdAndStatusOrderByCreatedAtAsc(principal.getId(), familyId, "UNREAD");

        return unread.stream().map(this::toChildDto).collect(Collectors.toList());
    }

    /**
     * Child activates viewing session (turning single Convocation toggle ON in Options).
     * All currently unread messages become visible together.
     * Server sets authoritative 2-minute expiration and marks them Seen for Parent.
     */
    @Transactional
    public ChildViewingSessionResponse childStartViewing(UserPrincipal principal) {
        validateChildRole(principal);
        FamilyMember childMember = getFamilyMember(principal.getId());
        Long familyId = childMember.getFamily().getId();
        Instant now = Instant.now();

        // 1. Find all currently unread messages
        List<ConvocationMessage> unreadMessages = messageRepository
                .findByReceiverUserIdAndFamilyIdAndStatusOrderByCreatedAtAsc(principal.getId(), familyId, "UNREAD");

        String sessionUuid = UUID.randomUUID().toString();
        Instant sessionExpiresAt = now.plus(2, ChronoUnit.MINUTES);

        if (!unreadMessages.isEmpty()) {
            for (ConvocationMessage msg : unreadMessages) {
                msg.setStatus("SEEN");
                msg.setReadAt(now);
                msg.setSeenAt(now);
                msg.setViewStartedAt(now);
                msg.setVisibilityExpiresAt(sessionExpiresAt);
                msg.setChildVisibilityExpiresAt(now.plus(1, ChronoUnit.HOURS));
            }
            messageRepository.saveAll(unreadMessages);

            ConvocationView view = new ConvocationView(
                    familyId,
                    principal.getId(),
                    sessionUuid,
                    now,
                    sessionExpiresAt
            );
            viewRepository.save(view);
            log.info("Child {} started 2-minute viewing session {} for {} messages",
                    principal.getId(), sessionUuid, unreadMessages.size());
        }

        // 2. Fetch all actively visible messages (2-minute server window + 1-hour absolute window)
        List<ConvocationMessage> visibleMessages = messageRepository
                .findActivelyVisibleMessages(principal.getId(), familyId, now);

        Instant maxExpiry = visibleMessages.stream()
                .map(ConvocationMessage::getVisibilityExpiresAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(sessionExpiresAt);

        long remainingSeconds = Math.max(0, Duration.between(now, maxExpiry).getSeconds());

        List<ChildConvocationMessageDto> dtos = visibleMessages.stream()
                .map(this::toChildDto)
                .collect(Collectors.toList());

        return new ChildViewingSessionResponse(
                sessionUuid,
                now,
                maxExpiry,
                remainingSeconds,
                dtos
        );
    }

    /**
     * Child sends a message to Parent.
     * Upon server acknowledgement, it disappears immediately from Child view,
     * while Parent retains the complete message in history.
     */
    @Transactional
    public Map<String, Object> childSendMessage(UserPrincipal principal, ChildSendMessageRequest request) {
        validateChildRole(principal);
        FamilyMember childMember = getFamilyMember(principal.getId());
        Long familyId = childMember.getFamily().getId();

        FamilyMember parentMember = findParentInFamily(familyId);

        ConvocationMessage message = new ConvocationMessage(
                familyId,
                principal.getId(),
                parentMember.getUser().getId(),
                request.getMessage().trim()
        );
        message.setStatus("CHILD_SENT");

        messageRepository.save(message);
        log.info("Child {} sent convocation message {} to parent {}", principal.getId(), message.getId(), parentMember.getUser().getId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "DELIVERED");
        response.put("acknowledged", true);
        response.put("message", "Note received by family");
        return response;
    }

    /**
     * Checks Child viewing visibility state and active timer countdown.
     */
    @Transactional(readOnly = true)
    public ChildVisibilityStateResponse childGetVisibilityState(UserPrincipal principal) {
        validateChildRole(principal);
        FamilyMember childMember = getFamilyMember(principal.getId());
        Long familyId = childMember.getFamily().getId();
        Instant now = Instant.now();

        List<ConvocationMessage> visible = messageRepository
                .findActivelyVisibleMessages(principal.getId(), familyId, now);

        long remainingSeconds = 0;
        boolean isViewingActive = false;

        if (!visible.isEmpty()) {
            Instant maxExpiry = visible.stream()
                    .map(ConvocationMessage::getVisibilityExpiresAt)
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo)
                    .orElse(now);
            remainingSeconds = Math.max(0, Duration.between(now, maxExpiry).getSeconds());
            isViewingActive = remainingSeconds > 0;
        }

        int unreadCount = messageRepository
                .findByReceiverUserIdAndFamilyIdAndStatusOrderByCreatedAtAsc(principal.getId(), familyId, "UNREAD")
                .size();

        return new ChildVisibilityStateResponse(isViewingActive, remainingSeconds, unreadCount);
    }

    // =========================================================================
    // VALIDATION & HELPERS
    // =========================================================================

    private void validateParentRole(UserPrincipal principal) {
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Convocation parent operations are restricted to Parent accounts");
        }
    }

    private void validateChildRole(UserPrincipal principal) {
        if (principal.getRole() != RoleType.CHILD) {
            throw new AccessDeniedException("Convocation child operations are restricted to Child accounts");
        }
    }

    private FamilyMember getFamilyMember(Long userId) {
        return familyMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("User is not enrolled in an active family unit"));
    }

    private FamilyMember findChildInFamily(Long familyId) {
        return familyMemberRepository.findByFamilyId(familyId).stream()
                .filter(m -> RoleType.CHILD.equals(m.getMemberRole()) || "CHILD".equalsIgnoreCase(String.valueOf(m.getMemberRole())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No child device enrolled in this family unit"));
    }

    private FamilyMember findParentInFamily(Long familyId) {
        return familyMemberRepository.findByFamilyId(familyId).stream()
                .filter(m -> RoleType.PARENT.equals(m.getMemberRole()) || "PARENT".equalsIgnoreCase(String.valueOf(m.getMemberRole())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No parent device enrolled in this family unit"));
    }

    private void validateFamilyMember(Long familyId, Long targetUserId, RoleType expectedRole) {
        FamilyMember member = familyMemberRepository.findByFamilyIdAndUserId(familyId, targetUserId)
                .orElseThrow(() -> new AccessDeniedException("Target user does not belong to the same family unit"));
        if (!expectedRole.equals(member.getMemberRole()) && !expectedRole.name().equalsIgnoreCase(String.valueOf(member.getMemberRole()))) {
            throw new AccessDeniedException("Target user does not possess required role: " + expectedRole);
        }
    }

    private ParentConvocationMessageDto toParentDto(ConvocationMessage m, String senderName) {
        boolean isChildOriginated = "CHILD_SENT".equalsIgnoreCase(m.getStatus());
        boolean isSeen = m.getSeenAt() != null || "SEEN".equalsIgnoreCase(m.getStatus());

        return new ParentConvocationMessageDto(
                m.getId(),
                m.getSenderUserId(),
                senderName,
                m.getReceiverUserId(),
                m.getMessage(),
                isChildOriginated,
                m.getCreatedAt(),
                isSeen,
                m.getSeenAt()
        );
    }

    private ChildConvocationMessageDto toChildDto(ConvocationMessage m) {
        // Strictly omits seen, seenAt, status, sender metadata
        return new ChildConvocationMessageDto(
                m.getId(),
                m.getMessage(),
                m.getCreatedAt()
        );
    }
}
