package com.gate.mockexam.pipeline.controller;

import com.gate.mockexam.pipeline.domain.GateQuestion;
import com.gate.mockexam.pipeline.domain.Paper;
import com.gate.mockexam.pipeline.enrichment.EnrichmentPipelineService;
import com.gate.mockexam.pipeline.ingestion.MultimodalIngestionService;
import com.gate.mockexam.pipeline.repository.AiArtifactRepository;
import com.gate.mockexam.pipeline.repository.GateQuestionRepository;
import com.gate.mockexam.pipeline.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST API for the GATE Question Intelligence Platform pipelines.
 *
 * All endpoints are ADMIN-only (secured via SecurityConfig).
 *
 * Endpoints:
 *   POST   /api/pipeline/ingest          → Upload PDFs → trigger Pipeline 1
 *   POST   /api/pipeline/enrich/{id}     → Trigger Pipeline 2 for a paper
 *   GET    /api/pipeline/status/{id}     → Get paper pipeline status
 *   GET    /api/pipeline/papers          → List all papers
 *   GET    /api/pipeline/papers/{id}/questions → List questions for a paper
 *   GET    /api/pipeline/health          → Check Python extractor health
 */
@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
@Slf4j
public class PaperIngestionController {

    private final EnrichmentPipelineService enrichmentPipeline;
    private final MultimodalIngestionService multimodalIngestionService;
    private final PaperRepository paperRepository;
    private final GateQuestionRepository questionRepository;
    private final AiArtifactRepository artifactRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/pipeline/ingest
    // Trigger Pipeline 1: upload single question paper PDF
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingestPaper(
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("paperId")   String paperId,
            @RequestParam("examName")  String examName,
            @RequestParam("year")      int year,
            @RequestParam(value = "branch", defaultValue = "CSE") String branch,
            Principal principal) {

        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        if (paperRepository.existsById(paperId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Paper already ingested: " + paperId,
                                 "hint", "Use a different paperId or delete the existing one first."));
        }

        try {
            log.info("[API] Starting Pipeline 1 for paperId={} by user={}", paperId, principal.getName());

            byte[] questionBytes = questionPaper.getBytes();

            Paper paper = multimodalIngestionService.ingest(paperId, examName, year, branch, questionBytes);
            enrichmentPipeline.enrichPaperAsync(paperId);

            return ResponseEntity.ok(Map.of(
                    "status",        "success",
                    "message",       "Pipeline v2.1: Extraction + Enrichment started. Question text and answers derived by Gemini directly from the PDF.",
                    "paperId",       paper.getPaperId(),
                    "totalQuestions", paper.getTotalQuestions(),
                    "paperStatus",   paper.getStatus()
            ));

        } catch (Exception e) {
            log.error("[API] Pipeline 1 failed for paperId={}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Pipeline 1 failed: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/pipeline/enrich/{paperId}
    // Trigger Pipeline 2: AI enrichment (runs asynchronously)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/enrich/{paperId}")
    public ResponseEntity<?> enrichPaper(@PathVariable String paperId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        if (!paperRepository.existsById(paperId)) {
            return ResponseEntity.notFound().build();
        }

        log.info("[API] Starting Pipeline 2 (async) for paperId={} by user={}", paperId, principal.getName());

        // Run enrichment asynchronously
        CompletableFuture<String> future = enrichmentPipeline.enrichPaperAsync(paperId);

        return ResponseEntity.accepted().body(Map.of(
                "status",  "accepted",
                "message", "Pipeline 2 (AI Enrichment) started asynchronously for paperId: " + paperId,
                "checkStatus", "GET /api/pipeline/status/" + paperId
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/pipeline/status/{paperId}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/status/{paperId}")
    public ResponseEntity<?> getPaperStatus(@PathVariable String paperId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        return paperRepository.findById(paperId).map(paper -> {
            long totalQuestions = questionRepository.countByPaperPaperId(paperId);
            long verifiedArtifacts = artifactRepository.countByQuestionPaperPaperIdAndStatus(paperId, "VERIFIED");
            long generatedArtifacts = artifactRepository.countByQuestionPaperPaperIdAndStatus(paperId, "GENERATED");
            long pendingArtifacts = artifactRepository.countByQuestionPaperPaperIdAndStatus(paperId, "PENDING");
            long errorArtifacts = artifactRepository.countByQuestionPaperPaperIdAndStatus(paperId, "ERROR");

            return ResponseEntity.ok(Map.of(
                    "paperId",       paper.getPaperId(),
                    "examName",      paper.getExamName(),
                    "year",          paper.getYear(),
                    "branch",        paper.getBranch() != null ? paper.getBranch() : "N/A",
                    "status",        paper.getStatus(),
                    "totalQuestions", totalQuestions,
                    "artifacts", Map.of(
                            "verified",  verifiedArtifacts,
                            "generated", generatedArtifacts,
                            "pending",   pendingArtifacts,
                            "error",     errorArtifacts
                    )
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/pipeline/papers
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/papers")
    public ResponseEntity<?> listPapers(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Map<String, Object>> papers = paperRepository.findAllByOrderByYearDesc()
                .stream()
                .map(paper -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("paperId",        paper.getPaperId());
                    m.put("examName",       paper.getExamName());
                    m.put("year",           paper.getYear());
                    m.put("branch",         paper.getBranch());
                    m.put("session",        paper.getSession());
                    m.put("totalQuestions", paper.getTotalQuestions());
                    m.put("status",         paper.getStatus());
                    m.put("uploadedAt",     paper.getUploadedAt());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("papers", papers, "total", papers.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/pipeline/papers/{paperId}/questions
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/papers/{paperId}/questions")
    public ResponseEntity<?> listQuestions(@PathVariable String paperId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        if (!paperRepository.existsById(paperId)) {
            return ResponseEntity.notFound().build();
        }

        List<GateQuestion> questions = questionRepository
                .findByPaperPaperIdOrderByQuestionNumberAsc(paperId);

        // Manual pagination
        int start  = Math.min(page * size, questions.size());
        int end    = Math.min(start + size, questions.size());
        List<GateQuestion> paged = questions.subList(start, end);

        List<Map<String, Object>> qList = paged.stream().map(q -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("questionId",     q.getQuestionId());
            m.put("questionNumber", q.getQuestionNumber());
            m.put("section",        q.getSection());
            m.put("questionType",   q.getQuestionType());
            m.put("marks",          q.getMarks());
            m.put("negativeMarks",  q.getNegativeMarks());
            m.put("questionText",   q.getQuestionText().substring(0, Math.min(150, q.getQuestionText().length())) + "...");
            m.put("correctAnswer",  q.getCorrectAnswer());
            m.put("optionsCount",   q.getOptions() != null ? q.getOptions().size() : 0);
            m.put("assetsCount",    q.getAssets() != null ? q.getAssets().size() : 0);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "paperId",       paperId,
                "total",         questions.size(),
                "page",          page,
                "pageSize",      size,
                "questions",     qList
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/pipeline/papers/{paperId}
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/papers/{paperId}")
    public ResponseEntity<?> deletePaper(@PathVariable String paperId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        if (!paperRepository.existsById(paperId)) {
            return ResponseEntity.notFound().build();
        }

        paperRepository.deleteById(paperId);
        log.info("[API] Deleted paper: {} by user={}", paperId, principal.getName());
        return ResponseEntity.ok(Map.of("status", "deleted", "paperId", paperId));
    }
}
