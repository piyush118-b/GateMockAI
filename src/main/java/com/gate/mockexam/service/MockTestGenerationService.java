package com.gate.mockexam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.entity.Option;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.MockTestRepository;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gate.mockexam.service.GeminiService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MockTestGenerationService {

    private final GeminiService geminiService;
    private final RagIngestionService ragIngestionService;
    private final ObjectMapper objectMapper;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final GeminiUsageService geminiUsageService;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsService analyticsService;
    private final com.gate.mockexam.repository.UserRepository userRepository;

    private static final java.util.Map<String, Integer> GATE_QUESTION_DISTRIBUTION = new java.util.LinkedHashMap<>();
    static {
        GATE_QUESTION_DISTRIBUTION.put("Data Structures & Algorithms", 12);
        GATE_QUESTION_DISTRIBUTION.put("Operating Systems", 8);
        GATE_QUESTION_DISTRIBUTION.put("Database Management Systems", 8);
        GATE_QUESTION_DISTRIBUTION.put("Computer Networks", 8);
        GATE_QUESTION_DISTRIBUTION.put("Theory of Computation", 7);
        GATE_QUESTION_DISTRIBUTION.put("Computer Organisation & Architecture", 7);
        GATE_QUESTION_DISTRIBUTION.put("Engineering Mathematics", 10);
        GATE_QUESTION_DISTRIBUTION.put("General Aptitude", 10);
    }

    private final java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(8);

    @Value("classpath:prompts/generate_mock_test.st")
    private Resource promptTemplate;

    @Value("classpath:prompts/generate_full_gate_segment.st")
    private Resource segmentPromptTemplate;

    @jakarta.annotation.PostConstruct
    public void init() {
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
    }



    public MockTest generateAndSaveTest(MockTestGenerationRequestDto request) {
        // Enforce limits to control Gemini API costs
        int totalRequested = request.getMcqCount() + request.getMsqCount() + request.getNatCount();
        if (totalRequested > 15) {
            throw new IllegalArgumentException("Cannot generate more than 15 total questions per test to control API costs.");
        }

        java.time.LocalDateTime oneHourAgo = java.time.LocalDateTime.now().minusHours(1);
        long recentGenerations = mockTestRepository.countByCreatedAtAfter(oneHourAgo);
        if (recentGenerations >= 5) {
            throw new IllegalStateException("AI Generation limit exceeded (Max 5 tests per hour to control API costs). Please try again later.");
        }

        // Step 1: RAG retrieval — get top 5 similar past questions as context
        List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(
            request.getTopic() + " " + request.getSubject(), 5
        );

        String contextQuestions = IntStream.range(0, contextDocs.size())
            .mapToObj(i -> (i + 1) + ". " + contextDocs.get(i).getText())
            .collect(Collectors.joining("\n\n"));

        // Step 2: Build and send prompt to Gemini with manual template placeholder replacements
        String renderedPrompt = loadTemplate()
            .replace("{contextCount}", String.valueOf(contextDocs.size()))
            .replace("{contextQuestions}", contextQuestions)
            .replace("{topic}", request.getTopic())
            .replace("{subject}", request.getSubject())
            .replace("{mcqCount}", String.valueOf(request.getMcqCount()))
            .replace("{msqCount}", String.valueOf(request.getMsqCount()))
            .replace("{natCount}", String.valueOf(request.getNatCount()));

        String rawJson = geminiService.generateJsonContent(renderedPrompt);

        log.debug("Raw Gemini response: {}", rawJson);

        // Step 3: Parse JSON → DTOs
        AiGeneratedTestDto dto = parseResponse(rawJson);

        // Step 4: Map DTOs → JPA entities and persist
        return persistTest(dto);
    }

    private AiGeneratedTestDto parseResponse(String rawJson) {
        // Strip potential markdown fences if Gemini adds them despite instructions
        String cleaned = rawJson.trim()
            .replaceAll("^```json\\s*", "")
            .replaceAll("^```\\s*", "")
            .replaceAll("```$", "")
            .trim();
        try {
            return objectMapper.readValue(cleaned, AiGeneratedTestDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini JSON response: {}", cleaned, e);
            throw new RuntimeException("AI returned invalid JSON. Please retry.", e);
        }
    }

    @Transactional
    public MockTest persistTest(AiGeneratedTestDto dto) {
        MockTest test = MockTest.builder()
            .title(dto.getTitle())
            .topic(dto.getTopic())
            .subject(dto.getSubject())
            .durationMinutes(dto.getDurationMinutes())
            .isPublished(false)
            .build();

        // Compute total marks
        BigDecimal totalMarks = dto.getQuestions().stream()
            .map(q -> BigDecimal.valueOf(q.getMarks()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        test.setTotalMarks(totalMarks);

        test = mockTestRepository.save(test);

        for (AiGeneratedQuestionDto qDto : dto.getQuestions()) {
            Question question = Question.builder()
                .test(test)
                .questionText(qDto.getQuestionText())
                .type(QuestionType.valueOf(qDto.getType()))
                .marks(BigDecimal.valueOf(qDto.getMarks()))
                .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                .correctNatValue(qDto.getCorrectNatValue())
                .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                .sequenceNo(qDto.getSequenceNo())
                .explanation(qDto.getExplanation())
                .build();

            question = questionRepository.save(question);

            if (qDto.getOptions() != null) {
                for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                    Option opt = Option.builder()
                        .question(question)
                        .optionLabel(oDto.getLabel().charAt(0))
                        .optionText(oDto.getText())
                        .isCorrect(oDto.isCorrect())
                        .build();
                    opt = optionRepository.save(opt);
                    question.getOptions().add(opt);
                }
            }
            test.getQuestions().add(question);
        }

        return test;
    }

    private java.util.UUID getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                String email = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
                com.gate.mockexam.entity.User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve current user ID: {}", e.getMessage());
        }
        return null;
    }

    private boolean isNearDuplicate(String questionBody, java.util.UUID userId) {
        if (userId == null) return false;
        try {
            float[] embedding = embeddingModel.embed(questionBody);
            String embStr = java.util.Arrays.toString(embedding).replace(" ", "");

            String sql = """
                SELECT COUNT(*) FROM questions q
                JOIN attempt_answers aa ON aa.question_id = q.id
                JOIN attempts a ON aa.attempt_id = a.id
                WHERE a.user_id = ?
                AND 1 - (q.embedding <=> ?::vector) > 0.92
                """;

            Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, embStr);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Deduplication check failed: {}", e.getMessage());
            return false;
        }
    }

    private List<AiGeneratedQuestionDto> generateSubjectQuestions(String subject, int target, java.util.function.Consumer<String> progressCallback) {
        List<AiGeneratedQuestionDto> collected = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 4;
        java.util.UUID userId = getCurrentUserId();

        while (collected.size() < target && attempts < maxAttempts) {
            int remaining = target - collected.size();
            int batchTarget = Math.min(remaining, 15);

            List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(subject, 8);
            List<String> ragContext = contextDocs.stream().map(Document::getText).collect(Collectors.toList());

            List<String> existingBodies = collected.stream()
                .map(AiGeneratedQuestionDto::getQuestionText)
                .collect(Collectors.toList());

            List<AiGeneratedQuestionDto> batch = callGeminiForBatch(subject, batchTarget, ragContext, existingBodies);
            
            // Apply deduplication (Change 7)
            for (AiGeneratedQuestionDto q : batch) {
                if (!isNearDuplicate(q.getQuestionText(), userId)) {
                    collected.add(q);
                } else {
                    log.info("Deduplication triggered: Skipped duplicate question for subject={}", subject);
                }
            }
            
            attempts++;

            emitProgress(progressCallback, subject, collected.size(), target, attempts);

            if (batch.isEmpty()) break;
        }

        return collected;
    }

    private void emitProgress(java.util.function.Consumer<String> progressCallback, String subject, int collectedSize, int target, int attempts) {
        String message = String.format("[Gemini] Generating %s: collected %d/%d questions (Attempt %d)", subject, collectedSize, target, attempts);
        progressCallback.accept(String.format("{\"step\": 2, \"message\": \"[System] %s\", \"percent\": %d}", message, Math.min(95, 10 + (attempts * 15))));
    }

    private String sanitizeGeminiJson(String raw) {
        if (raw == null || raw.isBlank()) throw new RuntimeException("Empty response");
        raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "");
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return raw.substring(start, end + 1).trim();
        }
        throw new RuntimeException("No JSON array found in Gemini response. Raw: " + raw.substring(0, Math.min(200, raw.length())));
    }

    private List<AiGeneratedQuestionDto> callGeminiForBatch(String subject, int batchTarget, List<String> ragContext, List<String> existingBodies) {
        String contextQuestions = IntStream.range(0, ragContext.size())
            .mapToObj(i -> (i + 1) + ". " + ragContext.get(i))
            .collect(Collectors.joining("\n\n"));

        String existingBodiesStr = existingBodies.isEmpty() ? "None" : String.join("\n", existingBodies);
        if (existingBodiesStr.length() > 2000) {
            existingBodiesStr = existingBodiesStr.substring(0, 2000) + "... (truncated)";
        }

        int mcq = Math.max(1, (int) Math.round(batchTarget * 0.6));
        int msq = Math.max(0, (int) Math.round(batchTarget * 0.2));
        int nat = batchTarget - mcq - msq;

        // Change 6: Adaptive difficulty
        java.util.UUID userId = getCurrentUserId();
        String difficultyHint = "moderate difficulty, mixing recall and application";
        int twoMarkRatio = 45;
        if (userId != null) {
            try {
                java.util.Map<String, Double> accuracy = analyticsService.getSubjectAccuracy(userId);
                double acc = accuracy.getOrDefault(subject, 0.5);
                if (acc >= 0.75) {
                    difficultyHint = "challenging, with tricky edge cases and subtle conceptual distinctions";
                    twoMarkRatio = 65;
                } else if (acc <= 0.40) {
                    difficultyHint = "straightforward, testing core definitions and basic application";
                    twoMarkRatio = 30;
                }
            } catch (Exception e) {
                log.warn("Could not calculate subject accuracy: {}", e.getMessage());
            }
        }

        String prompt = String.format("""
            You are a senior computer science professor from an elite IIT / IISc setting, designing a segment of the official full-length 100-mark GATE CSE examination paper.
            
            Below are %d real past GATE questions matching the subject "%s" for your direct academic reference.
            Study their mathematical depth, logic, formatting style, and psychometric trap designs carefully.
            
            --- REFERENCE PAST QUESTIONS ---
            %s
            --- END REFERENCE ---
            
            Your task is to generate a BRAND NEW, completely unique, syllabus-aligned set of questions for:
            - Subject: %s
            - MCQ Count to Generate: %d
            - MSQ Count to Generate: %d
            - NAT Count to Generate: %d
            
            --- STRICT SCIENTIFIC REQUIREMENTS ---
            1. Zero Hallucination or Plagiarism: Every question must be fully conceptual and original. Never copy reference questions.
            2. GATE Rigor: Match the high-level cognitive levels of Bloom's Taxonomy (Analyze, Apply). Incorporate realistic hidden traps or multi-step logic.
            3. No Placeholders: Write full, genuine mathematical equations, code structures, and descriptive technical statements.
            4. Marking Schema:
               - MCQ: 4 options (A, B, C, D), exactly one correct. Set 'marks' to 1 or 2. If 1 mark, set 'negativeMarks' to 0.33. If 2 marks, set 'negativeMarks' to 0.67.
               - MSQ: 4 options (A, B, C, D), 2 or more correct. Set 'marks' to 2, and 'negativeMarks' to 0.
               - NAT: Free numeric entry. Set 'marks' to 1 or 2. Set 'negativeMarks' to 0. Must specify a valid 'correctNatValue' as a single numeric float or integer. Set 'natTolerance' (e.g. 0.01 or 0).
            5. Concise Explanations: Every single question must have a concise, 2-3 sentence conceptual explanation in the 'explanation' field.
            
            Return ONLY a JSON array matching the schema (no markdown, no preamble). Schema:
            [
              {
                "sequenceNo": 1,
                "type": "MCQ",
                "subject": "%s",
                "topic": "...",
                "questionText": "...",
                "marks": 1,
                "negativeMarks": 0.33,
                "explanation": "...",
                "options": [
                  {"label": "A", "text": "...", "isCorrect": false},
                  {"label": "B", "text": "...", "isCorrect": true},
                  {"label": "C", "text": "...", "isCorrect": false},
                  {"label": "D", "text": "...", "isCorrect": false}
                ]
              }
            ]
            
            You MUST return a valid JSON array containing EXACTLY %d question objects.
            Difficulty level for this subject: %s
            Question marks distribution: %d%% should be 2-mark questions, rest 1-mark.
            Do NOT include any text, explanation, or markdown before or after the JSON array.
            Do NOT wrap the JSON in ```json fences.
            The array must start with [ and end with ].
            If generating %d distinct questions is difficult, create variations on related subtopics to reach the count.
            Minimum acceptable count: %d.
            Already generated question bodies (avoid duplicating these):
            %s
            """, 
            ragContext.size(), subject, contextQuestions, subject, mcq, msq, nat, 
            subject, batchTarget, difficultyHint, twoMarkRatio, batchTarget, Math.max(5, batchTarget - 2), existingBodiesStr);

        try {
            String rawJson = geminiService.generateJsonContent(prompt);
            String sanitized = sanitizeGeminiJson(rawJson);
            return objectMapper.readValue(sanitized, new com.fasterxml.jackson.core.type.TypeReference<List<AiGeneratedQuestionDto>>() {});
        } catch (Exception e) {
            log.warn("Gemini call failed for batch of subject {}, retrying once...", subject, e);
            try {
                String rawJson = geminiService.generateJsonContent(prompt, 0.3);
                String sanitized = sanitizeGeminiJson(rawJson);
                return objectMapper.readValue(sanitized, new com.fasterxml.jackson.core.type.TypeReference<List<AiGeneratedQuestionDto>>() {});
            } catch (Exception ex) {
                log.error("Gemini call failed twice for batch of subject {}", subject, ex);
                return Collections.emptyList();
            }
        }
    }

    @Transactional
    public MockTest generateFullGateCsePaper(java.util.function.Consumer<String> progressCallback) {
        log.info("Starting Full 65-Question GATE CSE AI Paper compilation in parallel...");
        
        MockTest test = MockTest.builder()
            .title("AI-Generated Full 100-Mark GATE CSE Exam")
            .topic("Full Syllabus")
            .subject("Computer Science")
            .branch("CSE")
            .yearLabel("2026")
            .durationMinutes(180)
            .isPublished(false)
            .build();
        test = mockTestRepository.save(test);

        progressCallback.accept("{\"step\": 1, \"message\": \"[System] Starting parallel subject RAG query and LLM generation...\", \"percent\": 10}");

        List<CompletableFuture<List<AiGeneratedQuestionDto>>> futures = GATE_QUESTION_DISTRIBUTION
            .entrySet().stream()
            .map(entry -> CompletableFuture.supplyAsync(() -> {
                try {
                    return generateSubjectQuestions(entry.getKey(), entry.getValue(), progressCallback);
                } catch (Exception e) {
                    log.error("Failed to generate subject: {}", entry.getKey(), e);
                    String warningMsg = String.format("[Warning] Failed to generate subject '%s': %s", entry.getKey(), e.getMessage());
                    progressCallback.accept(String.format("{\"step\": 2, \"message\": \"%s\", \"percent\": 20}", warningMsg));
                    return new ArrayList<AiGeneratedQuestionDto>();
                }
            }, executorService))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<List<AiGeneratedQuestionDto>> results = new ArrayList<>();
        int totalQuestionsGenerated = 0;
        for (CompletableFuture<List<AiGeneratedQuestionDto>> future : futures) {
            List<AiGeneratedQuestionDto> subjectQuestions = future.join();
            results.add(subjectQuestions);
            if (subjectQuestions != null) {
                totalQuestionsGenerated += subjectQuestions.size();
            }
        }

        if (totalQuestionsGenerated == 0) {
            log.error("AI Parallel Generation failed: All subjects failed to generate any questions.");
            throw new RuntimeException("AI Segment Compilation failed: All subjects failed to generate any questions.");
        }

        log.info("[Parallel] Committing compiled questions to database relationally...");
        BigDecimal aggregatedTotalMarks = BigDecimal.ZERO;
        int globalSeqNo = 1;
        
        for (int i = 0; i < results.size(); i++) {
            List<AiGeneratedQuestionDto> questionsList = results.get(i);
            if (questionsList != null) {
                for (AiGeneratedQuestionDto qDto : questionsList) {
                    Question question = Question.builder()
                        .test(test)
                        .questionText(qDto.getQuestionText())
                        .type(QuestionType.valueOf(qDto.getType()))
                        .marks(BigDecimal.valueOf(qDto.getMarks()))
                        .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                        .correctNatValue(qDto.getCorrectNatValue())
                        .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                        .sequenceNo(globalSeqNo++)
                        .explanation(qDto.getExplanation())
                        .build();

                    question = questionRepository.save(question);
                    aggregatedTotalMarks = aggregatedTotalMarks.add(question.getMarks());

                    // Change 7: Store embedding
                    try {
                        float[] emb = embeddingModel.embed(question.getQuestionText());
                        String embStr = java.util.Arrays.toString(emb).replace(" ", "");
                        jdbcTemplate.update("UPDATE questions SET embedding = ?::vector WHERE id = ?", embStr, question.getId());
                    } catch (Exception ex) {
                        log.warn("Could not save question embedding: {}", ex.getMessage());
                    }

                    if (qDto.getOptions() != null) {
                        for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                            Option opt = Option.builder()
                                .question(question)
                                .optionLabel(oDto.getLabel().charAt(0))
                                .optionText(oDto.getText())
                                .isCorrect(oDto.isCorrect())
                                .build();
                            opt = optionRepository.save(opt);
                            question.getOptions().add(opt);
                        }
                    }
                    test.getQuestions().add(question);
                }
            }
        }

        test.setTotalMarks(aggregatedTotalMarks);
        test = mockTestRepository.save(test);

        log.info("Full 65-Question GATE CSE AI Paper compilation complete! Total Questions: {}, Total Marks: {}", 
            test.getQuestions().size(), test.getTotalMarks());

        return test;
    }

    private String loadTemplate() {
        try {
            return new String(promptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load prompt template", e);
        }
    }

    private String loadSegmentTemplate() {
        try {
            return new String(segmentPromptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load segment prompt template", e);
        }
    }

    /**
     * Generates a full GATE paper based on admin-specified per-subject mark allocations.
     * subjectWeightages: Map<SubjectName, AllocatedMarks>
     */
    @Transactional
    public MockTest generateWeightedGatePaper(
            String branchCode,
            String yearLabel,
            java.util.Map<String, Integer> subjectWeightages,
            java.util.function.Consumer<String> progressCallback) {

        log.info("Starting Parallel Weighted GATE Paper compilation for branch={} year={}", branchCode, yearLabel);

        MockTest test = MockTest.builder()
            .title("GATE " + yearLabel + " " + branchCode + " — AI Generated Mock")
            .topic("Full Syllabus")
            .subject(branchCode)
            .branch(branchCode)
            .yearLabel(yearLabel)
            .durationMinutes(180)
            .isPublished(false)
            .build();
        test = mockTestRepository.save(test);

        class SubjectGenerationTask {
            String subjectName;
            int allocatedMarks;
            int oneMarkMcq;
            int twoMarkMcq;
            int nat;
            int startSeq;
            int stepNo;
            int percent;

            SubjectGenerationTask(String subjectName, int allocatedMarks, int oneMarkMcq, int twoMarkMcq, int nat, int startSeq, int stepNo, int percent) {
                this.subjectName = subjectName;
                this.allocatedMarks = allocatedMarks;
                this.oneMarkMcq = oneMarkMcq;
                this.twoMarkMcq = twoMarkMcq;
                this.nat = nat;
                this.startSeq = startSeq;
                this.stepNo = stepNo;
                this.percent = percent;
            }
        }

        List<SubjectGenerationTask> tasks = new ArrayList<>();
        int currentSeq = 1;
        int step = 0;
        int total = subjectWeightages.size();

        for (java.util.Map.Entry<String, Integer> entry : subjectWeightages.entrySet()) {
            String subjectName = entry.getKey();
            int allocatedMarks = entry.getValue();
            if (allocatedMarks <= 0) continue;

            step++;
            int percent = (step * 100) / total;

            int oneMarkMcq = Math.max(1, (int) Math.round(allocatedMarks * 0.6));
            int twoMarkMcq = Math.max(0, (int) Math.round(allocatedMarks * 0.3 / 2));
            int nat       = Math.max(0, allocatedMarks - oneMarkMcq - (twoMarkMcq * 2));
            int totalQuestions = oneMarkMcq + twoMarkMcq + nat;

            tasks.add(new SubjectGenerationTask(subjectName, allocatedMarks, oneMarkMcq, twoMarkMcq, nat, currentSeq, step, percent));
            currentSeq += totalQuestions;
        }

        // Notify initial RAG / compilation start
        progressCallback.accept("{\"step\": 1, \"message\": \"[System] Starting parallel subject RAG query and LLM generation...\", \"percent\": 10}");

        // Run RAG and Gemini generation in parallel
        List<CompletableFuture<List<AiGeneratedQuestionDto>>> futures = tasks.stream()
            .map(task -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[Parallel] Starting retrieval & generation for subject: {}", task.subjectName);
                    
                    // RAG context retrieval
                    List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(task.subjectName, 5);

                    // Run generation in batches of max 10
                    return generateSegmentInBatches(
                        task.subjectName, task.subjectName, task.subjectName + " (comprehensive GATE syllabus)",
                        task.startSeq, task.oneMarkMcq + task.twoMarkMcq,
                        0, task.nat,
                        contextDocs, progressCallback, task.stepNo, task.percent
                    );
                } catch (Exception e) {
                    log.error("Failed to generate subject: {}", task.subjectName, e);
                    String warningMsg = String.format("[Warning] Failed to generate subject '%s': %s", task.subjectName, e.getMessage());
                    progressCallback.accept(String.format("{\"step\": %d, \"message\": \"%s\", \"percent\": %d}", task.stepNo, warningMsg, task.percent));
                    return new ArrayList<AiGeneratedQuestionDto>();
                }
            }))
            .collect(Collectors.toList());

        // Wait for all subjects to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<List<AiGeneratedQuestionDto>> results = new ArrayList<>();
        int totalQuestionsGenerated = 0;
        for (CompletableFuture<List<AiGeneratedQuestionDto>> future : futures) {
            List<AiGeneratedQuestionDto> subjectQuestions = future.join();
            results.add(subjectQuestions);
            if (subjectQuestions != null) {
                totalQuestionsGenerated += subjectQuestions.size();
            }
        }

        if (totalQuestionsGenerated == 0) {
            log.error("AI Parallel Generation failed: All subjects failed to generate any questions.");
            throw new RuntimeException("AI Segment Compilation failed: All subjects failed to generate any questions.");
        }

        // Persist questions relationally on the main transactional thread
        log.info("[Parallel] Committing compiled questions to database relationally...");
        BigDecimal aggregatedTotalMarks = BigDecimal.ZERO;
        int globalSeqNo = 1;

        for (int i = 0; i < tasks.size(); i++) {
            List<AiGeneratedQuestionDto> questionsList = results.get(i);
            if (questionsList != null) {
                for (AiGeneratedQuestionDto qDto : questionsList) {
                    Question question = Question.builder()
                        .test(test)
                        .questionText(qDto.getQuestionText())
                        .type(QuestionType.valueOf(qDto.getType()))
                        .marks(BigDecimal.valueOf(qDto.getMarks()))
                        .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                        .correctNatValue(qDto.getCorrectNatValue())
                        .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                        .sequenceNo(globalSeqNo++) // Use sequentially incremented globalSeqNo to guarantee gapless sequence numbers
                        .explanation(qDto.getExplanation())
                        .build();

                    question = questionRepository.save(question);
                    aggregatedTotalMarks = aggregatedTotalMarks.add(question.getMarks());

                    if (qDto.getOptions() != null) {
                        for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                            Option opt = Option.builder()
                                .question(question)
                                .optionLabel(oDto.getLabel().charAt(0))
                                .optionText(oDto.getText())
                                .isCorrect(oDto.isCorrect())
                                .build();
                            opt = optionRepository.save(opt);
                            question.getOptions().add(opt);
                        }
                    }
                    test.getQuestions().add(question);
                }
            }
        }

        test.setTotalMarks(aggregatedTotalMarks);
        test = mockTestRepository.save(test);
        log.info("Weighted GATE Paper complete! Questions: {}, Marks: {}", test.getQuestions().size(), test.getTotalMarks());
        return test;
    }

    private List<AiGeneratedQuestionDto> generateSegmentInBatches(
            String segmentName,
            String subjects,
            String syllabusFocus,
            int startSeq,
            int reqMcq,
            int reqMsq,
            int reqNat,
            List<Document> contextDocs,
            java.util.function.Consumer<String> progressCallback,
            int stepNo,
            int percent) {
        
        List<AiGeneratedQuestionDto> allQuestions = new ArrayList<>();
        int currentSeq = startSeq;

        // Group into balanced chunks of max 10 questions (mixed types)
        class BatchRequest {
            int mcq;
            int msq;
            int nat;
            BatchRequest(int mcq, int msq, int nat) {
                this.mcq = mcq;
                this.msq = msq;
                this.nat = nat;
            }
        }
        List<BatchRequest> batches = new ArrayList<>();
        
        int maxBatchSize = 10;
        int remainingMcq = reqMcq;
        int remainingMsq = reqMsq;
        int remainingNat = reqNat;

        while (remainingMcq > 0 || remainingMsq > 0 || remainingNat > 0) {
            int batchMcq = 0;
            int batchMsq = 0;
            int batchNat = 0;
            int currentSize = 0;

            if (remainingMcq > 0) {
                batchMcq = Math.min(remainingMcq, maxBatchSize - currentSize);
                currentSize += batchMcq;
                remainingMcq -= batchMcq;
            }
            if (currentSize < maxBatchSize && remainingMsq > 0) {
                batchMsq = Math.min(remainingMsq, maxBatchSize - currentSize);
                currentSize += batchMsq;
                remainingMsq -= batchMsq;
            }
            if (currentSize < maxBatchSize && remainingNat > 0) {
                batchNat = Math.min(remainingNat, maxBatchSize - currentSize);
                currentSize += batchNat;
                remainingNat -= batchNat;
            }

            batches.add(new BatchRequest(batchMcq, batchMsq, batchNat));
        }

        String contextQuestions = IntStream.range(0, contextDocs.size())
            .mapToObj(idx -> (idx + 1) + ". " + contextDocs.get(idx).getText())
            .collect(Collectors.joining("\n\n"));

        for (int bIdx = 0; bIdx < batches.size(); bIdx++) {
            BatchRequest batch = batches.get(bIdx);
            int batchNum = bIdx + 1;
            String progressMsg = String.format("[Gemini] Sending context-grounded prompt to Gemini 2.5 Flash (Batch %d/%d)...", batchNum, batches.size());
            progressCallback.accept(String.format("{\"step\": %d, \"message\": \"%s\", \"percent\": %d}", stepNo, progressMsg, percent));

            String prompt = loadSegmentTemplate()
                .replace("{segmentName}", segmentName)
                .replace("{subjects}", subjects)
                .replace("{syllabusFocus}", syllabusFocus)
                .replace("{startSequenceNo}", String.valueOf(currentSeq))
                .replace("{mcqCount}", String.valueOf(batch.mcq))
                .replace("{msqCount}", String.valueOf(batch.msq))
                .replace("{natCount}", String.valueOf(batch.nat))
                .replace("{contextCount}", String.valueOf(contextDocs.size()))
                .replace("{contextQuestions}", contextQuestions);

            int totalQuestionCount = batch.mcq + batch.msq + batch.nat;
            log.info("[Generation] Starting AI call for topic: {} (Batch {}/{}), questionCount: {}", 
                segmentName, batchNum, batches.size(), totalQuestionCount);

            String rawJson;
            List<AiGeneratedQuestionDto> batchQuestionsList = null;
            try {
                rawJson = geminiService.generateJsonContent(prompt);
                log.info("[Generation] AI response received, length: {} chars", rawJson != null ? rawJson.length() : 0);
                
                String cleaned = extractJsonArray(rawJson);
                batchQuestionsList = objectMapper.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<List<AiGeneratedQuestionDto>>() {});
            } catch (Exception e) {
                log.warn("Gemini returned invalid JSON or timed out for segment {} (Batch {}/{}), retrying once with temperature 0.3...", 
                    segmentName, batchNum, batches.size(), e);
                progressCallback.accept(String.format("{\"step\": %d, \"message\": \"[Warning] Gemini returned invalid JSON for Batch %d/%d. Retrying...\", \"percent\": %d}", 
                    stepNo, batchNum, batches.size(), percent));
                
                try {
                    rawJson = geminiService.generateJsonContent(prompt, 0.3);
                    log.info("[Generation] AI response received on retry, length: {} chars", rawJson != null ? rawJson.length() : 0);
                    
                    String cleaned = extractJsonArray(rawJson);
                    batchQuestionsList = objectMapper.readValue(cleaned, new com.fasterxml.jackson.core.type.TypeReference<List<AiGeneratedQuestionDto>>() {});
                } catch (Exception ex) {
                    log.error("Gemini failed twice for segment {} (Batch {}/{})", segmentName, batchNum, batches.size(), ex);
                    throw new RuntimeException("Gemini mock test generation failed on batch retry: " + ex.getMessage(), ex);
                }
            }

            // Notify parsing and validating
            progressCallback.accept(String.format("{\"step\": %d, \"message\": \"Parsing and validating Batch %d/%d...\", \"percent\": %d}", stepNo, batchNum, batches.size(), percent));

            // Record token usage and emit
            try {
                com.gate.mockexam.entity.GeminiTokenUsage lastUsage = geminiUsageService.getLastUsageRecord();
                if (lastUsage != null) {
                    double cost = lastUsage.getTotalTokens() * 0.0000025;
                    progressCallback.accept(String.format("{\"step\": %d, \"message\": \"[Gemini] Batch %d/%d · Tokens used: %d · Est. cost: $%.6f\", \"percent\": %d}", 
                        stepNo, batchNum, batches.size(), lastUsage.getTotalTokens(), cost, percent));
                }
            } catch (Exception ex) {
                log.warn("Could not log token usage during progress callback: {}", ex.getMessage());
            }

            if (batchQuestionsList != null) {
                // Validate sequence numbering in response
                for (AiGeneratedQuestionDto qDto : batchQuestionsList) {
                    if (qDto.getSequenceNo() != currentSeq) {
                        qDto.setSequenceNo(currentSeq);
                    }
                    currentSeq += 1;
                }
                allQuestions.addAll(batchQuestionsList);
            }
        }

        return allQuestions;
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


    @lombok.Data
    public static class AiGeneratedQuestionsListDto {
        private List<AiGeneratedQuestionDto> questions;
    }
}
