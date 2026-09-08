package com.nivya.consent.repository;

import com.nivya.consent.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    List<Consent> findByUserId(Long userId);

    List<Consent> findByFamilyId(Long familyId);
}
