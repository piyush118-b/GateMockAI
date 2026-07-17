package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.AiArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiArtifactRepository extends JpaRepository<AiArtifact, String> {

    List<AiArtifact> findByQuestionQuestionId(String questionId);

    List<AiArtifact> findByQuestionQuestionIdAndArtifactType(String questionId, String artifactType);

    Optional<AiArtifact> findTopByQuestionQuestionIdAndArtifactTypeOrderByVersionDesc(
            String questionId, String artifactType);

    long countByQuestionPaperPaperIdAndStatus(String paperId, String status);

    List<AiArtifact> findByQuestionPaperPaperIdAndStatus(String paperId, String status);
}
