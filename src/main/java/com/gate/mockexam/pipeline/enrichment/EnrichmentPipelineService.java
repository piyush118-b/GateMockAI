package com.gate.mockexam.pipeline.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.domain.AiArtifact;
import com.gate.mockexam.pipeline.domain.GateQuestion;
import com.gate.mockexam.pipeline.domain.Paper;
import com.gate.mockexam.pipeline.ingestion.IngestedQuestionResult;
import com.gate.mockexam.pipeline.ingestion.MultimodalIngestionService;
import com.gate.mockexam.pipeline.repository.AiArtifactRepository;
import com.gate.mockexam.pipeline.repository.GateQuestionRepository;
import com.gate.mockexam.pipeline.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * v2.1 — AI Enrichment / Quality Gate Pipeline.
 *
 * Called AFTER MultimodalIngestionService has persisted questions to DB.
 * Runs Pass 2 (QualityReviewWorker) and then the AnswerConfidenceGate
 * on batches of already-ingested questions.
 *
 * Also supports re-enrichment of existing papers via POST /api/pipeline/enrich/{id}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentPipelineService {

    private final PaperRepository paperRepository;
    private final GateQuestionRepository questionRepository;
    private final AiArtifactRepository artifactRepository;
    private final QualityReviewWorker qualityReviewWorker;
    private final AnswerConfidenceGate confidenceGate;
    private final ObjectMapper objectMapper;

    @Value("${pipeline.batch-size:10}")
    private int batchSize;

    @Value("${gemini.model.solve:gemini-3.5-flash}")
    private String solveModel;

    @Async
    public CompletableFuture<String> enrichPaperAsync(String paperId) {
        return CompletableFuture.supplyAsync(() -> enrichPaper(paperId));
    }

    public String enrichPaper(String paperId) {
        log.info("=== ENRICHMENT PIPELINE v2.1 START: paperId={} ===", paperId);

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

        List<GateQuestion> questions = questionRepository
                .findByPaperPaperIdOrderByQuestionNumberAsc(paperId);

        if (questions.isEmpty()) {
            return "No questions to enrich for paper: " + paperId;
        }

        AtomicInteger published = new AtomicInteger();
        AtomicInteger review    = new AtomicInteger();
        AtomicInteger errors    = new AtomicInteger();

        // Partition into batches for Pass-2 verification
        List<List<GateQuestion>> batches = partition(questions, batchSize);
        int batchNum = 0;
        for (List<GateQuestion> batch : batches) {
            batchNum++;
            log.info("[Enrichment] Verifying batch {}/{} ({} questions)", batchNum, batches.size(), batch.size());
            try {
                processBatch(batch, published, review, errors);
            } catch (Exception e) {
                log.error("[Enrichment] Batch {} failed: {}", batchNum, e.getMessage(), e);
                errors.addAndGet(batch.size());
            }
        }

        paper.setStatus("Enriched");
        paperRepository.save(paper);

        String summary = String.format(
                "Enrichment v2.1 COMPLETE for paper=%s | published=%d | needsReview=%d | errors=%d",
                paperId, published.get(), review.get(), errors.get());
        log.info("=== ENRICHMENT COMPLETE: {} ===", summary);
        return summary;
    }

    private void processBatch(List<GateQuestion> batch,
                               AtomicInteger published, AtomicInteger review, AtomicInteger errors) {
        // Reconstruct IngestedQuestionResult from persisted GateQuestion
        // (Pass 2 just needs questionNumber, questionType, questionText, options, correctAnswer, confidence)
        List<IngestedQuestionResult> pass1 = batch.stream().map(q -> {
            List<IngestedQuestionResult.IngestedOptionResult> opts = q.getOptions().stream()
                    .map(o -> IngestedQuestionResult.IngestedOptionResult.builder()
                            .label(String.valueOf(o.getLabel()))
                            .text(o.getOptionText())
                            .build())
                    .collect(Collectors.toList());
            return IngestedQuestionResult.builder()
                    .questionId(q.getQuestionId())
                    .success(true)
                    .questionNumber(q.getQuestionNumber())
                    .questionType(q.getQuestionType())
                    .questionText(q.getQuestionText())
                    .options(opts)
                    .correctAnswer(q.getCorrectAnswer())
                    .confidenceScore(q.getConfidenceScore())
                    .build();
        }).collect(Collectors.toList());

        // Pass 2: verify
        List<IngestedQuestionResult> pass2 = qualityReviewWorker.verify(pass1);

        Map<String, GateQuestion> qMap = batch.stream()
                .collect(Collectors.toMap(GateQuestion::getQuestionId, q -> q));

        for (IngestedQuestionResult result : pass2) {
            GateQuestion question = qMap.get(result.getQuestionId());
            if (question == null) { errors.incrementAndGet(); continue; }
            try {
                // Update correctAnswer if Pass 2 corrected it
                question.setCorrectAnswer(result.getCorrectAnswer());
                question.setConfidenceScore(result.getConfidenceScore());

                boolean pub = confidenceGate.applyGate(question, result);
                if (pub) published.incrementAndGet(); else review.incrementAndGet();

                persistEnrichedArtifact(question, result);
            } catch (Exception e) {
                log.error("[Enrichment] Gate/persist failed for {}: {}", result.getQuestionId(), e.getMessage());
                errors.incrementAndGet();
            }
        }
    }

    private void persistEnrichedArtifact(GateQuestion question, IngestedQuestionResult result) {
        try {
            long version = artifactRepository.countByQuestionQuestionIdAndArtifactType(
                    question.getQuestionId(), "ENRICHED") + 1;
            AiArtifact artifact = AiArtifact.builder()
                    .artifactId(UUID.randomUUID().toString())
                    .question(question)
                    .artifactType("ENRICHED")
                    .version((int) version)
                    .model(solveModel)
                    .status("VERIFIED")
                    .createdAt(LocalDateTime.now())
                    .build();
            artifactRepository.save(artifact);
        } catch (Exception e) {
            log.warn("[Enrichment] Failed to persist ENRICHED artifact for {}: {}",
                    question.getQuestionId(), e.getMessage());
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size)
            result.add(list.subList(i, Math.min(i + size, list.size())));
        return result;
    }
}
