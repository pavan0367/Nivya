package com.nivya.email.repository;

import com.nivya.email.entity.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    Optional<EmailNotification> findByIdempotencyKey(String idempotencyKey);

    List<EmailNotification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<EmailNotification> findByStatus(String status);
}
