package com.gate.mockexam.pipeline.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.config.MinioStorageService;
import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.extraction.dto.*;
import com.gate.mockexam.pipeline.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pipeline 1 — Document Extraction Pipeline.
 *
 * Orchestrates both sub-pipelines:
 *   A) Question Paper Extraction (calls Python /extract/question-paper)
 *   B) Answer Key Extraction     (calls Python /extract/answer-key)
 *
 * After both complete, merges results, validates, and persists to PostgreSQL.
 * MinIO uploads are handled by the Python service; this service only stores the references.
 *
 * NO AI-generated information is produced here. This is the source-of-truth pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionPipelineService {

    private final PaperRepository paperRepository;
    private final GateQuestionRepository questionRepository;
    private final GateOptionRepository optionRepository;
    private final AssetRepository assetRepository;
    private final QuestionValidator questionValidator;
    private final ObjectMapper objectMapper;

    @Value("${extraction.service.url}")
    private String extractionServiceUrl;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(extractionServiceUrl)
                .build();
    }

    /**
     * Main entry point for Pipeline 1.
     * Runs Sub-Pipeline A and B in parallel (using CompletableFuture),
     * then merges and persists results.
     *
     * @param paperId         Unique ID for this paper (e.g., "gate_cse_2020")
     * @param examName        e.g., "GATE CSE 2020"
     * @param year            exam year
     * @param branch          e.g., "CSE"
     * @param questionPdfBytes raw bytes of the question paper PDF
     * @param answerKeyBytes   raw bytes of the answer key PDF (may be null)
     * @return the created Paper entity
     */
    @Transactional
    public Paper runPipeline(
            String paperId,
            String examName,
            int year,
            String branch,
            byte[] questionPdfBytes,
            byte[] answerKeyBytes) {

        log.info("=== PIPELINE 1 START: paperId={} examName={} year={} ===", paperId, examName, year);

        // ── Step 1: Sub-Pipeline A — Extract question paper ─────────────────
        log.info("[Pipeline 1A] Calling Python extraction service for question paper...");
        ExtractedPaperDto extractedPaper = callExtractQuestionPaper(paperId, questionPdfBytes);
        log.info("[Pipeline 1A] Extracted {} questions from paper", extractedPaper.getQuestions().size());

        // ── Step 2: Sub-Pipeline B — Extract answer key ──────────────────────
        ExtractedAnswerKeyDto answerKey = null;
        if (answerKeyBytes != null && answerKeyBytes.length > 0) {
            log.info("[Pipeline 1B] Calling Python extraction service for answer key...");
            answerKey = callExtractAnswerKey(paperId, answerKeyBytes);
            log.info("[Pipeline 1B] Extracted {} answers from key", answerKey.getTotalParsed());
        } else {
            log.info("[Pipeline 1B] No answer key provided; skipping Sub-Pipeline B.");
        }

        // ── Step 3: Merge — Attach official answers to questions ─────────────
        Map<String, String> answerMap = buildAnswerMap(answerKey);
        log.info("[Merge] Merging {} questions with {} official answers", extractedPaper.getQuestions().size(), answerMap.size());

        // ── Step 4: Create Paper entity ──────────────────────────────────────
        Paper paper = Paper.builder()
                .paperId(paperId)
                .examName(examName)
                .year(year)
                .branch(branch)
                .session(extractedPaper.getSession())
                .duration(extractedPaper.getDuration())
                .totalMarks(extractedPaper.getTotalMarks() != null ? BigDecimal.valueOf(extractedPaper.getTotalMarks()) : null)
                .totalQuestions(extractedPaper.getQuestions().size())
                .paperType("Official")
                .status("Extracted")
                .uploadedAt(LocalDateTime.now())
                .build();
        paperRepository.save(paper);
        log.info("[DB] Saved paper: {}", paperId);

        // ── Step 5: Validate, then persist each question ─────────────────────
        int savedCount = 0;
        int failedCount = 0;

        for (ExtractedQuestionDto qDto : extractedPaper.getQuestions()) {
            try {
                // Attach correct answer from merged answer map
                String correctAnswer = answerMap.get(String.valueOf(qDto.getQuestionNumber()));

                // Validate
                List<String> violations = questionValidator.validate(qDto, correctAnswer);
                if (!violations.isEmpty()) {
                    log.warn("[Validation] Question {} FAILED validation: {}", qDto.getQuestionNumber(), violations);
                    failedCount++;
                    continue; // Skip invalid questions (they go to manual review)
                }

                // Persist question
                String questionId = paperId + "_Q" + qDto.getQuestionNumber();
                GateQuestion question = GateQuestion.builder()
                        .questionId(questionId)
                        .paper(paper)
                        .questionNumber(qDto.getQuestionNumber())
                        .section(qDto.getSection())
                        .questionType(qDto.getQuestionType())
                        .marks(qDto.getMarks() != null ? BigDecimal.valueOf(qDto.getMarks()) : null)
                        .negativeMarks(qDto.getNegativeMarks() != null ? BigDecimal.valueOf(qDto.getNegativeMarks()) : BigDecimal.ZERO)
                        .questionText(qDto.getQuestionText())
                        .correctAnswer(correctAnswer)
                        .build();
                questionRepository.save(question);

                // Persist options
                if (qDto.getOptions() != null) {
                    for (int i = 0; i < qDto.getOptions().size(); i++) {
                        var optDto = qDto.getOptions().get(i);
                        String optionId = questionId + "_" + optDto.getLabel();
                        GateOption option = GateOption.builder()
                                .optionId(optionId)
                                .question(question)
                                .label(optDto.getLabel() != null && !optDto.getLabel().isEmpty()
                                        ? optDto.getLabel().charAt(0) : (char)('A' + i))
                                .optionText(optDto.getOptionText() != null ? optDto.getOptionText() : "")
                                .displayOrder(i + 1)
                                .build();
                        optionRepository.save(option);

                        // Persist option-level assets
                        if (optDto.getAssets() != null) {
                            persistAssets(optDto.getAssets(), question, option);
                        }
                    }
                }

                // Persist question-level assets
                if (qDto.getAssets() != null) {
                    persistAssets(qDto.getAssets(), question, null);
                }

                savedCount++;
            } catch (Exception e) {
                log.error("[Error] Failed to persist question {}: {}", qDto.getQuestionNumber(), e.getMessage(), e);
                failedCount++;
            }
        }

        // ── Step 6: Update paper status ──────────────────────────────────────
        paper.setTotalQuestions(savedCount);
        paper.setStatus(failedCount == 0 ? "Validated" : "PartiallyValidated");
        paperRepository.save(paper);

        log.info("=== PIPELINE 1 COMPLETE: paperId={} saved={} failed={} status={} ===",
                paperId, savedCount, failedCount, paper.getStatus());
        return paper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-Pipeline A: Call Python /extract/question-paper
    // ─────────────────────────────────────────────────────────────────────────

    private ExtractedPaperDto callExtractQuestionPaper(String paperId, byte[] pdfBytes) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(pdfBytes) {
                @Override public String getFilename() { return "question_paper.pdf"; }
            });
            body.add("paper_id", paperId);

            ResponseEntity<String> response = restClient().post()
                    .uri("/extract/question-paper")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), ExtractedPaperDto.class);
            }
            throw new RuntimeException("Python extractor returned status: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("[Pipeline 1A] Failed to call Python question-paper extractor: {}", e.getMessage(), e);
            throw new RuntimeException("Question paper extraction failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-Pipeline B: Call Python /extract/answer-key
    // ─────────────────────────────────────────────────────────────────────────

    private ExtractedAnswerKeyDto callExtractAnswerKey(String paperId, byte[] answerKeyBytes) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(answerKeyBytes) {
                @Override public String getFilename() { return "answer_key.pdf"; }
            });
            body.add("paper_id", paperId);

            ResponseEntity<String> response = restClient().post()
                    .uri("/extract/answer-key")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), ExtractedAnswerKeyDto.class);
            }
            throw new RuntimeException("Python extractor returned status: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("[Pipeline 1B] Failed to call Python answer-key extractor: {}", e.getMessage(), e);
            throw new RuntimeException("Answer key extraction failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: build answer map from ExtractedAnswerKeyDto
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, String> buildAnswerMap(ExtractedAnswerKeyDto answerKey) {
        if (answerKey == null || answerKey.getAnswers() == null) {
            return Collections.emptyMap();
        }
        return answerKey.getAnswers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: persist asset references (from Python-uploaded MinIO objects)
    // ─────────────────────────────────────────────────────────────────────────

    private void persistAssets(List<ExtractedAssetDto> assetDtos, GateQuestion question, GateOption option) {
        for (ExtractedAssetDto assetDto : assetDtos) {
            try {
                String assetId = UUID.randomUUID().toString();
                Asset asset = Asset.builder()
                        .assetId(assetId)
                        .question(question)
                        .option(option)
                        .assetType(assetDto.getAssetType() != null ? assetDto.getAssetType() : "Image")
                        .bucketName("gate-assets")
                        .objectKey(assetDto.getObjectKey())
                        .mimeType(assetDto.getMimeType())
                        .width(assetDto.getWidth())
                        .height(assetDto.getHeight())
                        .checksum(assetDto.getChecksum())
                        .build();
                assetRepository.save(asset);
            } catch (Exception e) {
                log.warn("[Assets] Failed to persist asset reference {}: {}", assetDto.getObjectKey(), e.getMessage());
            }
        }
    }
}
