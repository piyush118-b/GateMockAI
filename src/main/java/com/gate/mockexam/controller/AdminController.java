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

@RestController
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

