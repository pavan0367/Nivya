package com.nivya.usage.repository;

import com.nivya.usage.entity.UsageApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageAppRepository extends JpaRepository<UsageApp, Long> {

    List<UsageApp> findBySummaryIdOrderByForegroundSecondsDesc(Long summaryId);

    Optional<UsageApp> findBySummaryIdAndPackageName(Long summaryId, String packageName);
}
