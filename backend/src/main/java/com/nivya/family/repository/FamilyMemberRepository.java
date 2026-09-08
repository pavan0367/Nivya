package com.nivya.family.repository;

import com.nivya.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

    Optional<FamilyMember> findByUserId(Long userId);

    List<FamilyMember> findByFamilyId(Long familyId);

    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

    void deleteByFamilyIdAndUserId(Long familyId, Long userId);
}
