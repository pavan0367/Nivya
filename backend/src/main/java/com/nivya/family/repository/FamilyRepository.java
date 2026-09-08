package com.nivya.family.repository;

import com.nivya.family.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findByFamilyCode(String familyCode);

    Optional<Family> findByCreatedById(Long createdById);
}
