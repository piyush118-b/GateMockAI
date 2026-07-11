package com.gate.mockexam.repository;

import com.gate.mockexam.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAttemptId(UUID attemptId);

    @org.springframework.data.jpa.repository.Query("SELECT aa FROM AttemptAnswer aa " +
           "JOIN FETCH aa.question q " +
           "JOIN FETCH q.test t " +
           "JOIN FETCH aa.attempt att " +
           "WHERE att.user.id = :userId AND att.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED")
    List<AttemptAnswer> findSubmittedAnswersByUserId(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT aa FROM AttemptAnswer aa " +
           "JOIN aa.attempt a " +
           "WHERE a.user.id = :userId " +
           "AND aa.nextReview <= :today " +
           "AND aa.marksAwarded <= 0 " +
           "ORDER BY aa.nextReview ASC")
    List<AttemptAnswer> findDueForReview(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("today") java.time.LocalDate today);
}
