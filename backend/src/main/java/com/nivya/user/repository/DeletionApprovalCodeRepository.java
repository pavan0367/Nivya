package com.nivya.user.repository;

import com.nivya.user.entity.DeletionApprovalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeletionApprovalCodeRepository extends JpaRepository<DeletionApprovalCode, Long> {

    Optional<DeletionApprovalCode> findFirstByChildUserIdAndStatusOrderByCreatedAtDesc(Long childUserId, String status);

    Optional<DeletionApprovalCode> findFirstByChildUserIdOrderByCreatedAtDesc(Long childUserId);

    List<DeletionApprovalCode> findAllByChildUserIdAndStatus(Long childUserId, String status);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM DeletionApprovalCode d WHERE d.childUserId = :userId OR d.parentUserId = :userId")
    void deleteAllByChildUserIdOrParentUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
