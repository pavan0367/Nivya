package com.nivya.convocation.repository;

import com.nivya.convocation.entity.ConvocationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ConvocationMessageRepository extends JpaRepository<ConvocationMessage, Long> {

    /**
     * Parent Retained History: Retrieves all messages in the family, ordered chronologically.
     */
    List<ConvocationMessage> findByFamilyIdOrderByCreatedAtAsc(Long familyId);

    /**
     * Unread Parent Messages: Messages sent to the child that have not been viewed yet.
     */
    List<ConvocationMessage> findByReceiverUserIdAndFamilyIdAndStatusOrderByCreatedAtAsc(
            Long receiverUserId, Long familyId, String status);

    /**
     * Actively Visible Messages: Messages where visibility has not expired (within 2 min of view start)
     * and within 1 hour child visibility absolute expiration.
     */
    @Query("SELECT m FROM ConvocationMessage m WHERE m.receiverUserId = :childUserId " +
            "AND m.familyId = :familyId " +
            "AND m.visibilityExpiresAt IS NOT NULL AND m.visibilityExpiresAt > :now " +
            "AND m.childVisibilityExpiresAt IS NOT NULL AND m.childVisibilityExpiresAt > :now " +
            "ORDER BY m.createdAt ASC")
    List<ConvocationMessage> findActivelyVisibleMessages(
            @Param("childUserId") Long childUserId,
            @Param("familyId") Long familyId,
            @Param("now") Instant now);
}
