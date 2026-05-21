package com.gate.mockexam.controller;

import com.gate.mockexam.dto.MockTestSummaryDto;
import com.gate.mockexam.entity.Branch;
import com.gate.mockexam.repository.BranchRepository;
import com.gate.mockexam.service.MockTestService;
import com.gate.mockexam.service.RagIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminPortalApiController {

    private final MockTestService mockTestService;
    private final RagIngestionService ragIngestionService;
    private final BranchRepository branchRepository;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    public AdminPortalApiController(
            MockTestService mockTestService,
            RagIngestionService ragIngestionService,
            BranchRepository branchRepository) {
        this.mockTestService = mockTestService;
        this.ragIngestionService = ragIngestionService;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<MockTestSummaryDto> tests = mockTestService.getAllTests();
            long publishedCount = tests.stream().filter(MockTestSummaryDto::isPublished).count();

            Map<String, Object> response = new HashMap<>();
            response.put("testsCount", tests.size());
            response.put("publishedCount", publishedCount);
            response.put("vectorCount", ragIngestionService.getVectorCount());
            response.put("storePath", vectorStorePath);
            response.put("tests", tests);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load admin dashboard REST data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading admin dashboard: " + e.getMessage());
        }
    }

    @GetMapping("/branches")
    public ResponseEntity<?> getBranches(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<Branch> branches = branchRepository.findAll();
            List<Map<String, Object>> response = branches.stream().map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", b.getId().toString());
                map.put("name", b.getName());
                map.put("code", b.getCode());

                List<Map<String, Object>> subjectsList = b.getSubjects().stream().map(s -> {
                    Map<String, Object> sMap = new HashMap<>();
                    sMap.put("id", s.getId().toString());
                    sMap.put("name", s.getName());
                    return sMap;
                }).collect(Collectors.toList());

                map.put("subjects", subjectsList);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch branches via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error fetching branches: " + e.getMessage());
        }
    }

    @PostMapping("/tests/{id}/publish")
    public ResponseEntity<?> publishTest(@PathVariable("id") UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            mockTestService.publishTest(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test published successfully."));
        } catch (Exception e) {
            log.error("Failed to publish test via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Publish failed: " + e.getMessage());
        }
    }

    @PostMapping("/tests/{id}/unpublish")
    public ResponseEntity<?> unpublishTest(@PathVariable("id") UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            mockTestService.unpublishTest(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test unpublished successfully."));
        } catch (Exception e) {
            log.error("Failed to unpublish test via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Unpublish failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable("id") UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            mockTestService.deleteTest(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test deleted successfully."));
        } catch (Exception e) {
            log.error("Failed to delete test via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Delete failed: " + e.getMessage());
        }
    }

    @GetMapping("/rag/status")
    public ResponseEntity<?> getRagStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            return ResponseEntity.ok(Map.of(
                    "vectorCount", ragIngestionService.getVectorCount(),
                    "storePath", vectorStorePath
            ));
        } catch (Exception e) {
            log.error("Failed to fetch RAG status via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to fetch RAG status: " + e.getMessage());
        }
    }

    @PostMapping("/rag/reingest")
    public ResponseEntity<?> reingestSeedQuestions(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            int count = ragIngestionService.ingestSeedQuestions();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Re-ingested " + count + " questions successfully."));
        } catch (Exception e) {
            log.error("Failed to reingest seed questions via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Re-ingestion failed: " + e.getMessage());
        }
    }
}
