package com.gate.mockexam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.service.DocumentParserService;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.RagIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rag")
@Slf4j
@RequiredArgsConstructor
public class AdminRagController {

    private final RagIngestionService ragIngestionService;
    private final DocumentParserService documentParserService;
    private final MockTestGenerationService mockTestGenerationService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;



    /**
     * POST /api/admin/rag/upload (multipart)
     * Parses PDF/TXT, extracts & aligns questions with answer key using Ollama AI.
     * Returns the aligned draft as JSON — client holds this in state for review.
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

            // Extract Answer Key PDF text if uploaded
            String extractedAnswerKeyText = "";
            if (answerKeyFile != null && !answerKeyFile.isEmpty()) {
                log.info("Processing uploaded Answer Key file: {}", answerKeyFile.getOriginalFilename());
                if (Objects.requireNonNull(answerKeyFile.getOriginalFilename()).endsWith(".pdf")) {
                    extractedAnswerKeyText = documentParserService.parsePdf(answerKeyFile.getBytes());
                } else {
                    extractedAnswerKeyText = documentParserService.parseTxt(answerKeyFile.getBytes());
                }
            }

            // Parse advanced answer keys into a structured map (SECTION_QNO -> AnswerKeyEntry)
            Map<String, DocumentParserService.AnswerKeyEntry> answerKeyMap = new HashMap<>();
            if (!extractedAnswerKeyText.isEmpty()) {
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(extractedAnswerKeyText));
            }
            if (answerKeyText != null && !answerKeyText.isBlank()) {
                answerKeyMap.putAll(documentParserService.parseAnswerKeyToMap(answerKeyText));
            }
            log.info("Total parsed answer key entries: {}", answerKeyMap.size());

            // Split Question Paper into logical page segments (3 pages with 1 page overlap)
            List<String> chunks = new ArrayList<>();
            if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".pdf")) {
                List<String> pages = documentParserService.parsePdfPages(file.getBytes());
                log.info("Total PDF pages: {}", pages.size());
                int step = 2;
                int chunkSize = 3;
                for (int startPage = 0; startPage < pages.size(); startPage += step) {
                    StringBuilder currentChunk = new StringBuilder();
                    int endPage = Math.min(startPage + chunkSize, pages.size());
                    for (int p = startPage; p < endPage; p++) {
                        currentChunk.append(pages.get(p)).append("\n--- PAGE ").append(p + 1).append(" ---\n");
                    }
                    chunks.add(currentChunk.toString());
                    if (endPage == pages.size()) {
                        break;
                    }
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

            List<AiGeneratedQuestionDto> allQuestions = new ArrayList<>();
            log.info("Processing past paper document in {} segmented chunks", chunks.size());

            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                String chunkText = chunks.get(chunkIndex);
                log.info("Processing segment {}/{} of past paper", chunkIndex + 1, chunks.size());

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

                try {
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
                        allQuestions.addAll(alignedChunk.getQuestions());
                        log.info("Extracted {} questions from segment {}", alignedChunk.getQuestions().size(), chunkIndex + 1);
                    }
                } catch (Exception segmentEx) {
                    log.error("Failed to parse segment {}: {}", chunkIndex + 1, segmentEx.getMessage());
                }
            }

            if (allQuestions.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of("error",
                        "No questions could be successfully parsed from the document segments. Please verify the PDF format."));
            }

            // Deduplicate questions from overlapping chunks: group by section + "_" + sequenceNo
            Map<String, List<AiGeneratedQuestionDto>> grouped = allQuestions.stream()
                    .filter(q -> q.getSequenceNo() > 0 && q.getSection() != null)
                    .collect(Collectors.groupingBy(q -> q.getSection().toUpperCase() + "_" + q.getSequenceNo()));

            List<AiGeneratedQuestionDto> uniqueQuestions = new ArrayList<>();
            for (Map.Entry<String, List<AiGeneratedQuestionDto>> entry : grouped.entrySet()) {
                List<AiGeneratedQuestionDto> duplicates = entry.getValue();
                // Find candidate with longest question text and most options
                AiGeneratedQuestionDto best = duplicates.stream()
                        .max(Comparator.comparingInt((AiGeneratedQuestionDto q) -> q.getQuestionText() != null ? q.getQuestionText().length() : 0)
                                .thenComparingInt(q -> q.getOptions() != null ? q.getOptions().size() : 0))
                        .orElse(null);
                if (best != null) {
                    uniqueQuestions.add(best);
                }
            }

            // Programmatically align each question with the Answer Key map
            for (AiGeneratedQuestionDto q : uniqueQuestions) {
                String sec = q.getSection().toUpperCase().trim();
                int qNo = q.getSequenceNo();
                String key = sec + "_" + qNo;

                // Set fallback defaults
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
                        
                        // Parse correct labels
                        Set<String> correctLabels = new HashSet<>();
                        if (entry.getCorrectKey() != null) {
                            Matcher m = Pattern.compile("[A-D]").matcher(entry.getCorrectKey().toUpperCase());
                            while (m.find()) {
                                correctLabels.add(m.group());
                            }
                        }

                        if (q.getOptions() == null) {
                            q.setOptions(new ArrayList<>());
                        }
                        
                        // Mark correct options
                        for (AiGeneratedOptionDto opt : q.getOptions()) {
                            if (opt.getLabel() != null) {
                                opt.setCorrect(correctLabels.contains(opt.getLabel().toUpperCase()));
                            }
                        }
                    } else if ("NAT".equals(entry.getType())) {
                        q.setNegativeMarks(0.0);
                        q.setOptions(new ArrayList<>());
                        
                        // Parse NAT ranges/values
                        List<Double> nums = new ArrayList<>();
                        if (entry.getCorrectKey() != null) {
                            Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(entry.getCorrectKey());
                            while (m.find()) {
                                try {
                                    nums.add(Double.parseDouble(m.group()));
                                } catch (NumberFormatException nfe) {
                                    // ignore
                                }
                            }
                        }
                        if (nums.size() >= 2) {
                            double low = nums.get(0);
                            double high = nums.get(1);
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
                    // Default negative marking if key entry not found but type is MCQ
                    if ("MCQ".equals(q.getType())) {
                        q.setNegativeMarks(q.getMarks() == 1.0 ? 0.33 : 0.67);
                    } else {
                        q.setNegativeMarks(0.0);
                    }
                }
            }

            // Sort questions: GA (1 to 10) first, then CS (1 to 55)
            uniqueQuestions.sort((q1, q2) -> {
                String sec1 = q1.getSection().toUpperCase().trim();
                String sec2 = q2.getSection().toUpperCase().trim();
                if (!sec1.equals(sec2)) {
                    return sec1.equals("GA") ? -1 : 1;
                }
                return Integer.compare(q1.getSequenceNo(), q2.getSequenceNo());
            });

            // Adjust global sequenceNo after sorting (so they are sequential 1 to 65 for the final exam)
            for (int i = 0; i < uniqueQuestions.size(); i++) {
                uniqueQuestions.get(i).setSequenceNo(i + 1);
            }

            // Build aggregated final DTO
            AiGeneratedTestDto alignedTest = new AiGeneratedTestDto();
            alignedTest.setTitle("Official GATE Past Paper: " + topic + " (" + subject + ")");
            alignedTest.setSubject(subject);
            alignedTest.setTopic(topic);
            alignedTest.setDurationMinutes(180);
            alignedTest.setQuestions(uniqueQuestions);

            // Return draft JSON directly — client stores it in React state
            return ResponseEntity.ok(alignedTest);

        } catch (Exception e) {
            log.error("Failed to parse and align past paper: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to parse document: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/rag/confirm
     * Accepts the aligned draft JSON from client, persists to DB and embeds to PGVector.
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmIngestion(@RequestBody AiGeneratedTestDto draft) {
        if (draft == null || draft.getQuestions() == null || draft.getQuestions().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Draft is empty or missing questions."));
        }

        try {
            // Step 1: Persist questions relationally
            MockTest test = mockTestGenerationService.persistTest(draft);

            // Step 2: Embed in PGVector
            List<Document> documents = new ArrayList<>();
            for (AiGeneratedQuestionDto q : draft.getQuestions()) {
                String dynamicSubject = q.getSubject() != null && !q.getSubject().trim().isEmpty() ? q.getSubject() : draft.getSubject();
                String dynamicTopic = q.getTopic() != null && !q.getTopic().trim().isEmpty() ? q.getTopic() : draft.getTopic();

                String content = String.format(
                        "Subject: %s | Topic: %s | Type: %s\nQuestion: %s\nExplanation: %s",
                        dynamicSubject, dynamicTopic, q.getType(),
                        q.getQuestionText(),
                        q.getExplanation() != null ? q.getExplanation() : ""
                );

                java.util.Map<String, Object> metadata = java.util.Map.of(
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
                    "message", "Successfully committed " + draft.getQuestions().size() + " questions and populated PGVector embeddings!",
                    "testId", test.getId().toString()
            ));

        } catch (Exception e) {
            log.error("Failed to commit aligned paper to PGVector: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to ingest: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/rag/test?query=...&topK=N
     * Similarity search playground endpoint.
     */
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
}
