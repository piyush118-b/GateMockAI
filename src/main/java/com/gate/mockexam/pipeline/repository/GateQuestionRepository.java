package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.GateQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GateQuestionRepository extends JpaRepository<GateQuestion, String> {

    @EntityGraph(attributePaths = {"options"})
    List<GateQuestion> findByPaperPaperIdOrderByQuestionNumberAsc(String paperId);

    Optional<GateQuestion> findByPaperPaperIdAndQuestionNumber(String paperId, int questionNumber);

    long countByPaperPaperId(String paperId);

    List<GateQuestion> findByPaperPaperIdAndSection(String paperId, String section);

    List<GateQuestion> findByPaperPaperIdAndQuestionType(String paperId, String questionType);

    @Query("SELECT q FROM GateQuestion q WHERE q.paper.paperId = :paperId AND q.correctAnswer IS NULL")
    List<GateQuestion> findQuestionsWithoutAnswer(@Param("paperId") String paperId);

    List<GateQuestion> findByPaperPaperIdAndReviewStatus(String paperId, String reviewStatus);
}
