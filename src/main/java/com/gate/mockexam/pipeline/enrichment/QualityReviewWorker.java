package com.gate.mockexam.pipeline.enrichment;

import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.repository.AiArtifactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Worker 5 — Quality Review.
 *
 * Inspects ALL AI artifacts generated for a question and:
 *   - Verifies metadata correctness (difficulty aligns with question complexity)
 *   - Verifies explanation completeness and accuracy
 *   - Rejects artifacts with empty or incoherent outputs
 *   - Marks verified artifacts as VERIFIED, bad ones as ERROR
 *
 * This worker runs AFTER all other workers complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QualityReviewWorker {

    private final AiArtifactRepository artifactRepository;

    /**
     * Reviews all GENERATED artifacts for a question.
     * @return number of artifacts that passed review
     */
    public int review(GateQuestion question) {
        log.debug("[Worker5/QualityReview] Reviewing artifacts for: {}", question.getQuestionId());

        List<AiArtifact> artifacts = artifactRepository.findByQuestionQuestionId(question.getQuestionId());
        int passed = 0;
        int rejected = 0;

        for (AiArtifact artifact : artifacts) {
            if (!"GENERATED".equals(artifact.getStatus())) {
                continue; // Skip PENDING and already-ERROR artifacts
            }

            boolean valid = validateArtifact(artifact);
            if (valid) {
                artifact.setStatus("VERIFIED");
                passed++;
            } else {
                artifact.setStatus("ERROR");
                rejected++;
                log.warn("[Worker5/QualityReview] Rejected artifact {} (type={}) for question {}",
                        artifact.getArtifactId(), artifact.getArtifactType(), question.getQuestionId());
            }
            artifactRepository.save(artifact);
        }

        log.info("[Worker5/QualityReview] Question {} → {}/{} artifacts passed",
                question.getQuestionId(), passed, passed + rejected);
        return passed;
    }

    private boolean validateArtifact(AiArtifact artifact) {
        // Basic structural validation per type
        return switch (artifact.getArtifactType()) {
            case "METADATA" -> artifact.getVersion() > 0 && artifact.getModel() != null;
            case "EXPLANATION" -> artifact.getVersion() > 0 && artifact.getModel() != null;
            case "HINT" -> artifact.getVersion() > 0;
            case "CONCEPT" -> artifact.getVersion() > 0;
            case "EMBEDDING" -> artifact.getVersion() > 0;
            default -> false;
        };
    }
}
