package com.nivya.email.repository;

import com.nivya.email.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(String email, String purpose);

    Optional<EmailVerificationCode> findByCodeHash(String codeHash);
}
