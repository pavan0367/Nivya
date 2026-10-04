package com.nivya.pairing.repository;

import com.nivya.pairing.entity.DisconnectCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisconnectCodeRepository extends JpaRepository<DisconnectCode, Long> {

    List<DisconnectCode> findByFamilyIdAndStatus(Long familyId, String status);

    Optional<DisconnectCode> findFirstByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, String status);

    Optional<DisconnectCode> findByCodeHash(String codeHash);

    @Modifying
    @Query("DELETE FROM DisconnectCode d WHERE d.parentUserId = :userId OR d.childUserId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
