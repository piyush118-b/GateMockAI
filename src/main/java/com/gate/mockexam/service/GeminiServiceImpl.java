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

    public GeminiServiceImpl(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model.transcription:gemini-2.5-flash}") String transcriptionModel,
            @Value("${gemini.model.generation:gemini-2.5-flash}") String generationModel,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey.trim();
        this.transcriptionModel = transcriptionModel;
        this.generationModel = generationModel;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public String transcribePdfToQuestions(byte[] pdfBytes, String manualAnswerKey) {
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
                "temperature", 0.0,
                "maxOutputTokens", 8192,
                "responseMimeType", "application/json"
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", transcriptionModel, apiKey);
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
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
            log.error("Failed to call Gemini API for transcription: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini transcription failed: " + e.getMessage());
        }
    }

    @Override
    public String generateContent(String prompt) {
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
                "maxOutputTokens", 8192
            )
        );

        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", generationModel, apiKey);
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
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
        log.info("Sending prompt to Gemini ({}) for JSON content generation", generationModel);

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
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
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
            log.error("Failed to call Gemini API for JSON generation: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini JSON generation failed: " + e.getMessage());
        }
    }
}
