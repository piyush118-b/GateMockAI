package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.AiMetadataDetails;
import com.gate.mockexam.pipeline.domain.GateQuestion;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find published official questions by subject for few-shot context in mock generation.
     * Joins ai_artifacts -> ai_metadata_details to filter by subject (subject lives in metadata table).
     */
    @Query("""
        SELECT q FROM GateQuestion q
        JOIN FETCH q.options
        JOIN q.paper p
        JOIN AiArtifact aa ON aa.question = q AND aa.artifactType = 'METADATA'
        JOIN AiMetadataDetails m ON m.artifact = aa
        WHERE p.paperType = 'Official'
          AND p.status = 'Published'
          AND q.reviewStatus = 'PUBLISHED'
          AND m.subject = :subject
        ORDER BY q.marks ASC, q.confidenceScore DESC
        """)
    List<GateQuestion> findPublishedBySubject(@Param("subject") String subject, Pageable pageable);

    /**
     * Count published official questions for a given subject.
     */
    @Query("""
        SELECT COUNT(q) FROM GateQuestion q
        JOIN q.paper p
        JOIN AiArtifact aa ON aa.question = q AND aa.artifactType = 'METADATA'
        JOIN AiMetadataDetails m ON m.artifact = aa
        WHERE p.paperType = 'Official'
          AND p.status = 'Published'
          AND q.reviewStatus = 'PUBLISHED'
          AND m.subject = :subject
        """)
    long countPublishedBySubject(@Param("subject") String subject);

    /**
     * Find published official questions by subject and topic for precise mock distribution.
     */
    @Query("""
        SELECT q FROM GateQuestion q
        JOIN FETCH q.options
        JOIN q.paper p
        JOIN AiArtifact aa ON aa.question = q AND aa.artifactType = 'METADATA'
        JOIN AiMetadataDetails m ON m.artifact = aa
        WHERE p.paperType = 'Official'
          AND p.status = 'Published'
          AND q.reviewStatus = 'PUBLISHED'
          AND m.subject = :subject
          AND m.topic = :topic
        ORDER BY q.marks ASC, q.confidenceScore DESC
        """)
    List<GateQuestion> findPublishedBySubjectAndTopic(
            @Param("subject") String subject,
            @Param("topic") String topic,
            Pageable pageable);
}
