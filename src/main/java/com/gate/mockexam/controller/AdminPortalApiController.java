package com.gate.mockexam.controller;

import com.gate.mockexam.pipeline.domain.Paper;
import com.gate.mockexam.pipeline.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v2.1 Admin Portal REST Controller.
 * Restores dashboard stats and configuration endpoints to work with
 * the new Paper and GateQuestion models.
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminPortalApiController {

    private final PaperRepository paperRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.solve:gemini-3.5-flash}")
    private String solveModel;

    @Value("${gate.rag.vector-store-path:gate_vector_store}")
    private String vectorStorePath;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<Paper> papers = paperRepository.findAllByOrderByYearDesc();
            long publishedCount = papers.stream()
                    .filter(p -> "Published".equalsIgnoreCase(p.getStatus()))
                    .count();

            // Safe fetch count from gate_vector_store
            long vectorCount = 0;
            try {
                Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM gate_vector_store", Long.class);
                if (count != null) {
                    vectorCount = count;
                }
            } catch (Exception e) {
                log.warn("Failed to get vector store count: {}", e.getMessage());
            }

            // Map Paper objects to MockTestSummary DTOs expected by the frontend
            List<Map<String, Object>> testsList = papers.stream().map(p -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", p.getPaperId());
                map.put("title", p.getExamName());
                map.put("topic", p.getBranch());
                map.put("subject", String.valueOf(p.getYear()));
                map.put("durationMinutes", p.getDuration() != null ? p.getDuration() : 180);
                map.put("totalMarks", p.getTotalMarks());
                map.put("isPublished", "Published".equalsIgnoreCase(p.getStatus()));
                map.put("published", "Published".equalsIgnoreCase(p.getStatus()));
                map.put("branch", p.getBranch());
                map.put("yearLabel", String.valueOf(p.getYear()));
                map.put("createdAt", p.getUploadedAt());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("testsCount", papers.size());
            response.put("publishedCount", publishedCount);
            response.put("vectorCount", vectorCount);
            response.put("storePath", vectorStorePath);
            response.put("tests", testsList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load admin dashboard REST data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading admin dashboard: " + e.getMessage());
        }
    }

    @GetMapping("/gemini-status")
    public ResponseEntity<Map<String, Object>> geminiStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        boolean keySet = apiKey != null && !apiKey.trim().equals("placeholder") && !apiKey.trim().isEmpty();
        return ResponseEntity.ok(Map.of(
            "connected", keySet,
            "model", solveModel,
            "apiKeySet", keySet
        ));
    }

    @PutMapping("/mock-tests/{id}/publish")
    public ResponseEntity<?> publishMockTest(@PathVariable("id") String id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            return paperRepository.findById(id).map(paper -> {
                paper.setStatus("Published");
                paperRepository.save(paper);

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", paper.getPaperId());
                map.put("title", paper.getExamName());
                map.put("topic", paper.getBranch());
                map.put("subject", String.valueOf(paper.getYear()));
                map.put("durationMinutes", paper.getDuration() != null ? paper.getDuration() : 180);
                map.put("totalMarks", paper.getTotalMarks());
                map.put("isPublished", true);
                map.put("published", true);
                map.put("branch", paper.getBranch());
                map.put("yearLabel", String.valueOf(paper.getYear()));
                map.put("createdAt", paper.getUploadedAt());

                return ResponseEntity.ok(map);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to publish mock test: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error publishing test: " + e.getMessage());
        }
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable("id") String id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            if (!paperRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            paperRepository.deleteById(id);
            log.info("[API] Deleted paper: {} via dashboard by user={}", id, principal.getName());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test deleted successfully."));
        } catch (Exception e) {
            log.error("Failed to delete test via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Delete failed: " + e.getMessage());
        }
    }

    @PostMapping("/rag/reingest")
    public ResponseEntity<?> reingest(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(Map.of(
            "status", "success", 
            "message", "RAG model index is active and managed by MultimodalIngestionService."
        ));
    }
}
