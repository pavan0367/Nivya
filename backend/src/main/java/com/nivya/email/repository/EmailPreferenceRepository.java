package com.nivya.email.repository;

import com.nivya.email.entity.EmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailPreferenceRepository extends JpaRepository<EmailPreference, Long> {

    Optional<EmailPreference> findByUserId(Long userId);
}
