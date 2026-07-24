package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.AdminReviewItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminReviewRepository extends JpaRepository<AdminReviewItem, Long> {

    /** Unresolved items sorted by lowest confidence first. */
    @Query("SELECT r FROM AdminReviewItem r WHERE r.resolved = false ORDER BY r.confidenceScore ASC NULLS LAST, r.flaggedAt ASC")
    Page<AdminReviewItem> findAllUnresolved(Pageable pageable);

    long countByResolvedFalse();

    boolean existsByQuestionQuestionId(String questionId);

    Optional<AdminReviewItem> findByQuestionQuestionId(String questionId);
}
