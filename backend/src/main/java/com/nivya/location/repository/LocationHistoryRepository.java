package com.nivya.location.repository;

import com.nivya.location.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {

    List<LocationHistory> findByDeviceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long deviceId, Instant startTime, Instant endTime);

    List<LocationHistory> findTop50ByDeviceIdOrderByRecordedAtDesc(Long deviceId);

    Optional<LocationHistory> findTopByDeviceIdOrderByRecordedAtDesc(Long deviceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LocationHistory lh WHERE lh.recordedAt < :cutoff")
    int deleteByRecordedAtBefore(@Param("cutoff") Instant cutoff);
}
