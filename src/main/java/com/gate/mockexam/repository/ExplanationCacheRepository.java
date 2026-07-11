package com.gate.mockexam.repository;

import com.gate.mockexam.entity.ExplanationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExplanationCacheRepository extends JpaRepository<ExplanationCache, Long> {
    Optional<ExplanationCache> findByQuestionIdAndStudentAnswer(UUID questionId, String studentAnswer);
}
