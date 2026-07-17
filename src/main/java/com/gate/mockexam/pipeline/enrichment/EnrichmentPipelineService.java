package com.gate.mockexam.pipeline.enrichment;

import com.gate.mockexam.pipeline.domain.GateQuestion;
import com.gate.mockexam.pipeline.domain.Paper;
import com.gate.mockexam.pipeline.repository.GateQuestionRepository;
import com.gate.mockexam.pipeline.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pipeline 2 — AI Enrichment Pipeline.
 *
 * This pipeline starts ONLY AFTER Pipeline 1 (Extraction) completes successfully.
 * It consumes validated GateQuestion objects from the database and runs 5 workers
 * in sequence per question:
 *
 *   Worker 1: MetadataGeneratorWorker  → subject, topic, difficulty, bloom level
 *   Worker 2: ExplanationGeneratorWorker → detailed explanation + alt approach
 *   Worker 3: ConceptExtractorWorker   → concepts, keywords, prerequisites
 *   Worker 4: HintGeneratorWorker      → 3-tier hints
 *   Worker 5: QualityReviewWorker      → validates all generated artifacts
 *
 * DESIGN PRINCIPLE:
 *   - Extraction data (GateQuestion, Paper) is NEVER modified.
 *   - All AI outputs are stored in separate ai_artifacts tables.
 *   - Each worker's output is independently versioned.
 *   - Failed workers do not stop other workers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentPipelineService {

    private final PaperRepository paperRepository;
    private final GateQuestionRepository questionRepository;
    private final MetadataGeneratorWorker metadataWorker;
    private final ExplanationGeneratorWorker explanationWorker;
    private final ConceptExtractorWorker conceptWorker;
    private final HintGeneratorWorker hintWorker;
    private final QualityReviewWorker qualityWorker;

    /**
     * Runs the full AI enrichment pipeline asynchronously for all questions in a paper.
     *
     * @param paperId the paper to enrich
     * @return summary stats as a string
     */
    @Async
    public CompletableFuture<String> enrichPaperAsync(String paperId) {
        return CompletableFuture.supplyAsync(() -> enrichPaper(paperId));
    }

    /**
     * Synchronous version — runs enrichment for all questions in a paper.
     */
    public String enrichPaper(String paperId) {
        log.info("=== PIPELINE 2 START: paperId={} ===", paperId);

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

        List<GateQuestion> questions = questionRepository
                .findByPaperPaperIdOrderByQuestionNumberAsc(paperId);

        if (questions.isEmpty()) {
            log.warn("[Pipeline 2] No questions found for paper: {}", paperId);
            return "No questions to enrich for paper: " + paperId;
        }

        log.info("[Pipeline 2] Enriching {} questions for paper: {}", questions.size(), paperId);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount   = new AtomicInteger(0);

        for (GateQuestion question : questions) {
            try {
                enrichQuestion(question);
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.error("[Pipeline 2] Failed to enrich question {}: {}",
                        question.getQuestionId(), e.getMessage(), e);
                errorCount.incrementAndGet();
            }
        }

        // Update paper status to Enriched
        paper.setStatus("Enriched");
        paperRepository.save(paper);

        String summary = String.format(
                "Pipeline 2 COMPLETE for paper=%s | enriched=%d | errors=%d",
                paperId, successCount.get(), errorCount.get());
        log.info("=== PIPELINE 2 COMPLETE: {} ===", summary);
        return summary;
    }

    /**
     * Enriches a single question by running all 5 workers in sequence.
     * Worker failures are isolated — one failing worker does not abort others.
     */
    public void enrichQuestion(GateQuestion question) {
        String qId = question.getQuestionId();
        log.debug("[Pipeline 2] Enriching question: {}", qId);

        // Worker 1: Metadata
        try {
            metadataWorker.generate(question);
            log.debug("[Pipeline 2] ✓ Worker1/Metadata for {}", qId);
        } catch (Exception e) {
            log.warn("[Pipeline 2] ✗ Worker1/Metadata failed for {}: {}", qId, e.getMessage());
        }

        // Worker 2: Explanation
        try {
            explanationWorker.generate(question);
            log.debug("[Pipeline 2] ✓ Worker2/Explanation for {}", qId);
        } catch (Exception e) {
            log.warn("[Pipeline 2] ✗ Worker2/Explanation failed for {}: {}", qId, e.getMessage());
        }

        // Worker 3: Concepts
        try {
            conceptWorker.generate(question);
            log.debug("[Pipeline 2] ✓ Worker3/Concepts for {}", qId);
        } catch (Exception e) {
            log.warn("[Pipeline 2] ✗ Worker3/Concepts failed for {}: {}", qId, e.getMessage());
        }

        // Worker 4: Hints (all 3 levels)
        for (int level = 1; level <= 3; level++) {
            final int l = level;
            try {
                hintWorker.generate(question, l);
                log.debug("[Pipeline 2] ✓ Worker4/Hint-L{} for {}", l, qId);
            } catch (Exception e) {
                log.warn("[Pipeline 2] ✗ Worker4/Hint-L{} failed for {}: {}", l, qId, e.getMessage());
            }
        }

        // Worker 5: Quality Review (runs last — reviews all outputs)
        try {
            int passed = qualityWorker.review(question);
            log.debug("[Pipeline 2] ✓ Worker5/QualityReview passed={} for {}", passed, qId);
        } catch (Exception e) {
            log.warn("[Pipeline 2] ✗ Worker5/QualityReview failed for {}: {}", qId, e.getMessage());
        }

        log.debug("[Pipeline 2] Enrichment complete for: {}", qId);
    }
}
