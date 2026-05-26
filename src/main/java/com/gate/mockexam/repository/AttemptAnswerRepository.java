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
}
