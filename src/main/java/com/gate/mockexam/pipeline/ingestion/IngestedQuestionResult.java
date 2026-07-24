package com.gate.mockexam.pipeline.ingestion;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * v2.1: Unified DTO for one question extracted from the Gemini multimodal response.
 *
 * A single Gemini call now returns ALL of these fields — question text, options,
 * correct answer, enrichment metadata, and diagram bounding box — replacing:
 *   - ExtractedQuestionDto (old Python OCR output)
 *   - AiMetadataDetails, ExplanationDetails, HintDetails (old enrichment output)
 *
 * This DTO is the source of truth between MultimodalIngestionService (Pass 1)
 * and QualityReviewWorker (Pass 2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestedQuestionResult {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String questionId;          // assigned by MultimodalIngestionService after parsing
    private boolean success;
    private String failureReason;

    // ── Extraction fields (read from PDF by Gemini) ───────────────────────────
    private Integer questionNumber;
    private String  section;            // GA or CS
    private String  questionType;       // MCQ | MSQ | NAT
    private BigDecimal marks;
    private BigDecimal negativeMarks;
    private String  questionText;       // full question text as read by Gemini
    private List<IngestedOptionResult> options;

    // ── Answer derivation (Gemini solves + self-scores) ──────────────────────
    private String     correctAnswer;   // A | B | A,C | 42.5
    private BigDecimal confidenceScore; // 0.00 – 1.00

    // ── Enrichment fields (merged 4-worker output) ────────────────────────────
    private String explanation;         // step-by-step, LaTeX-supported
    private String difficulty;          // EASY | MEDIUM | HARD
    private String bloomsLevel;         // REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE
    private String subject;
    private String topic;
    private String subtopic;
    private Integer estimatedSolveTimeSecs;
    private List<String> prerequisites;
    private String hintTier1;
    private String hintTier2;
    private String hintTier3;

    // ── Diagram detection (Gemini returns bounding box if diagram found) ───────
    private boolean hasDiagram;
    private DiagramBoundingBox diagramBoundingBox;

    /** Factory for failed (parse-error) results. */
    public static IngestedQuestionResult failed(String reason) {
        return IngestedQuestionResult.builder()
                .success(false)
                .failureReason(reason)
                .confidenceScore(BigDecimal.ZERO)
                .build();
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngestedOptionResult {
        private String label;    // A, B, C, D
        private String text;     // option text
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DiagramBoundingBox {
        /** Normalized coordinates on a 0–1000 grid relative to page dimensions. */
        private Double yMin;
        private Double xMin;
        private Double yMax;
        private Double xMax;
        private Integer pageNumber;  // 1-indexed
    }
}
