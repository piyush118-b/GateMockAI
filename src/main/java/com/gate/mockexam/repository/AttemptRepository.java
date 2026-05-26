package com.gate.mockexam.repository;

import com.gate.mockexam.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<Attempt> findByUserIdAndTestIdOrderByStartedAtDesc(UUID userId, UUID testId);

    void deleteByTestId(UUID testId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attempt a JOIN FETCH a.test WHERE a.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED")
    List<Attempt> findAllSubmittedAttemptsWithTest();

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attempt a JOIN FETCH a.user JOIN FETCH a.test WHERE a.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED")
    List<Attempt> findAllSubmittedAttemptsWithUserAndTest();

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Attempt a JOIN FETCH a.test WHERE a.user.id = :userId AND a.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED ORDER BY a.submittedAt ASC")
    List<Attempt> findSubmittedAttemptsWithTestChronological(@org.springframework.data.repository.query.Param("userId") UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT a.user.id, AVG(a.score) FROM Attempt a WHERE a.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED GROUP BY a.user.id")
    List<Object[]> getAverageScoresByStudent();

    @org.springframework.data.jpa.repository.Query("SELECT a.user.id, MAX(a.score) FROM Attempt a WHERE a.test.id = :testId AND a.status = com.gate.mockexam.enums.AttemptStatus.SUBMITTED GROUP BY a.user.id")
    List<Object[]> getBestScoresByStudentForTest(@org.springframework.data.repository.query.Param("testId") UUID testId);
}

