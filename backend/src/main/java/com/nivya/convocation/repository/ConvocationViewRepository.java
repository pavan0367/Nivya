package com.nivya.convocation.repository;

import com.nivya.convocation.entity.ConvocationView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConvocationViewRepository extends JpaRepository<ConvocationView, Long> {

    Optional<ConvocationView> findBySessionUuid(String sessionUuid);

    List<ConvocationView> findByUserIdAndFamilyIdAndStatusAndVisibilityExpiresAtAfter(
            Long userId, Long familyId, String status, Instant now);
}
