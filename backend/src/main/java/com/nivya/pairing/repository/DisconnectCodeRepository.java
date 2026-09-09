package com.nivya.pairing.repository;

import com.nivya.pairing.entity.DisconnectCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectCodeRepository extends JpaRepository<DisconnectCode, Long> {

    List<DisconnectCode> findByFamilyIdAndStatus(Long familyId, String status);

    Optional<DisconnectCode> findFirstByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, String status);

    Optional<DisconnectCode> findByCodeHash(String codeHash);
}
