package com.gate.mockexam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.service.DocumentParserService;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.RagIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;
    private final ExecutorService chunkExecutor;

    /** Semaphore caps concurrent Ollama HTTP calls to 3 — prevents VRAM OOM. */
    private final Semaphore ollamaSemaphore = new Semaphore(3);

    @Value("${spring.ai.ollama.chat.options.model:qwen2.5-coder:7b}")
    private String chatModel;

    public AdminRagController(
            RagIngestionService ragIngestionService,
            DocumentParserService documentParserService,
            MockTestGenerationService mockTestGenerationService,
            ChatClient chatClient,
            ObjectMapper objectMapper,
            QuestionRepository questionRepository,
            @Qualifier("ollamaChunkExecutor") ExecutorService chunkExecutor) {
        this.ragIngestionService = ragIngestionService;
        this.documentParserService = documentParserService;
        this.mockTestGenerationService = mockTestGenerationService;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.questionRepository = questionRepository;
        this.chunkExecutor = chunkExecutor;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/admin/rag/upload
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses PDF/TXT, extracts & aligns questions with answer key using Ollama AI.
     * Chunk-level LLM calls run in parallel (max 3 concurrent) via CompletableFuture.
     * All deduplication, answer-key binding, sorting, and sequence-number logic
     * is preserved exactly as before.
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
            if (!extractedAnswerKeyText.isEmpty()) {
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(extractedAnswerKeyText));
            }
            if (answerKeyText != null && !answerKeyText.isBlank()) {
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(answerKeyText));
            }
            log.info("Total parsed answer key entries: {}", answerKeyMap.size());

            // ── Build page chunks ────────────────────────────────────────────
            List<String> chunks = new ArrayList<>();
            if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".pdf")) {
                List<String> pages = documentParserService.parsePdfPages(file.getBytes());
                log.info("Total PDF pages: {}", pages.size());
                int step = 1;
                int chunkSize = 2;
                for (int startPage = 0; startPage < pages.size(); startPage += step) {
                    StringBuilder currentChunk = new StringBuilder();
                    int endPage = Math.min(startPage + chunkSize, pages.size());
                    for (int p = startPage; p < endPage; p++) {
                        currentChunk.append(pages.get(p)).append("\n--- PAGE ").append(p + 1).append(" ---\n");
                    }
                    chunks.add(currentChunk.toString());
                    if (endPage == pages.size()) break;
                }
            } else {
                String text = documentParserService.parseTxt(file.getBytes());
                int len = text.length();
                int start = 0;
                int size = 15000;
                int overlap = 3000;
                while (start < len) {
                    int end = Math.min(start + size, len);
                    chunks.add(text.substring(start, end));
                    if (end == len) break;
                    start += (size - overlap);
                }
            }

            log.info("Processing past paper document in {} segmented chunks (parallel, max 3 concurrent)", chunks.size());

            // ── TRACK 1: Parallel chunk → Ollama calls ───────────────────────
            List<CompletableFuture<List<AiGeneratedQuestionDto>>> futures = new ArrayList<>();

            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                final String chunkText = chunks.get(chunkIndex);
                final int idx = chunkIndex;

                CompletableFuture<List<AiGeneratedQuestionDto>> future =
                        CompletableFuture.supplyAsync(() -> transcribeChunk(chunkText, idx, chunks.size()), chunkExecutor);

                futures.add(future);
            }

            // Wait for all chunk transcriptions to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<AiGeneratedQuestionDto> allQuestions = new ArrayList<>();
            for (CompletableFuture<List<AiGeneratedQuestionDto>> f : futures) {
                allQuestions.addAll(f.get()); // already done, no blocking
            }

            if (allQuestions.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of("error",
                        "No questions could be successfully parsed from the document segments. Please verify the PDF format."));
            }

            // ── Deduplication: longest text wins ────────────────────────────
            Map<String, List<AiGeneratedQuestionDto>> grouped = allQuestions.stream()
                    .filter(q -> q.getSequenceNo() > 0 && q.getSection() != null)
                    .collect(Collectors.groupingBy(q -> q.getSection().toUpperCase() + "_" + q.getSequenceNo()));

            List<AiGeneratedQuestionDto> uniqueQuestions = new ArrayList<>();
            for (Map.Entry<String, List<AiGeneratedQuestionDto>> entry : grouped.entrySet()) {
                AiGeneratedQuestionDto best = entry.getValue().stream()
                        .max(Comparator.comparingInt((AiGeneratedQuestionDto q) ->
                                q.getQuestionText() != null ? q.getQuestionText().length() : 0)
                                .thenComparingInt(q -> q.getOptions() != null ? q.getOptions().size() : 0))
                        .orElse(null);
                if (best != null) uniqueQuestions.add(best);
            }

            // ── Answer-key binding (unchanged logic) ─────────────────────────
            for (AiGeneratedQuestionDto q : uniqueQuestions) {
                String sec = q.getSection().toUpperCase().trim();
                int qNo = q.getSequenceNo();
                String key = sec + "_" + qNo;

                q.setMarks(qNo <= 5 && sec.equals("GA") || qNo <= 25 && sec.equals("CS") ? 1.0 : 2.0);
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
                String sec1 = q1.getSection().toUpperCase().trim();
                String sec2 = q2.getSection().toUpperCase().trim();
                if (!sec1.equals(sec2)) return sec1.equals("GA") ? -1 : 1;
                return Integer.compare(q1.getSequenceNo(), q2.getSequenceNo());
            });
            for (int i = 0; i < uniqueQuestions.size(); i++) {
                uniqueQuestions.get(i).setSequenceNo(i + 1);
            }

            AiGeneratedTestDto alignedTest = new AiGeneratedTestDto();
            alignedTest.setTitle("Official GATE Past Paper: " + topic + " (" + subject + ")");
            alignedTest.setSubject(subject);
            alignedTest.setTopic(topic);
            alignedTest.setDurationMinutes(180);
            alignedTest.setQuestions(uniqueQuestions);

            return ResponseEntity.ok(alignedTest);

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
     * (TRACK 5), then embeds questions (with explanations) to PGVector.
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmIngestion(@RequestBody AiGeneratedTestDto draft) {
        if (draft == null || draft.getQuestions() == null || draft.getQuestions().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Draft is empty or missing questions."));
        }

        try {
            // Step 1: Persist questions relationally
            MockTest test = mockTestGenerationService.persistTest(draft);

            // Step 2: TRACK 5 — Async explanation generation pass
            //   Launch parallel explanation calls; join before ingesting into PGVector
            //   so the vector store Document includes the explanation text.
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

                // TRACK 5: explanation now included in vector store content
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
     * TRACK 1: Semaphore-guarded Ollama call for a single page chunk.
     * Acquires a permit before the HTTP call, releases in finally.
     */
    private List<AiGeneratedQuestionDto> transcribeChunk(String chunkText, int idx, int total) {
        try {
            ollamaSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for Ollama semaphore on chunk {}", idx + 1);
            return Collections.emptyList();
        }
        try {
            log.info("Processing segment {}/{} of past paper", idx + 1, total);
            String segmentPrompt = String.format("""
                You are a GATE Exam Question Extractor.
                Analyze the following segment of a GATE past paper and extract all written exam questions.
                
                --- RAW QUESTION PAPER SEGMENT TEXT ---
                %s
                
                For each question, extract:
                1. "sequenceNo": The printed question number (e.g., 1, 2, 3...).
                2. "section": The section it belongs to: "GA" (General Aptitude, usually questions 1-10 at the start of the exam) or "CS" (Computer Science, usually questions 1-55). If not explicitly written, infer from context.
                3. "type": "MCQ" (multiple choice with options), "MSQ" (multiple select with options), or "NAT" (numerical answer type, no options).
                4. "questionText": The complete and exact text of the question, including any context or inline equations. Do not truncate or use placeholders.
                5. "options": For MCQ/MSQ, list all options with:
                   - "label": "A", "B", "C", "D", etc.
                   - "text": The option content. Do not truncate or use placeholders.
                   For NAT, leave this list empty or null.
                   
                Output a valid JSON matching this schema:
                {
                  "questions": [
                    {
                      "sequenceNo": 1,
                      "section": "GA",
                      "type": "MCQ",
                      "questionText": "...",
                      "options": [
                        {"label": "A", "text": "..."},
                        {"label": "B", "text": "..."},
                        {"label": "C", "text": "..."},
                        {"label": "D", "text": "..."}
                      ]
                    }
                  ]
                }
                
                Ensure:
                - Return ONLY clean raw JSON. No markdown backticks or explanation.
                - Extract ONLY the actual academic exam questions physically written inside the segment text. Do NOT make up any questions.
                - If no questions are found in this text segment, return: {"questions": []}
                - Pay close attention to equations, symbols, and complete text. Never truncate text with "..." in the JSON fields.
                """, chunkText);

            String aiResponse = chatClient.prompt()
                    .user(segmentPrompt)
                    .call()
                    .content();

            String cleaned = aiResponse.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("```$", "")
                    .trim();

            AiGeneratedTestDto alignedChunk = objectMapper.readValue(cleaned, AiGeneratedTestDto.class);
            if (alignedChunk != null && alignedChunk.getQuestions() != null) {
                log.info("Extracted {} questions from segment {}", alignedChunk.getQuestions().size(), idx + 1);
                return alignedChunk.getQuestions();
            }
        } catch (Exception e) {
            log.error("Failed to parse segment {}: {}", idx + 1, e.getMessage());
        } finally {
            ollamaSemaphore.release();
        }
        return Collections.emptyList();
    }

    /**
     * TRACK 5: Generates an explanation for a single question via Ollama.
     * Max 300 tokens. Returns null on any error — never throws.
     */
    private String generateExplanation(AiGeneratedQuestionDto q) {
        try {
            ollamaSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
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

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return response != null ? response.trim() : null;
        } catch (Exception e) {
            log.warn("Explanation generation failed for question seq={}: {}", q.getSequenceNo(), e.getMessage());
            return null;
        } finally {
            ollamaSemaphore.release();
        }
    }
}
