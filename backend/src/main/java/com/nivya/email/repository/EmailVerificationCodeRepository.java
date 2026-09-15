package com.nivya.email.repository;

import com.nivya.email.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(String email, String purpose);

    List<EmailVerificationCode> findAllByEmailAndPurposeAndUsedAtIsNull(String email, String purpose);

    Optional<EmailVerificationCode> findByCodeHash(String codeHash);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM EmailVerificationCode e WHERE (e.user IS NOT NULL AND e.user.id = :userId) OR e.email = :email")
    void deleteAllByUserIdOrEmail(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("email") String email);
}
