package com.nivya.pairing.repository;

import com.nivya.pairing.entity.PairingRequest;
import com.nivya.pairing.entity.PairingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PairingRequestRepository extends JpaRepository<PairingRequest, Long> {

    Optional<PairingRequest> findByConnectionCode(String connectionCode);

    Optional<PairingRequest> findTopByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, PairingStatus status);

    List<PairingRequest> findByRequesterIdAndStatus(Long requesterId, PairingStatus status);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM PairingRequest p WHERE p.requester.id = :userId OR (p.acceptedBy IS NOT NULL AND p.acceptedBy.id = :userId)")
    void deleteAllByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
