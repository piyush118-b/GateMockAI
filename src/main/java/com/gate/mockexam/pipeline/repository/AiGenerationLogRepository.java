package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.AiGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, String> {

    List<AiGenerationLog> findByQuestionQuestionIdOrderByGeneratedAtDesc(String questionId);

    @Query("SELECT SUM(l.tokenUsage) FROM AiGenerationLog l WHERE l.question.paper.paperId = :paperId")
    Long sumTokensByPaperId(@Param("paperId") String paperId);
}
