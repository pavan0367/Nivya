package com.nivya.network.repository;

import com.nivya.network.entity.NetworkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkHistoryRepository extends JpaRepository<NetworkHistory, Long> {

    List<NetworkHistory> findTop50ByDeviceIdOrderByRecordedAtDesc(Long deviceId);
}
