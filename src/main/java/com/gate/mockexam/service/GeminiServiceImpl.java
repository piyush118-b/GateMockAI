package com.gate.mockexam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final String apiKey;
    private final String transcriptionModel;
    private final String generationModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiUsageService geminiUsageService;

    @Value("${gemini.model.max-output-tokens.ingestion:65536}")
    private int maxOutputTokensIngestion;

    @Value("${gemini.model.max-output-tokens.generation:8192}")
    private int maxOutputTokensGeneration;

    @Value("${gemini.model.temperature.ingestion:0.0}")
    private double temperatureIngestion;

    public GeminiServiceImpl(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model.transcription:gemini-2.5-flash}") String transcriptionModel,
            @Value("${gemini.model.generation:gemini-2.5-flash}") String generationModel,
            ObjectMapper objectMapper,
            GeminiUsageService geminiUsageService) {
        this.apiKey = apiKey.trim();
        this.transcriptionModel = transcriptionModel;
        this.generationModel = generationModel;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(180000); // Wait up to 3 minutes for long LLM responses
        
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
        this.geminiUsageService = geminiUsageService;
    }

    private void checkApiKey() {
        if (this.apiKey == null || this.apiKey.trim().isEmpty() || 
            this.apiKey.trim().equalsIgnoreCase("YOUR_GEMINI_API_KEY") || 
            this.apiKey.trim().equalsIgnoreCase("placeholder") || 
            this.apiKey.trim().equalsIgnoreCase("TODO")) {
            throw new IllegalStateException("GEMINI_API_KEY not configured. Set it in your environment variables.");
        }
    }

    @Override
    public String transcribePdfToQuestions(byte[] pdfBytes, String manualAnswerKey) {
        checkApiKey();
        geminiUsageService.checkDailyLimit();
        log.info("Sending entire PDF of size {} bytes to Gemini ({}) for full transcription", pdfBytes.length, transcriptionModel);

        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        String prompt = String.format("""
            You are a precise GATE exam paper digitizer. The attached PDF is an official
            GATE CSE question paper. Extract ALL questions into a JSON object containing a 'questions' array.

            Return ONLY a valid JSON object matching the schema below, no markdown, no explanation, no preamble.
            
            Schema:
            {
              "questions": [
                {
                  "sequenceNo": <number>,
                  "section": "<section: GA (General Aptitude) or CS (Computer Science)>",
                  "type": "<type: MCQ or MSQ or NAT>",
                  "questionText": "<extracted question text, including any context or equations; never truncate with placeholders>",
                  "options": [
                    {"label": "A", "text": "<text>"},
                    {"label": "B", "text": "<text>"},
                    {"label": "C", "text": "<text>"},
                    {"label": "D", "text": "<text>"}
                  ],
                  "marks": <1 or 2>,
                  "negativeMarks": <negative marks, 0.33 for 1-mark MCQ, 0.67 for 2-mark MCQ, 0.0 for MSQ and NAT>,
                  "correctNatValue": <Double numerical answer if NAT. Otherwise null.>,
                  "natTolerance": <Double tolerance range if NAT. Otherwise null.>,
                  "explanation": "Official Answer derived from key.",
                  "subject": "<Auto-classified subject name, e.g. Operating Systems>",
                  "topic": "<Auto-classified topic name, e.g. CPU Scheduling>"
                }
              ]
            }
            
            Manual Answer Key Reference (use this if provided to align the correct options or correct NAT values):
            %s
            
            Ensure:
            - For MCQ/MSQ options, set 'isCorrect' true for the correct options, e.g., {"label": "A", "text": "...", "isCorrect": true}. For incorrect options, set 'isCorrect' false.
            - Extract ALL questions written inside the PDF. Do NOT truncate or skip any questions.
            - Ensure clean valid JSON format.
            """, manualAnswerKey != null ? manualAnswerKey : "None");

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt),
                        Map.of(
                            "inlineData", Map.of(
                                "mimeType", "application/pdf",
                                "data", base64Pdf
                            )
                        )
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", temperatureIngestion,
                "maxOutputTokens", maxOutputTokensIngestion,
                "responseMimeType", "application/json"
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", transcriptionModel, apiKey);
            Map<?, ?> response = executePostWithRetry(url, requestBody);

            if (response != null && response.containsKey("candidates")) {
                recordUsageHelper("INGESTION", response);
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        String rawResponse = (String) part.get("text");
                        log.info("=== GEMINI RAW RESPONSE START ===");
                        if (rawResponse != null) {
                            log.info(rawResponse.substring(0, Math.min(rawResponse.length(), 2000)));
                            log.info("=== GEMINI RAW RESPONSE END (first 2000 chars) ===");
                            log.info("Response length: {} chars", rawResponse.length());
                            log.info("Starts with: '{}'", rawResponse.substring(0, Math.min(50, rawResponse.length())));
                            log.info("Ends with: '{}'", rawResponse.substring(Math.max(0, rawResponse.length()-100)));
                        } else {
                            log.info("rawResponse is null");
                        }
                        return rawResponse;
                    }
                }
            }
            throw new RuntimeException("Empty or invalid response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to call Gemini API for transcription: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini transcription failed: " + e.getMessage());
        }
    }

    @Override
    public String generateContent(String prompt) {
        checkApiKey();
        geminiUsageService.checkDailyLimit();
        log.info("Sending prompt to Gemini ({}) for content generation", generationModel);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", maxOutputTokensGeneration
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", generationModel, apiKey);
            Map<?, ?> response = executePostWithRetry(url, requestBody);

            if (response != null && response.containsKey("candidates")) {
                recordUsageHelper("EXPLANATION", response);
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
            throw new RuntimeException("Empty or invalid response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to call Gemini API for generation: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini generation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateJsonContent(String prompt) {
        return generateJsonContent(prompt, 0.7);
    }

    @Override
    public String generateJsonContent(String prompt, double temperature) {
        checkApiKey();
        geminiUsageService.checkDailyLimit();
        log.info("Sending prompt to Gemini ({}) for JSON content generation with temperature {}. maxOutputTokens={}", 
            generationModel, temperature, maxOutputTokensGeneration);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", temperature,
                "maxOutputTokens", maxOutputTokensGeneration,
                "responseMimeType", "application/json"
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", generationModel, apiKey);
            Map<?, ?> response = executePostWithRetry(url, requestBody);

            if (response != null && response.containsKey("candidates")) {
                recordUsageHelper("GENERATION", response);
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    log.info("Gemini Candidate metadata: finishReason={}, safetyRatings={}", 
                        candidate.get("finishReason"), candidate.get("safetyRatings"));
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        String rawResponse = (String) part.get("text");
                        log.info("=== GEMINI JSON GENERATION RAW RESPONSE START ===");
                        if (rawResponse != null) {
                            log.info("Raw JSON response length: {}", rawResponse.length());
                            log.info("Raw JSON snippet: {}", rawResponse.substring(0, Math.min(rawResponse.length(), 2000)));
                            log.info("=== GEMINI JSON GENERATION RAW RESPONSE END ===");
                        } else {
                            log.info("rawResponse is null");
                        }
                        return rawResponse;
                    }
                }
            }
            throw new RuntimeException("Empty or invalid response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to call Gemini API for JSON generation: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini JSON generation failed: " + e.getMessage());
        }
    }

    @Override
    public String generateMockQuestions(String topic, String contextSection, String subjectWeightagesList, int totalCount) {
        checkApiKey();
        geminiUsageService.checkDailyLimit();
        log.info("Generating mock questions for topic: {} totalCount: {}", topic, totalCount);

        String prompt = String.format("""
            You are an expert GATE CSE exam paper generator.

            REFERENCE QUESTIONS (real GATE past questions for style/difficulty reference):
            %s

            GENERATION TASK:
            Generate exactly %d GATE-style questions distributed as follows:
            %s

            STRICT OUTPUT FORMAT — return ONLY a valid JSON array, no markdown,
            no explanation, no preamble. Each element:
            {
              "sequenceNo": <number>,
              "section": "GA" for General Aptitude questions, "CS" for all others,
              "type": "MCQ" or "MSQ" or "NAT",
              "questionText": "<question text>",
              "options": [
                {"label": "A", "text": "<text>"},
                {"label": "B", "text": "<text>"},
                {"label": "C", "text": "<text>"}
              ],
              "correctOptions": ["A"],
              "correctNatValue": null,
              "natTolerance": null,
              "marks": 1 or 2,
              "negativeMarks": 0.33 or 0.67 or 0,
              "explanation": "<2-3 sentence explanation>",
              "hasImage": false
            }
            For NAT: set options to [], correctOptions to [],
                     set correctNatValue to the answer number.
            For MSQ: set correctOptions to e.g. ["A", "C"].
            GA questions: sequenceNo 1-10, section "GA", marks 1 or 2.
            CS questions: sequenceNo 11-65 (or 11-55), section "CS".
            Match GATE exam difficulty and notation style exactly.
            """, contextSection, totalCount, subjectWeightagesList);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 8192,
                "responseMimeType", "application/json"
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", generationModel, apiKey);
            Map<?, ?> response = executePostWithRetry(url, requestBody);

            if (response != null && response.containsKey("candidates")) {
                recordUsageHelper("GENERATION", response);
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> parts = (List<?>) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
            throw new RuntimeException("Empty or invalid response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to call Gemini API for mock test generation: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini mock test generation failed: " + e.getMessage());
        }
    }

    @Override
    public String callGeminiWithImage(String prompt, byte[] imageBytes, String mimeType) {
        checkApiKey();
        geminiUsageService.checkDailyLimit();
        log.info("Sending prompt and image to Gemini ({}) for multimodal content generation", generationModel);

        List<Map<String, Object>> parts = new ArrayList<>();

        // Text part first
        parts.add(Map.of("text", prompt));

        // Image part (inline base64)
        if (imageBytes != null) {
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            parts.add(Map.of(
                "inlineData", Map.of(
                    "mimeType", mimeType, // "image/png" or "image/jpeg"
                    "data", base64Data
                )
            ));
            parts.add(Map.of("text",
                "The image above is directly relevant to this question. " +
                "Generate questions that explicitly reference elements visible in this diagram."
            ));
        }

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", parts)),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", maxOutputTokensGeneration
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", generationModel, apiKey);
            Map<?, ?> response = executePostWithRetry(url, requestBody);

            if (response != null && response.containsKey("candidates")) {
                recordUsageHelper("GENERATION", response);
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                    List<?> partsResponse = (List<?>) content.get("parts");
                    if (!partsResponse.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) partsResponse.get(0);
                        return (String) part.get("text");
                    }
                }
            }
            throw new RuntimeException("Empty or invalid response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to call Gemini API with image: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini multimodal generation failed: " + e.getMessage());
        }
    }

    private void recordUsageHelper(String callType, Map<?, ?> response) {
        try {
            if (response != null && response.containsKey("usageMetadata")) {
                Map<?, ?> usage = (Map<?, ?>) response.get("usageMetadata");
                int promptTokens = usage.containsKey("promptTokenCount") ? ((Number) usage.get("promptTokenCount")).intValue() : 0;
                int candidateTokens = usage.containsKey("candidatesTokenCount") ? ((Number) usage.get("candidatesTokenCount")).intValue() : 0;
                geminiUsageService.recordUsage(callType, promptTokens, candidateTokens);
            }
        } catch (Exception e) {
            log.warn("Failed to extract or record Gemini token usage: {}", e.getMessage());
        }
    }

    private Map<?, ?> executePostWithRetry(String url, Map<String, Object> requestBody) {
        int maxAttempts = 5;
        long backoffMs = 1500; // start with 1.5 seconds

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<?, ?> response = restClient.post()
                        .uri(url)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);
                if (response != null) {
                    return response;
                }
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API rate limit (429) hit on attempt {}/{}. Backing off for {} ms...", 
                    attempt, maxAttempts, backoffMs);
                if (attempt == maxAttempts) throw e;
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                log.warn("Gemini API returned status {} on attempt {}/{}. Backing off for {} ms...", 
                    e.getStatusCode(), attempt, maxAttempts, backoffMs);
                if (attempt == maxAttempts) throw e;
            } catch (Exception e) {
                log.warn("Gemini API call failed with exception: {} on attempt {}/{}. Backing off for {} ms...", 
                    e.getMessage(), attempt, maxAttempts, backoffMs);
                if (attempt == maxAttempts) throw new RuntimeException(e);
            }

            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("API retry interrupted", ie);
            }
            backoffMs *= 2; // exponential backoff
        }
        throw new RuntimeException("Failed to get response from Gemini API after 5 attempts");
    }
}
