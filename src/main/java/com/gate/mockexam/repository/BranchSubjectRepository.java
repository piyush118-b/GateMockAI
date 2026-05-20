package com.gate.mockexam.repository;

import com.gate.mockexam.entity.BranchSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchSubjectRepository extends JpaRepository<BranchSubject, UUID> {
    List<BranchSubject> findByBranchIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID branchId);
}
