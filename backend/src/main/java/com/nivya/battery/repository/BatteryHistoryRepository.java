package com.nivya.battery.repository;

import com.nivya.battery.entity.BatteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BatteryHistoryRepository extends JpaRepository<BatteryHistory, Long> {

    List<BatteryHistory> findTop50ByDeviceIdOrderByRecordedAtDesc(Long deviceId);

    List<BatteryHistory> findByDeviceIdAndRecordedAtAfterOrderByRecordedAtAsc(Long deviceId, Instant after);
}
