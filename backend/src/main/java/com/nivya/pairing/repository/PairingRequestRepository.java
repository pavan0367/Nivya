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
}
