package com.nivya.activity.repository;

import com.nivya.activity.entity.ActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    /**
     * Finds the single currently active foreground activity event for a device.
     */
    Optional<ActivityEvent> findFirstByDeviceIdAndCurrentTrueOrderByStartedAtDesc(Long deviceId);

    /**
     * Finds all active activities flagged as current for a device (to clear when switching).
     */
    List<ActivityEvent> findByDeviceIdAndCurrentTrue(Long deviceId);

    /**
     * Retrieves chronological activity events for a device ordered newest first.
     */
    List<ActivityEvent> findByDeviceIdOrderByStartedAtDesc(Long deviceId, Pageable pageable);

    /**
     * Retrieves recent chronological activity events for a device ordered newest first.
     */
    List<ActivityEvent> findTop20ByDeviceIdOrderByStartedAtDesc(Long deviceId);

    /**
     * Deletes all activity events for a given device.
     */
    long deleteByDeviceId(Long deviceId);
}
