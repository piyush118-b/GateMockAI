package com.gate.mockexam.pipeline.controller;

import com.gate.mockexam.pipeline.domain.AdminReviewItem;
import com.gate.mockexam.pipeline.repository.AdminReviewRepository;
import com.gate.mockexam.pipeline.repository.GateQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * v2.1 — Admin REST API for the Answer Confidence Review Queue.
 *
 * GET  /api/admin/review/queue              → paginated NEEDS_REVIEW list
 * GET  /api/admin/review/queue/count        → unresolved count badge
 * POST /api/admin/review/{questionId}/approve → confirm LLM answer is correct
 * POST /api/admin/review/{questionId}/correct → override answer
 */
@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
@Slf4j
public class AdminReviewController {

    private final AdminReviewRepository reviewRepository;
    private final GateQuestionRepository questionRepository;

    @GetMapping("/queue")
    public ResponseEntity<?> getQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        Page<AdminReviewItem> items = reviewRepository.findAllUnresolved(PageRequest.of(page, size));
        var content = items.getContent().stream().map(item -> Map.of(
                "id",             item.getId(),
                "questionId",     item.getQuestion().getQuestionId(),
                "questionText",   truncate(item.getQuestion().getQuestionText(), 200),
                "questionType",   orEmpty(item.getQuestion().getQuestionType()),
                "paperId",        item.getQuestion().getPaper() != null ? item.getQuestion().getPaper().getPaperId() : "",
                "correctAnswer",  orEmpty(item.getQuestion().getCorrectAnswer()),
                "confidenceScore", item.getConfidenceScore(),
                "flaggedReason",  orEmpty(item.getFlaggedReason()),
                "flaggedAt",      item.getFlaggedAt().toString(),
                "hasDiagram",     false  // diagram URL lookup can be added later
        )).toList();
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalElements", items.getTotalElements(),
                "totalPages", items.getTotalPages(),
                "currentPage", page
        ));
    }

    @GetMapping("/queue/count")
    public ResponseEntity<?> getCount(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(Map.of("unresolvedCount", reviewRepository.countByResolvedFalse()));
    }

    @PostMapping("/{questionId}/approve")
    public ResponseEntity<?> approve(@PathVariable String questionId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        return questionRepository.findById(questionId).map(q -> {
            q.setReviewStatus("PUBLISHED");
            q.setAnswerSource("HUMAN_VERIFIED");
            questionRepository.save(q);
            resolveItem(questionId, principal.getName());
            log.info("[AdminReview] ✅ Approved {} by {}", questionId, principal.getName());
            return ResponseEntity.ok(Map.of("status", "approved", "questionId", questionId));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{questionId}/correct")
    public ResponseEntity<?> correct(
            @PathVariable String questionId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");
        String answer = body.get("correctAnswer");
        if (answer == null || answer.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "correctAnswer is required"));
        return questionRepository.findById(questionId).map(q -> {
            q.setCorrectAnswer(answer.trim());
            q.setAnswerSource("HUMAN_VERIFIED");
            q.setReviewStatus("PUBLISHED");
            questionRepository.save(q);
            resolveItem(questionId, principal.getName());
            log.info("[AdminReview] ✏️ Corrected {} → '{}' by {}", questionId, answer, principal.getName());
            return ResponseEntity.ok(Map.of("status", "corrected", "questionId", questionId, "correctAnswer", answer));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void resolveItem(String questionId, String reviewer) {
        reviewRepository.findByQuestionQuestionId(questionId).ifPresent(item -> {
            item.setResolved(true);
            item.setReviewedBy(reviewer);
            item.setReviewedAt(LocalDateTime.now());
            reviewRepository.save(item);
        });
    }

    private String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max) + "...";
    }
    private String orEmpty(String s) { return s != null ? s : ""; }
}
