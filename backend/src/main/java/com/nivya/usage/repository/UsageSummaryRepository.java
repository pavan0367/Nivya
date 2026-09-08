package com.nivya.usage.repository;

import com.nivya.usage.entity.UsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {

    Optional<UsageSummary> findByDeviceIdAndDate(Long deviceId, LocalDate date);

    List<UsageSummary> findByDeviceIdAndDateBetweenOrderByDateAsc(Long deviceId, LocalDate startDate, LocalDate endDate);
}
