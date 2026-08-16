package com.gate.mockexam.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.service.MockPaperGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the SSE endpoint for mock paper generation progress streaming.
 *
 * Flow:
 *   WeightedGenerator (FE) → navigates to /admin/generate/progress?... query params
 *   SseProgressCompiler (FE) opens EventSource to this endpoint
 *   This controller returns SseEmitter; MockPaperGenerationService emits
 *   progress/complete/error events back through it.
 */
@RestController
@RequestMapping("/admin/tests")
@Slf4j
@RequiredArgsConstructor
public class MockPaperController {

    private final MockPaperGenerationService mockPaperGenerationService;
    private final ObjectMapper objectMapper;

    /**
     * GET /admin/tests/generate/progress/weighted
     *
     * SSE endpoint — streams generation progress as JSON events.
     * Matches what SseProgressCompiler.jsx expects:
     *   - progress: {"message": "..."} or plain string
     *   - complete: "/admin/tests/{paperId}"
     *   - error: error message string
     *
     * Query params (from WeightedGenerator):
     *   branchCode      — e.g. "CS"
     *   yearLabel       — e.g. "2026 Practice Paper"
     *   weightagesJson  — JSON string, e.g. {"Data Structures": 9, ...}
     */
    @GetMapping(value = "/generate/progress/weighted", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateWeightedProgress(
            @RequestParam String branchCode,
            @RequestParam String yearLabel,
            @RequestParam String weightagesJson,
            Principal principal) {

        if (principal == null) {
            // Return a zero-emitter that immediately completes — security filter should
            // have already rejected the request, but handle gracefully
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }

        // 10-minute timeout — paper generation can be slow (multiple Gemini calls)
        SseEmitter emitter = new SseEmitter(600_000L);

        emitter.onCompletion(() -> log.debug("SSE completed for branch={}", branchCode));
        emitter.onTimeout(() -> log.debug("SSE timed out for branch={}", branchCode));
        emitter.onError(e -> log.debug("SSE error for branch={}: {}", branchCode, e.getMessage()));

        // Parse weightages JSON
        Map<String, Integer> weightages;
        try {
            weightages = objectMapper.readValue(weightagesJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.error("Failed to parse weightages JSON: {}", weightagesJson, e);
            try {
                emitter.send(SseEmitter.event().name("error").data("Invalid weightages JSON: " + e.getMessage()));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // Kick off async generation — runs on pipelineExecutor
        CompletableFuture<Void> task = mockPaperGenerationService.generateMockPaperAsync(
                branchCode, yearLabel, weightages, emitter);

        return emitter;
    }
}
