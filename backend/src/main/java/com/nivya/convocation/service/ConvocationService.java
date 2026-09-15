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
import com.nivya.websocket.service.RealtimeBroadcastService;
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
    private final RealtimeBroadcastService realtimeBroadcastService;

    public ConvocationService(ConvocationMessageRepository messageRepository,
                              ConvocationViewRepository viewRepository,
                              FamilyMemberRepository familyMemberRepository,
                              UserRepository userRepository,
                              DeviceRepository deviceRepository,
                              PushNotificationService pushNotificationService,
                              RealtimeBroadcastService realtimeBroadcastService) {
        this.messageRepository = messageRepository;
        this.viewRepository = viewRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.pushNotificationService = pushNotificationService;
        this.realtimeBroadcastService = realtimeBroadcastService;
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

        // Resolve Child recipient dynamically from authenticated family context
        Long receiverUserId = request.getReceiverUserId();
        if (receiverUserId != null) {
            Optional<FamilyMember> memberOpt = familyMemberRepository.findByFamilyIdAndUserId(familyId, receiverUserId);
            if (memberOpt.isEmpty() || !RoleType.CHILD.equals(memberOpt.get().getMemberRole())) {
                log.warn("Provided receiverUserId {} is not a valid child member in family {}. Resolving from family context.", receiverUserId, familyId);
                receiverUserId = null;
            }
        }
        if (receiverUserId == null && request.getTargetDeviceId() != null) {
            Optional<Device> targetDevice = deviceRepository.findById(request.getTargetDeviceId());
            if (targetDevice.isPresent() && targetDevice.get().getUser() != null) {
                Long devUserId = targetDevice.get().getUser().getId();
                if (familyMemberRepository.existsByFamilyIdAndUserId(familyId, devUserId)) {
                    receiverUserId = devUserId;
                }
            }
        }
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
        if (request.getReplyToId() != null) {
            Optional<ConvocationMessage> replyTarget = messageRepository.findById(request.getReplyToId());
            if (replyTarget.isPresent() && replyTarget.get().getFamilyId().equals(familyId)) {
                message.setReplyToId(replyTarget.get().getId());
            }
        }

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

        ParentConvocationMessageDto dto = toParentDto(message, principal.getName());
        realtimeBroadcastService.broadcastConvocationMessage(familyId, dto);
        return dto;
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

        return messages.stream()
                .filter(m -> !"UNSENT".equalsIgnoreCase(m.getStatus()))
                .map(m -> {
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

    /**
     * Parent unsends a Parent-owned message.
     * Validates ownership, family boundaries, and ensures Child messages cannot be unsent by Parent.
     */
    @Transactional
    public void parentUnsendMessage(UserPrincipal principal, Long messageId) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        ConvocationMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Convocation message not found"));

        if (!message.getFamilyId().equals(familyId)) {
            throw new AccessDeniedException("Message does not belong to this family unit");
        }

        if ("CHILD_SENT".equalsIgnoreCase(message.getStatus()) || !message.getSenderUserId().equals(principal.getId())) {
            throw new AccessDeniedException("Cannot unsend messages sent by other family members");
        }

        message.setStatus("UNSENT");
        messageRepository.save(message);
        log.info("Parent {} unsent convocation message {}", principal.getId(), messageId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "UNSEND");
        payload.put("messageId", messageId);
        realtimeBroadcastService.broadcastConvocationAction(familyId, payload);
    }

    /**
     * Parent toggles pin state on a message in the family audit log.
     */
    @Transactional
    public ParentConvocationMessageDto parentTogglePinMessage(UserPrincipal principal, Long messageId) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        ConvocationMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Convocation message not found"));

        if (!message.getFamilyId().equals(familyId)) {
            throw new AccessDeniedException("Message does not belong to this family unit");
        }

        message.setPinned(!message.isPinned());
        message = messageRepository.save(message);
        log.info("Parent {} toggled pin on message {} -> {}", principal.getId(), messageId, message.isPinned());

        String senderName = userRepository.findById(message.getSenderUserId())
                .map(User::getName).orElse("Family Member");
        ParentConvocationMessageDto dto = toParentDto(message, senderName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PIN_TOGGLE");
        payload.put("messageId", messageId);
        payload.put("isPinned", message.isPinned());
        realtimeBroadcastService.broadcastConvocationAction(familyId, payload);

        return dto;
    }

    /**
     * Parent reacts to a Child or Parent message in Convocation.
     */
    @Transactional
    public ParentConvocationMessageDto parentReactToMessage(UserPrincipal principal, Long messageId, String reaction) {
        validateParentRole(principal);
        FamilyMember parentMember = getFamilyMember(principal.getId());
        Long familyId = parentMember.getFamily().getId();

        ConvocationMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Convocation message not found"));

        if (!message.getFamilyId().equals(familyId)) {
            throw new AccessDeniedException("Message does not belong to this family unit");
        }

        message.setReaction(reaction);
        message = messageRepository.save(message);
        log.info("Parent {} reacted {} on message {}", principal.getId(), reaction, messageId);

        String senderName = userRepository.findById(message.getSenderUserId())
                .map(User::getName).orElse("Family Member");
        ParentConvocationMessageDto dto = toParentDto(message, senderName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REACTION");
        payload.put("messageId", messageId);
        payload.put("reaction", reaction);
        realtimeBroadcastService.broadcastConvocationAction(familyId, payload);

        return dto;
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

            List<Long> seenIds = unreadMessages.stream().map(ConvocationMessage::getId).collect(Collectors.toList());
            realtimeBroadcastService.broadcastConvocationSeen(familyId, seenIds, now.toString());
        }

        // 2. Return the unread messages activated for viewing in this session
        // Requirement 17: Old seen messages leave the visible set and do not reappear when new messages arrive
        List<ChildConvocationMessageDto> dtos = unreadMessages.stream()
                .map(this::toChildDto)
                .collect(Collectors.toList());

        long remainingSeconds = unreadMessages.isEmpty() ? 0 : 120;

        return new ChildViewingSessionResponse(
                sessionUuid,
                now,
                sessionExpiresAt,
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

        String trimmedMessage = request.getMessage().trim();

        // Idempotency check: prevent duplicate rapid message creation (CRACK/FREAK / network retries)
        List<ConvocationMessage> history = messageRepository.findByFamilyIdOrderByCreatedAtAsc(familyId);
        if (!history.isEmpty()) {
            ConvocationMessage last = history.get(history.size() - 1);
            if (last.getSenderUserId().equals(principal.getId()) &&
                    trimmedMessage.equals(last.getMessage()) &&
                    last.getCreatedAt() != null &&
                    Duration.between(last.getCreatedAt(), Instant.now()).getSeconds() < 5) {
                log.info("Duplicate child message suppressed by idempotency guard: {}", trimmedMessage);
                Map<String, Object> ack = new HashMap<>();
                ack.put("status", "DELIVERED");
                ack.put("acknowledged", true);
                ack.put("message", "Note received by family");
                return ack;
            }
        }

        ConvocationMessage message = new ConvocationMessage(
                familyId,
                principal.getId(),
                parentMember.getUser().getId(),
                trimmedMessage
        );
        message.setStatus("CHILD_SENT");

        messageRepository.save(message);
        log.info("Child {} sent convocation message {} to parent {}", principal.getId(), message.getId(), parentMember.getUser().getId());

        String senderName = childMember.getUser() != null ? childMember.getUser().getName() : "Child";
        ParentConvocationMessageDto parentDto = toParentDto(message, senderName);
        realtimeBroadcastService.broadcastConvocationMessage(familyId, parentDto);

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

        ParentConvocationMessageDto dto = new ParentConvocationMessageDto(
                m.getId(),
                m.getSenderUserId(),
                senderName,
                m.getReceiverUserId(),
                m.getMessage(),
                isChildOriginated,
                m.getCreatedAt(),
                isSeen,
                m.getSeenAt(),
                m.isPinned(),
                m.getReaction(),
                m.getReplyToId(),
                m.getStatus()
        );
        dto.setFamilyId(m.getFamilyId());
        return dto;
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
