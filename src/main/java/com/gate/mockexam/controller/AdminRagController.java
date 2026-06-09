package com.gate.mockexam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.service.DocumentParserService;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.RagIngestionService;
import com.gate.mockexam.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rag")
@Slf4j
public class AdminRagController {

    private final RagIngestionService ragIngestionService;
    private final DocumentParserService documentParserService;
    private final MockTestGenerationService mockTestGenerationService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;
    private final ExecutorService chunkExecutor;
    private final com.gate.mockexam.service.GeminiUsageService geminiUsageService;

    public AdminRagController(
            RagIngestionService ragIngestionService,
            DocumentParserService documentParserService,
            MockTestGenerationService mockTestGenerationService,
            GeminiService geminiService,
            ObjectMapper objectMapper,
            QuestionRepository questionRepository,
            @Qualifier("ollamaChunkExecutor") ExecutorService chunkExecutor,
            com.gate.mockexam.service.GeminiUsageService geminiUsageService) {
        this.ragIngestionService = ragIngestionService;
        this.documentParserService = documentParserService;
        this.mockTestGenerationService = mockTestGenerationService;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
        this.questionRepository = questionRepository;
        this.chunkExecutor = chunkExecutor;
        this.geminiUsageService = geminiUsageService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/rag/upload
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses PDF/TXT, extracts & aligns questions with answer key using Gemini AI.
     * Gemini 2.5 Flash native multimodality permits sending the entire PDF in one call.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handlePdfUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "answerKeyFile", required = false) MultipartFile answerKeyFile,
            @RequestParam("subject") String subject,
            @RequestParam("topic") String topic,
            @RequestParam(value = "answerKeyText", defaultValue = "") String answerKeyText) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a GATE Question Paper PDF to upload."));
        }

        try {
            log.info("Processing uploaded Question Paper file: {}", file.getOriginalFilename());

            // ── Extract Answer Key ───────────────────────────────────────────
            String extractedAnswerKeyText = "";
            if (answerKeyFile != null && !answerKeyFile.isEmpty()) {
                log.info("Processing uploaded Answer Key file: {}", answerKeyFile.getOriginalFilename());
                if (Objects.requireNonNull(answerKeyFile.getOriginalFilename()).endsWith(".pdf")) {
                    extractedAnswerKeyText = documentParserService.parsePdf(answerKeyFile.getBytes());
                } else {
                    extractedAnswerKeyText = documentParserService.parseTxt(answerKeyFile.getBytes());
                }
            }

            Map<String, DocumentParserService.AnswerKeyEntry> answerKeyMap = new HashMap<>();
            String manualAnswerKey = "";
            if (!extractedAnswerKeyText.isEmpty()) {
                manualAnswerKey += extractedAnswerKeyText + "\n";
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(extractedAnswerKeyText));
            }
            if (answerKeyText != null && !answerKeyText.isBlank()) {
                manualAnswerKey += answerKeyText;
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(answerKeyText));
            }
            log.info("Total parsed answer key entries: {}", answerKeyMap.size());

            // ── Call Gemini Service directly with the entire PDF ───────────────────
            log.info("Calling Gemini for full paper transcription...");
            String rawJson = geminiService.transcribePdfToQuestions(file.getBytes(), manualAnswerKey.trim());
            
            // Log for debugging
            log.info("Gemini response length: {} chars", rawJson != null ? rawJson.length() : 0);
            log.info("Response tail: '{}'",
              rawJson != null && rawJson.length() > 100
              ? rawJson.substring(rawJson.length() - 100) : rawJson);

            // Clean and extract
            String jsonArray = extractJsonArray(rawJson);

            // Parse with fallback
            List<AiGeneratedQuestionDto> uniqueQuestions;
            try {
                uniqueQuestions = objectMapper.readValue(jsonArray,
                  new com.fasterxml.jackson.core.type.TypeReference<List<AiGeneratedQuestionDto>>() {});
                log.info("Successfully parsed {} questions from Gemini response", uniqueQuestions.size());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("JSON parse failed after cleaning. Cleaned JSON start: {}",
                  jsonArray.substring(0, Math.min(500, jsonArray.length())));
                log.error("Jackson error: {}", e.getMessage());
                throw new org.springframework.web.server.ResponseStatusException(
                  org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                  "Gemini response could not be parsed. Raw response was " +
                  (rawJson != null ? rawJson.length() : 0) +
                  " chars. Jackson error: " + e.getOriginalMessage() +
                  ". Try uploading again — responses vary slightly each call."
                );
            }

            if (uniqueQuestions == null || uniqueQuestions.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of("error",
                        "No questions could be successfully parsed from the document segment by Gemini."));
            }

            // ── Align, Deduplicate & Bind Answer Keys ─────────────────────────
            for (AiGeneratedQuestionDto q : uniqueQuestions) {
                String sec = q.getSection() != null ? q.getSection().toUpperCase().trim() : "CS";
                int qNo = q.getSequenceNo();
                String key = sec + "_" + qNo;

                q.setExplanation("Official Answer derived from key.");
                q.setSubject(subject);
                q.setTopic(topic);

                DocumentParserService.AnswerKeyEntry entry = answerKeyMap.get(key);
                if (entry != null) {
                    q.setType(entry.getType());
                    q.setMarks(entry.getMarks());

                    if ("MCQ".equals(entry.getType()) || "MSQ".equals(entry.getType())) {
                        q.setNegativeMarks("MCQ".equals(entry.getType()) ? (entry.getMarks() == 1.0 ? 0.33 : 0.67) : 0.0);
                        Set<String> correctLabels = new HashSet<>();
                        if (entry.getCorrectKey() != null) {
                            Matcher m = Pattern.compile("[A-D]").matcher(entry.getCorrectKey().toUpperCase());
                            while (m.find()) correctLabels.add(m.group());
                        }
                        if (q.getOptions() == null) q.setOptions(new ArrayList<>());
                        for (AiGeneratedOptionDto opt : q.getOptions()) {
                            if (opt.getLabel() != null) {
                                opt.setCorrect(correctLabels.contains(opt.getLabel().toUpperCase()));
                            }
                        }
                    } else if ("NAT".equals(entry.getType())) {
                        q.setNegativeMarks(0.0);
                        q.setOptions(new ArrayList<>());
                        List<Double> nums = new ArrayList<>();
                        if (entry.getCorrectKey() != null) {
                            Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(entry.getCorrectKey());
                            while (m.find()) {
                                try { nums.add(Double.parseDouble(m.group())); } catch (NumberFormatException ignored) {}
                            }
                        }
                        if (nums.size() >= 2) {
                            double low = nums.get(0), high = nums.get(1);
                            q.setCorrectNatValue(Math.round(((low + high) / 2.0) * 10000.0) / 10000.0);
                            q.setNatTolerance(Math.round((Math.abs(high - low) / 2.0) * 10000.0) / 10000.0);
                        } else if (nums.size() == 1) {
                            q.setCorrectNatValue(nums.get(0));
                            q.setNatTolerance(0.0);
                        } else {
                            q.setCorrectNatValue(null);
                            q.setNatTolerance(0.0);
                        }
                    }
                } else {
                    if ("MCQ".equals(q.getType())) {
                        q.setNegativeMarks(q.getMarks() == 1.0 ? 0.33 : 0.67);
                    } else {
                        q.setNegativeMarks(0.0);
                    }
                }
            }

            // ── Sort GA first, then CS; rewrite global sequenceNos ───────────
            uniqueQuestions.sort((q1, q2) -> {
                String sec1 = q1.getSection() != null ? q1.getSection().toUpperCase().trim() : "CS";
                String sec2 = q2.getSection() != null ? q2.getSection().toUpperCase().trim() : "CS";
                if (!sec1.equals(sec2)) return sec1.equals("GA") ? -1 : 1;
                return Integer.compare(q1.getSequenceNo(), q2.getSequenceNo());
            });
            for (int i = 0; i < uniqueQuestions.size(); i++) {
                uniqueQuestions.get(i).setSequenceNo(i + 1);
            }

            Map<String, Object> responseMap = new LinkedHashMap<>();
            responseMap.put("title", "Official GATE Past Paper: " + topic + " (" + subject + ")");
            responseMap.put("subject", subject);
            responseMap.put("topic", topic);
            responseMap.put("durationMinutes", 180);
            responseMap.put("questions", uniqueQuestions);
            responseMap.put("totalExtracted", uniqueQuestions.size());
            responseMap.put("warningMessage", null);

            try {
                com.gate.mockexam.entity.GeminiTokenUsage lastUsage = geminiUsageService.getLastUsageRecord();
                if (lastUsage != null) {
                    double estCost = (lastUsage.getInputTokens() * 0.0000003) + (lastUsage.getOutputTokens() * 0.0000025);
                    responseMap.put("tokenUsage", Map.of(
                        "inputTokens", lastUsage.getInputTokens(),
                        "outputTokens", lastUsage.getOutputTokens(),
                        "totalTokens", lastUsage.getTotalTokens(),
                        "estimatedCostUsd", estCost
                    ));
                }
            } catch (Exception ex) {
                log.warn("Could not append token usage to RAG upload response: {}", ex.getMessage());
            }

            return ResponseEntity.ok(responseMap);

        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse and align past paper: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to parse document: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/rag/confirm
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Accepts the aligned draft JSON, persists to DB, runs async explanation generation
     * (Gemini pass), then embeds questions (with explanations) to PGVector.
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmIngestion(@RequestBody AiGeneratedTestDto draft) {
        if (draft == null || draft.getQuestions() == null || draft.getQuestions().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Draft is empty or missing questions."));
        }

        try {
            // Step 1: Persist questions relationally
            MockTest test = mockTestGenerationService.persistTest(draft);

            // Step 2: Async explanation generation pass
            List<Question> savedQuestions = questionRepository.findByTestIdOrderBySequenceNoAsc(test.getId());
            Map<Integer, Question> seqToQuestion = new HashMap<>();
            for (Question q : savedQuestions) {
                seqToQuestion.put(q.getSequenceNo(), q);
            }

            List<CompletableFuture<Void>> explFutures = new ArrayList<>();
            for (AiGeneratedQuestionDto qDto : draft.getQuestions()) {
                final AiGeneratedQuestionDto finalQDto = qDto;
                CompletableFuture<Void> explFuture = CompletableFuture.runAsync(() -> {
                    Question savedQ = seqToQuestion.get(finalQDto.getSequenceNo());
                    if (savedQ == null) return;
                    String explanation = generateExplanation(finalQDto);
                    if (explanation != null) {
                        savedQ.setExplanation(explanation);
                        // Also update the DTO so the vector store Document picks it up
                        finalQDto.setExplanation(explanation);
                        try {
                            questionRepository.save(savedQ);
                        } catch (Exception e) {
                            log.warn("Failed to save explanation for question {}: {}", savedQ.getId(), e.getMessage());
                        }
                    }
                }, chunkExecutor);
                explFutures.add(explFuture);
            }

            // Wait for all explanation futures (max 10 min total — each already has model-level timeout)
            CompletableFuture.allOf(explFutures.toArray(new CompletableFuture[0]))
                    .get(600, TimeUnit.SECONDS);

            log.info("Explanation generation pass complete for {} questions", draft.getQuestions().size());

            // Step 3: Embed in PGVector — Document content now includes explanation
            List<Document> documents = new ArrayList<>();
            for (AiGeneratedQuestionDto q : draft.getQuestions()) {
                String dynamicSubject = q.getSubject() != null && !q.getSubject().trim().isEmpty()
                        ? q.getSubject() : draft.getSubject();
                String dynamicTopic = q.getTopic() != null && !q.getTopic().trim().isEmpty()
                        ? q.getTopic() : draft.getTopic();

                String content = String.format(
                        "Subject: %s | Topic: %s | Type: %s\nQuestion: %s\nExplanation: %s",
                        dynamicSubject, dynamicTopic, q.getType(),
                        q.getQuestionText(),
                        q.getExplanation() != null ? q.getExplanation() : ""
                );

                Map<String, Object> metadata = Map.of(
                        "id", "parsed_" + q.getSequenceNo() + "_" + System.currentTimeMillis(),
                        "topic", dynamicTopic,
                        "subject", dynamicSubject,
                        "type", q.getType()
                );
                documents.add(new Document(content, metadata));
            }
            ragIngestionService.ingestDocumentChunks(documents);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Successfully committed " + draft.getQuestions().size()
                            + " questions (with AI explanations) and populated PGVector embeddings!",
                    "testId", test.getId().toString()
            ));

        } catch (TimeoutException te) {
            log.warn("Explanation generation timed out — proceeding without some explanations: {}", te.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Explanation generation timed out: " + te.getMessage()));
        } catch (Exception e) {
            log.error("Failed to commit aligned paper to PGVector: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to ingest: " + e.getMessage()));
        }
    }

    @GetMapping("/vector-count")
    public ResponseEntity<Map<String, Object>> getVectorCount() {
        return ResponseEntity.ok(Map.of("count", ragIngestionService.getVectorCount()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/rag/test
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/test")
    public List<Map<String, Object>> testSimilarityQuery(
            @RequestParam("query") String query,
            @RequestParam("topK") int topK) {
        log.info("Testing similarity query: '{}' topK: {}", query, topK);
        List<Document> matches = ragIngestionService.retrieveSimilarQuestions(query, topK);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Document doc : matches) {
            results.add(Map.of("content", doc.getText(), "metadata", doc.getMetadata()));
        }
        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates an explanation for a single question via Gemini.
     */
    private String generateExplanation(AiGeneratedQuestionDto q) {
        try {
            // Build correct answer description
            String correctAnswer;
            if ("NAT".equals(q.getType())) {
                correctAnswer = q.getCorrectNatValue() != null
                        ? String.valueOf(q.getCorrectNatValue()) : "N/A";
            } else {
                correctAnswer = q.getOptions() == null ? "N/A" :
                        q.getOptions().stream()
                                .filter(AiGeneratedOptionDto::isCorrect)
                                .map(o -> o.getLabel() + ": " + o.getText())
                                .collect(Collectors.joining("; "));
            }

            String prompt = String.format("""
                You are a GATE CS expert. Explain why the correct answer to this question is correct, \
                in 3-5 concise sentences. Be precise and technical.
                Question: %s
                Correct answer: %s
                """, q.getQuestionText(), correctAnswer);

            String response = geminiService.generateContent(prompt);
            return response != null ? response.trim() : null;
        } catch (Exception e) {
            log.warn("Explanation generation failed for question seq={}: {}", q.getSequenceNo(), e.getMessage());
            return null;
        }
    }

    private String extractJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            log.error("Gemini returned null or empty response");
            return "[]";
        }
        String s = raw.strip();

        // Strip markdown code fences (Gemini does this often)
        if (s.startsWith("```")) {
            s = s.replaceAll("(?s)^```[a-zA-Z]*\\s*", "");
            s = s.replaceAll("(?s)\\s*```$", "");
            s = s.strip();
            log.info("Stripped markdown fences from Gemini response");
        }

        // Strip any preamble text before the JSON array
        // (Gemini sometimes says "Here is the JSON:" before the array)
        int arrayStart = s.indexOf('[');
        if (arrayStart < 0) {
            log.error("No JSON array found in Gemini response. Raw: {}", s.substring(0, Math.min(200, s.length())));
            return "[]";
        }
        if (arrayStart > 0) {
            log.warn("Trimmed {} chars of preamble before JSON array", arrayStart);
            s = s.substring(arrayStart);
        }

        // Find matching closing bracket
        int arrayEnd = s.lastIndexOf(']');
        if (arrayEnd < 0) {
            log.warn("JSON array not closed — truncation detected. Attempting repair.");
            // Find last complete object
            int lastBrace = s.lastIndexOf('}');
            if (lastBrace > 0) {
                s = s.substring(0, lastBrace + 1) + "]";
                log.warn("Repaired: closed array after last complete object at pos {}", lastBrace);
            } else {
                log.error("Cannot repair truncated JSON — no complete object found");
                return "[]";
            }
        } else {
            // Trim anything after the closing bracket
            s = s.substring(0, arrayEnd + 1);
        }

        return escapeJsonBackslashes(s);
    }

    private String escapeJsonBackslashes(String json) {
        if (json == null) return null;
        StringBuilder sb = new StringBuilder();
        int len = json.length();
        for (int i = 0; i < len; i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                if (i + 1 < len) {
                    char next = json.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == '/' || 
                        next == 'b' || next == 'f' || next == 'n' || 
                        next == 'r' || next == 't') {
                        sb.append(c);
                        sb.append(next);
                        i++;
                    } else if (next == 'u' && i + 5 < len && isHex(json.substring(i + 2, i + 6))) {
                        sb.append(c);
                        sb.append(json.substring(i + 1, i + 6));
                        i += 5;
                    } else {
                        sb.append("\\\\");
                    }
                } else {
                    sb.append("\\\\");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}

