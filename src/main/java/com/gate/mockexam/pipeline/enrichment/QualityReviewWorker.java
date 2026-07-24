package com.gate.mockexam.pipeline.enrichment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.ingestion.IngestedQuestionResult;
import com.gate.mockexam.service.GeminiUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v2.1 — Pipeline Pass 2: Quality Review (Verification).
 *
 * Receives the list of IngestedQuestionResult from Pass 1 (MultimodalIngestionService)
 * and asks Gemini to independently verify each answer and recalibrate confidence.
 *
 * Key contract:
 *   - Returns updated IngestedQuestionResult objects (same order as input)
 *   - May correct wrong answers (sets confirmationStatus = CORRECTED)
 *   - Always returns at least the Pass-1 result if verification call fails
 */
@Component
@Slf4j
public class QualityReviewWorker {

    private final String apiKey;
    private final String solveModel;
    private final ObjectMapper objectMapper;
    private final GeminiUsageService geminiUsageService;
    private final RestClient restClient;

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com";

    public QualityReviewWorker(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model.solve:gemini-3.5-flash}") String solveModel,
            ObjectMapper objectMapper,
            GeminiUsageService geminiUsageService) {
        this.apiKey = apiKey.trim();
        this.solveModel = solveModel;
        this.objectMapper = objectMapper;
        this.geminiUsageService = geminiUsageService;

        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(300_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Verifies Pass-1 results for a batch of questions.
     * Returns updated results with recalibrated confidence scores.
     */
    public List<IngestedQuestionResult> verify(List<IngestedQuestionResult> pass1Results) {
        List<IngestedQuestionResult> successful = pass1Results.stream()
                .filter(IngestedQuestionResult::isSuccess)
                .collect(Collectors.toList());

        if (successful.isEmpty()) {
            log.warn("[QualityReview] No successful Pass-1 results to verify — skipping Pass 2");
            return pass1Results;
        }

        log.info("[QualityReview] Verifying {} questions (Pass 2)", successful.size());
        geminiUsageService.checkDailyLimit();

        try {
            String prompt = buildVerifyPrompt(successful);
            String rawJson = callGemini(prompt);
            Map<Integer, IngestedQuestionResult> corrections = parseVerifyResponse(rawJson, successful);

            // Merge corrections into pass1Results
            return pass1Results.stream().map(r -> {
                if (r.isSuccess() && r.getQuestionNumber() != null
                        && corrections.containsKey(r.getQuestionNumber())) {
                    return corrections.get(r.getQuestionNumber());
                }
                return r;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[QualityReview] Pass 2 failed — using Pass-1 results as-is: {}", e.getMessage());
            return pass1Results; // Graceful degradation
        }
    }

    private String buildVerifyPrompt(List<IngestedQuestionResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are a GATE exam answer verification expert.
                Below are question numbers, proposed answers, and explanations from an AI solver.
                
                For each question:
                1. Independently verify the proposed answer using first principles.
                2. If CORRECT: set confirmationStatus = "CONFIRMED", keep the answer.
                3. If WRONG: set confirmationStatus = "CORRECTED", provide the correct answer.
                4. Recalibrate the confidenceScore based on your independent verification.
                
                Return ONLY a valid JSON array — no markdown, no fences.
                One element per question in the SAME ORDER as the input.
                
                Schema per element:
                {
                  "questionNumber": <integer>,
                  "confirmationStatus": "CONFIRMED | CORRECTED",
                  "correctAnswer": "<verified answer>",
                  "confidenceScore": <number 0.0-1.0>,
                  "verifierNote": "<brief note if corrected, else empty string>"
                }
                
                Questions to verify:
                """);

        for (IngestedQuestionResult r : results) {
            sb.append("\n--- Q").append(r.getQuestionNumber())
              .append(" | Type: ").append(r.getQuestionType())
              .append(" ---");
            sb.append("\nQuestion: ").append(
                    r.getQuestionText() != null && r.getQuestionText().length() > 400
                            ? r.getQuestionText().substring(0, 400) + "..."
                            : r.getQuestionText());
            if (r.getOptions() != null && !r.getOptions().isEmpty()) {
                r.getOptions().forEach(o ->
                        sb.append("\n  ").append(o.getLabel()).append(") ").append(o.getText()));
            }
            sb.append("\nProposed answer: ").append(r.getCorrectAnswer());
            sb.append("\nPass-1 confidence: ").append(r.getConfidenceScore());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String callGemini(String prompt) {
        String url = GEMINI_API_BASE + "/v1beta/models/" + solveModel + ":generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1,
                        "maxOutputTokens", 16384
                )
        );
        try {
            String requestJson = objectMapper.writeValueAsString(body);
            String response = restClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
            JsonNode usage = root.path("usageMetadata");
            if (!usage.isMissingNode()) {
                geminiUsageService.recordUsage("QUALITY_VERIFY",
                        usage.path("promptTokenCount").asInt(0),
                        usage.path("candidatesTokenCount").asInt(0));
            }
            return text;
        } catch (Exception e) {
            throw new RuntimeException("Gemini verify call failed: " + e.getMessage(), e);
        }
    }

    private Map<Integer, IngestedQuestionResult> parseVerifyResponse(
            String rawJson, List<IngestedQuestionResult> originals) {
        try {
            String cleaned = rawJson.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }
            List<Map<String, Object>> parsed = objectMapper.readValue(cleaned,
                    new TypeReference<>() {});

            Map<Integer, IngestedQuestionResult> origByNum = originals.stream()
                    .collect(Collectors.toMap(
                            IngestedQuestionResult::getQuestionNumber,
                            r -> r, (a, b) -> a));

            Map<Integer, IngestedQuestionResult> corrected = new LinkedHashMap<>();
            for (Map<String, Object> item : parsed) {
                int qNum = item.get("questionNumber") instanceof Number n ? n.intValue() : 0;
                if (qNum == 0 || !origByNum.containsKey(qNum)) continue;

                IngestedQuestionResult orig = origByNum.get(qNum);
                String newAnswer = item.get("correctAnswer") instanceof String s && !s.isBlank()
                        ? s : orig.getCorrectAnswer();
                double newConf = item.get("confidenceScore") instanceof Number n
                        ? n.doubleValue() : orig.getConfidenceScore().doubleValue();
                String status = (String) item.getOrDefault("confirmationStatus", "CONFIRMED");

                if ("CORRECTED".equals(status)) {
                    log.info("[QualityReview] ✏️ Corrected Q{}: {} → {}", qNum, orig.getCorrectAnswer(), newAnswer);
                }

                // Build updated result (preserve all Pass-1 enrichment fields)
                IngestedQuestionResult updated = IngestedQuestionResult.builder()
                        .questionId(orig.getQuestionId())
                        .success(true)
                        .questionNumber(orig.getQuestionNumber())
                        .section(orig.getSection())
                        .questionType(orig.getQuestionType())
                        .marks(orig.getMarks())
                        .negativeMarks(orig.getNegativeMarks())
                        .questionText(orig.getQuestionText())
                        .options(orig.getOptions())
                        .correctAnswer(newAnswer)
                        .confidenceScore(BigDecimal.valueOf(newConf).setScale(2, java.math.RoundingMode.HALF_UP))
                        .explanation(orig.getExplanation())
                        .difficulty(orig.getDifficulty())
                        .bloomsLevel(orig.getBloomsLevel())
                        .subject(orig.getSubject())
                        .topic(orig.getTopic())
                        .subtopic(orig.getSubtopic())
                        .estimatedSolveTimeSecs(orig.getEstimatedSolveTimeSecs())
                        .prerequisites(orig.getPrerequisites())
                        .hintTier1(orig.getHintTier1())
                        .hintTier2(orig.getHintTier2())
                        .hintTier3(orig.getHintTier3())
                        .hasDiagram(orig.isHasDiagram())
                        .diagramBoundingBox(orig.getDiagramBoundingBox())
                        .build();
                corrected.put(qNum, updated);
            }
            return corrected;
        } catch (Exception e) {
            log.error("[QualityReview] Failed to parse verify response: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
