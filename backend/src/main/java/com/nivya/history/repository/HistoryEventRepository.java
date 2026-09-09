package com.nivya.history.repository;

import com.nivya.history.entity.HistoryEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryEventRepository extends JpaRepository<HistoryEvent, Long> {

    /**
     * Filtered, chronological paginated search for device history events.
     */
    @Query("SELECT h FROM HistoryEvent h WHERE h.device.id = :deviceId " +
            "AND (:startDate IS NULL OR h.eventTimestamp >= :startDate) " +
            "AND (:endDate IS NULL OR h.eventTimestamp <= :endDate) " +
            "AND (:appName IS NULL OR LOWER(h.appName) = LOWER(:appName) OR LOWER(h.packageName) = LOWER(:appName)) " +
            "ORDER BY h.eventTimestamp DESC")
    Page<HistoryEvent> findHistoryEvents(
            @Param("deviceId") Long deviceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("appName") String appName,
            Pageable pageable
    );

    /**
     * Finds single event ensuring device ownership.
     */
    Optional<HistoryEvent> findByIdAndDeviceId(Long id, Long deviceId);

    /**
     * Retrieves unique application names recorded for the device.
     */
    @Query("SELECT DISTINCT h.appName FROM HistoryEvent h WHERE h.device.id = :deviceId ORDER BY h.appName ASC")
    List<String> findDistinctAppNamesByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Deletes all history events for a given device.
     */
    long deleteByDeviceId(Long deviceId);
}
