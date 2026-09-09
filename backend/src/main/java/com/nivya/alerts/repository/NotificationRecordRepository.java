package com.nivya.alerts.repository;

import com.nivya.alerts.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByUserIdOrderBySentAtDesc(Long userId);

    List<NotificationRecord> findByAlertId(Long alertId);

    void deleteByAlertId(Long alertId);
}
