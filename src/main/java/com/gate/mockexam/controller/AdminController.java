package com.gate.mockexam.controller;

import com.gate.mockexam.dto.MockTestGenerationRequestDto;
import com.gate.mockexam.dto.MockTestSummaryDto;
import com.gate.mockexam.entity.Branch;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.repository.BranchRepository;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.MockTestService;
import com.gate.mockexam.service.RagIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

// @Controller
// @RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final MockTestService mockTestService;
    private final RagIngestionService ragIngestionService;
    private final MockTestGenerationService generationService;
    private final BranchRepository branchRepository;
    private final ObjectMapper objectMapper;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    public AdminController(MockTestService mockTestService,
                           RagIngestionService ragIngestionService,
                           MockTestGenerationService generationService,
                           BranchRepository branchRepository,
                           ObjectMapper objectMapper) {
        this.mockTestService = mockTestService;
        this.ragIngestionService = ragIngestionService;
        this.generationService = generationService;
        this.branchRepository = branchRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        List<MockTestSummaryDto> tests = mockTestService.getAllTests();
        model.addAttribute("testsCount", tests.size());
        model.addAttribute("publishedCount", tests.stream().filter(MockTestSummaryDto::isPublished).count());
        
        // Expose RAG details to the template directly
        model.addAttribute("vectorCount", ragIngestionService.getVectorCount());
        model.addAttribute("storePath", vectorStorePath);
        
        return "admin/dashboard";
    }

    @GetMapping("/tests")
    public String viewTests(Model model) {
        model.addAttribute("pageTitle", "Manage Tests");
        model.addAttribute("tests", mockTestService.getAllTests());
        return "admin/tests";
    }

    // GET /admin/tests/generate — show the form (loaded from DB)
    @GetMapping("/tests/generate")
    @org.springframework.transaction.annotation.Transactional
    public String generateForm(Model model) {
        model.addAttribute("pageTitle", "AI Exam Generator");
        model.addAttribute("request", new MockTestGenerationRequestDto());
        List<Branch> branches = branchRepository.findAll();
        // Eager load the subjects collection for Thymeleaf rendering
        for (Branch b : branches) {
            b.getSubjects().size();
        }
        model.addAttribute("branches", branches);
        // Default to first branch (CSE)
        if (!branches.isEmpty()) {
            model.addAttribute("defaultBranch", branches.get(0));
        }
        return "admin/generate";
    }


    // POST /admin/tests/generate — trigger AI custom-topic generation
    @PostMapping("/tests/generate")
    public String generate(@Valid @ModelAttribute("request") MockTestGenerationRequestDto request,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "AI Exam Generator");
            model.addAttribute("branches", branchRepository.findAll());
            return "admin/generate";
        }
        try {
            MockTest test = generationService.generateAndSaveTest(request);
            redirectAttributes.addFlashAttribute("success",
                "Test generated: " + test.getTitle() + " (" + test.getQuestions().size() + " questions)");
            return "redirect:/admin/tests/" + test.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Generation failed: " + e.getMessage());
            return "redirect:/admin/tests/generate";
        }
    }

    // POST /admin/tests/generate/weighted — trigger full weighted-syllabus generation
    @PostMapping("/tests/generate/weighted")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateWeighted(
            @RequestParam String branchCode,
            @RequestParam String yearLabel,
            @RequestBody Map<String, Integer> subjectWeightages,
            RedirectAttributes redirectAttributes) {
        // subjectWeightages: { subjectName -> allocatedMarks }
        try {
            MockTest test = generationService.generateWeightedGatePaper(branchCode, yearLabel, subjectWeightages, msg -> {});
            return ResponseEntity.ok(Map.of("redirectUrl", "/admin/tests/" + test.getId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /admin/tests/{id} — view generated test with all questions
    @GetMapping("/tests/{id}")
    public String viewTest(@PathVariable UUID id, Model model) {
        MockTest test = mockTestService.getTestById(id);
        if (test == null) {
            return "redirect:/admin/tests";
        }
        model.addAttribute("pageTitle", test.getTitle());
        model.addAttribute("test", test);
        return "admin/test-detail";
    }

    // POST /admin/tests/{id}/publish — make test visible to students
    @PostMapping("/tests/{id}/publish")
    public String publishTest(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            mockTestService.publishTest(id);
            ra.addFlashAttribute("success", "Test published successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Publish failed: " + e.getMessage());
        }
        return "redirect:/admin/tests/" + id;
    }

    // GET /admin/rag/status — returns count of embedded docs
    @GetMapping("/rag/status")
    @ResponseBody
    public Map<String, Object> ragStatus() {
        return Map.of(
            "vectorCount", ragIngestionService.getVectorCount(),
            "storePath", vectorStorePath
        );
    }

    // POST /admin/rag/reingest — force re-embed all seed questions
    @PostMapping("/rag/reingest")
    public String reingest(RedirectAttributes redirectAttributes) {
        try {
            int count = ragIngestionService.ingestSeedQuestions();
            redirectAttributes.addFlashAttribute("success", "Re-ingested " + count + " questions.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ingestion failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // GET /admin/tests/generate/progress — Asynchronous full 65-question RAG paper compiler streaming
    @GetMapping("/tests/generate/progress")
    public SseEmitter streamFullPaperGeneration() {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes timeout
        
        CompletableFuture.runAsync(() -> {
            try {
                MockTest test = generationService.generateFullGateCsePaper(progressJson -> {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(progressJson));
                    } catch (IOException e) {
                        log.error("Failed to send SSE progress", e);
                    }
                });

                 // Send success progress message
                 emitter.send(SseEmitter.event()
                     .name("progress")
                     .data("{\"step\": 6, \"message\": \"[Success] Mock test saved to database.\", \"percent\": 100}"));

                 // Send final completion message with redirect url
                 emitter.send(SseEmitter.event()
                     .name("complete")
                     .data("/admin/tests/" + test.getId()));
                 emitter.complete();
 
             } catch (Exception e) {
                 log.error("AI Paper compilation failed asynchronously", e);
                 try {
                     String errMsg = getSseErrorMessage(e);
                     emitter.send(SseEmitter.event()
                         .name("progress")
                         .data(String.format("{\"step\": 6, \"message\": \"[Error] %s\", \"percent\": 100}", errMsg)));
                     emitter.send(SseEmitter.event()
                         .name("error")
                         .data(errMsg));
                 } catch (IOException ioException) {
                     log.error("Failed to send SSE error notification", ioException);
                 }
                 emitter.completeWithError(e);
             }
         });
 
         return emitter;
     }
 
     // GET /admin/tests/generate/progress/weighted — Asynchronous custom dynamic weightage compiler streaming
     @GetMapping("/tests/generate/progress/weighted")
     public SseEmitter streamWeightedPaperGeneration(
             @RequestParam String branchCode,
             @RequestParam String yearLabel,
             @RequestParam String weightagesJson) {
         SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes timeout
         
         CompletableFuture.runAsync(() -> {
             try {
                 // Parse weightagesJson into Map<String, Integer>
                 java.util.Map<String, Integer> subjectWeightages = new java.util.LinkedHashMap<>();
                 try {
                     subjectWeightages = objectMapper.readValue(weightagesJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Integer>>() {});
                 } catch (Exception parseEx) {
                     log.error("Failed to parse weightages JSON: {}", weightagesJson, parseEx);
                     // Fallback parse if it's sent as a flat string key:val,key:val
                     String[] pairs = weightagesJson.split(",");
                     for (String pair : pairs) {
                         String[] kv = pair.split(":");
                         if (kv.length == 2) {
                             subjectWeightages.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                         }
                     }
                 }
 
                 MockTest test = generationService.generateWeightedGatePaper(branchCode, yearLabel, subjectWeightages, progressJson -> {
                     try {
                         emitter.send(SseEmitter.event()
                             .name("progress")
                             .data(progressJson));
                     } catch (IOException e) {
                         log.error("Failed to send SSE progress", e);
                     }
                 });
 
                 // Send success progress message
                 emitter.send(SseEmitter.event()
                     .name("progress")
                     .data("{\"step\": 6, \"message\": \"[Success] Mock test saved to database.\", \"percent\": 100}"));

                 // Send final completion message with redirect url
                 emitter.send(SseEmitter.event()
                     .name("complete")
                     .data("/admin/tests/" + test.getId()));
                 emitter.complete();
 
             } catch (Exception e) {
                 log.error("AI Weighted Paper compilation failed asynchronously", e);
                 try {
                     String errMsg = getSseErrorMessage(e);
                     emitter.send(SseEmitter.event()
                         .name("progress")
                         .data(String.format("{\"step\": 6, \"message\": \"[Error] %s\", \"percent\": 100}", errMsg)));
                     emitter.send(SseEmitter.event()
                         .name("error")
                         .data(errMsg));
                 } catch (IOException ioException) {
                     log.error("Failed to send SSE error notification", ioException);
                 }
                 emitter.completeWithError(e);
             }
         });
 
         return emitter;
     }

     private String getSseErrorMessage(Throwable e) {
         Throwable root = e;
         while (root.getCause() != null && root != root.getCause()) {
             root = root.getCause();
         }
         
         if (root instanceof com.gate.mockexam.exception.QuotaExceededException) {
             return "Daily Gemini token quota reached. Resets at midnight IST.";
         }
         if (root instanceof IllegalStateException && (
             "GEMINI_API_KEY not configured. Set it in your environment variables.".equals(root.getMessage()) ||
             root.getMessage().contains("api.key") ||
             root.getMessage().contains("API key") ||
             root.getMessage().contains("GEMINI_API_KEY")
         )) {
             return "GEMINI_API_KEY not configured. Set it in your environment variables.";
         }
         if (root instanceof java.io.IOException || 
             root instanceof java.net.SocketTimeoutException || 
             root instanceof java.net.ConnectException || 
             root.getMessage().contains("timed out") || 
             root.getMessage().contains("Connection closed") || 
             root.getMessage().contains("Timeout") || 
             root.getMessage().contains("ConnectException")
         ) {
             return "Gemini API call timed out. Check your internet connection and retry.";
         }
         if (root instanceof com.fasterxml.jackson.core.JsonProcessingException || 
             root.getMessage().contains("invalid JSON") || 
             root.getMessage().contains("JsonProcessingException")
         ) {
             return "Gemini returned invalid JSON. The prompt may need adjustment. Retrying...";
         }
         return "Generation failed: " + root.getMessage();
     }
 }

