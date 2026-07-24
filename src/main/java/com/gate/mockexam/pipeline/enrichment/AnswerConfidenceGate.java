package com.gate.mockexam.pipeline.enrichment;

import com.gate.mockexam.pipeline.domain.AdminReviewItem;
import com.gate.mockexam.pipeline.domain.GateQuestion;
import com.gate.mockexam.pipeline.ingestion.IngestedQuestionResult;
import com.gate.mockexam.pipeline.repository.AdminReviewRepository;
import com.gate.mockexam.pipeline.repository.GateQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * v2.1 — Answer Confidence Gate.
 *
 * Routes each LLM-solved question to PUBLISHED or NEEDS_REVIEW based on
 * the Gemini confidence score:
 *
 *   >= nat-threshold (0.85)  for NAT questions  → PUBLISHED
 *   >= threshold     (0.70)  for MCQ/MSQ         → PUBLISHED
 *   < threshold                                   → NEEDS_REVIEW
 *
 * NEEDS_REVIEW questions are:
 *   - Hidden from students (review_status = NEEDS_REVIEW)
 *   - Added to admin_review_queue table
 *   - NOT eligible for pgvector indexing until approved
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerConfidenceGate {

    private final GateQuestionRepository questionRepository;
    private final AdminReviewRepository reviewRepository;

    @Value("${gemini.confidence.threshold:0.70}")
    private double threshold;

    @Value("${gemini.confidence.nat-threshold:0.85}")
    private double natThreshold;

    /**
     * Applies the confidence gate. Updates review_status on the question entity.
     *
     * @param question the persisted GateQuestion entity
     * @param result   the enriched result from Pass 1 / Pass 2
     * @return true if PUBLISHED, false if NEEDS_REVIEW
     */
    public boolean applyGate(GateQuestion question, IngestedQuestionResult result) {
        BigDecimal score = result.getConfidenceScore() != null
                ? result.getConfidenceScore() : BigDecimal.ZERO;

        double effectiveThreshold = "NAT".equalsIgnoreCase(question.getQuestionType())
                ? natThreshold : threshold;

        boolean publish = score.doubleValue() >= effectiveThreshold;
        String status  = publish ? "PUBLISHED" : "NEEDS_REVIEW";

        question.setReviewStatus(status);
        questionRepository.save(question);

        if (publish) {
            log.info("[ConfidenceGate] ✅ PUBLISHED {} — score={} (threshold={})",
                    question.getQuestionId(), score, effectiveThreshold);
        } else {
            log.warn("[ConfidenceGate] 🔴 NEEDS_REVIEW {} — score={} (threshold={})",
                    question.getQuestionId(), score, effectiveThreshold);
            enqueueForReview(question, score, effectiveThreshold);
        }
        return publish;
    }

    private void enqueueForReview(GateQuestion question, BigDecimal score, double threshold) {
        if (reviewRepository.existsByQuestionQuestionId(question.getQuestionId())) return;

        String reason = String.format(
                "LLM confidence %.2f below %.2f threshold for %s question",
                score.doubleValue(), threshold, question.getQuestionType());

        reviewRepository.save(AdminReviewItem.builder()
                .question(question)
                .flaggedReason(reason)
                .confidenceScore(score)
                .build());
    }
}
