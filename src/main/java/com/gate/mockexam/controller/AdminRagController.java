package com.gate.mockexam.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.service.DocumentParserService;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.RagIngestionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// @Controller
// @RequestMapping("/admin/rag")
@Slf4j
@RequiredArgsConstructor
public class AdminRagController {

    private final RagIngestionService ragIngestionService;
    private final DocumentParserService documentParserService;
    private final MockTestGenerationService mockTestGenerationService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String ragDashboard(Model model) {
        model.addAttribute("vectorCount", ragIngestionService.getVectorCount());
        return "admin/rag";
    }

    @PostMapping("/upload")
    public String handlePdfUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "answerKeyFile", required = false) MultipartFile answerKeyFile,
            @RequestParam("subject") String subject,
            @RequestParam("topic") String topic,
            @RequestParam("answerKeyText") String answerKeyText,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a GATE Question Paper PDF to upload.");
            return "redirect:/admin/rag";
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

            // Parse manual answer key text if provided
            Map<Integer, List<String>> parsedKeys = documentParserService.parseAnswerKey(answerKeyText);
            String keysJson = objectMapper.writeValueAsString(parsedKeys);

            // Split Question Paper into logical page segments
            List<String> chunks = new ArrayList<>();
            if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".pdf")) {
                List<String> pages = documentParserService.parsePdfPages(file.getBytes());
                StringBuilder currentChunk = new StringBuilder();
                for (int i = 0; i < pages.size(); i++) {
                    currentChunk.append(pages.get(i)).append("\n--- PAGE ").append(i+1).append(" ---\n");
                    if ((i + 1) % 5 == 0 || i == pages.size() - 1) {
                        chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder();
                    }
                }
            } else {
                // Split by character length for text files
                String text = documentParserService.parseTxt(file.getBytes());
                int len = text.length();
                int start = 0;
                while (start < len) {
                    chunks.add(text.substring(start, Math.min(start + 15000, len)));
                    start += 15000;
                }
            }

            List<AiGeneratedQuestionDto> allQuestions = new ArrayList<>();
            log.info("Processing past paper document in {} segmented chunks to ensure complete coverage", chunks.size());
            
            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                String chunkText = chunks.get(chunkIndex);
                log.info("Processing segment {}/{} of past paper", chunkIndex + 1, chunks.size());

                String segmentPrompt = String.format("""
                    You are a GATE Question Paper and Answer Key Alignment Parser. You are given:
                    1. A segment of a GATE past paper (Pages %d to %d equivalent)
                    2. Raw extracted text of the Official Answer Key PDF (if provided)
                    3. A manual list of the Answer Key mapped by question number (if provided)
                    
                    --- RAW QUESTION PAPER SEGMENT TEXT ---
                    %s
                    
                    --- RAW ANSWER KEY DOCUMENT TEXT ---
                    %s
                    
                    --- MANUAL ANSWER KEY LIST MAP ---
                    %s
                    
                    Extract all questions present in this segment. Identify their type (MCQ, MSQ, or NAT) and align them with the correct official answer.
                    Output a valid JSON matching this schema:
                    {
                      "questions": [
                        {
                          "sequenceNo": 1,
                          "type": "MCQ",
                          "questionText": "...",
                          "marks": 1,
                          "negativeMarks": 0.33,
                          "explanation": "Official Answer derived from key.",
                          "subject": "Auto-classified subject based on academic context (e.g., Operating Systems, Databases, Algorithms)",
                          "topic": "Auto-classified topic based on academic context (e.g., CPU Scheduling, SQL Queries, Asymptotic Notation)",
                          "options": [
                            {"label": "A", "text": "...", "isCorrect": true},
                            {"label": "B", "text": "...", "isCorrect": false},
                            {"label": "C", "text": "...", "isCorrect": false},
                            {"label": "D", "text": "...", "isCorrect": false}
                          ]
                        }
                      ]
                    }
                    
                    Ensure:
                    - Return ONLY clean raw JSON. No markdown backticks.
                    - CRITICAL: Extract ONLY the actual, real academic exam questions physically written inside "RAW QUESTION PAPER SEGMENT TEXT".
                    - CRITICAL: Do NOT generate or invent any dummy questions, placeholder questions, general knowledge questions (e.g., "Capital of France", "prime number", "chemical symbol for water", "Red Planet"), or repeat the same question multiple times.
                    - If no questions are found in this text segment, return an empty array: {"questions": []}
                    - If a segment contains only a few questions, extract exactly only those questions.
                    - MCQ: exactly 4 options. Correct option label MUST match the Answer Key source context.
                    - MSQ: multiple options marked correct based on Answer Key source context.
                    - NAT: no options, set correctNatValue to the exact value from the Answer Key source.
                    - You MUST auto-classify the academic subject (e.g., 'Operating Systems', 'Databases', 'Algorithms') and topic (e.g., 'CPU Scheduling', 'SQL Queries', 'Asymptotic Notation') for every single question in the array.
                    """,
                    (chunkIndex * 5) + 1,
                    Math.min((chunkIndex + 1) * 5, chunks.size() * 5),
                    chunkText,
                    extractedAnswerKeyText.isEmpty() ? "No Answer Key PDF uploaded." : extractedAnswerKeyText.substring(0, Math.min(extractedAnswerKeyText.length(), 4000)),
                    keysJson.equals("{}") ? "No manual list provided." : keysJson
                );

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
                throw new IllegalStateException("No questions could be successfully parsed from the document segments. Please verify the PDF format.");
            }

            // Build aggregated final DTO
            AiGeneratedTestDto alignedTest = new AiGeneratedTestDto();
            alignedTest.setTitle("Official GATE Past Paper: " + topic + " (" + subject + ")");
            alignedTest.setSubject(subject); // overall domain e.g. CSE
            alignedTest.setTopic(topic);     // overall year e.g. 2019
            alignedTest.setDurationMinutes(180);
            alignedTest.setQuestions(allQuestions);

            // Save aligned test DTO inside Session for confirmation
            session.setAttribute("alignedTestDraft", alignedTest);
            redirectAttributes.addFlashAttribute("success", String.format("Successfully parsed and aligned the past paper! Extracted a total of %d questions across all PDF pages.", allQuestions.size()));
            return "redirect:/admin/rag/review";

        } catch (Exception e) {
            log.error("Failed to parse and align past paper: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to parse document: " + e.getMessage());
            return "redirect:/admin/rag";
        }
    }

    @GetMapping("/review")
    public String reviewAlignedTest(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        AiGeneratedTestDto draft = (AiGeneratedTestDto) session.getAttribute("alignedTestDraft");
        if (draft == null) {
            redirectAttributes.addFlashAttribute("error", "No parsed draft found in session.");
            return "redirect:/admin/rag";
        }
        model.addAttribute("draft", draft);
        return "admin/rag-review";
    }

    @PostMapping("/confirm")
    public String confirmIngestion(HttpSession session, RedirectAttributes redirectAttributes) {
        AiGeneratedTestDto draft = (AiGeneratedTestDto) session.getAttribute("alignedTestDraft");
        if (draft == null) {
            redirectAttributes.addFlashAttribute("error", "Session draft expired. Please re-upload.");
            return "redirect:/admin/rag";
        }

        try {
            // Step 1: Persist questions relationally so students can take the exam
            MockTest test = mockTestGenerationService.persistTest(draft);
            
            // Step 2: Convert to Spring AI documents and embed inside PGVector
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

            session.removeAttribute("alignedTestDraft");
            redirectAttributes.addFlashAttribute("success", "Successfully committed official paper and populated its embeddings inside PGVector store!");
            return "redirect:/admin/rag";

        } catch (Exception e) {
            log.error("Failed to commit aligned paper to PGVector: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to ingest: " + e.getMessage());
            return "redirect:/admin/rag";
        }
    }

    @PostMapping("/test")
    @ResponseBody
    public List<Map<String, Object>> testSimilarityQuery(@RequestParam("query") String query, @RequestParam("topK") int topK) {
        log.info("Testing similarity query: '{}' topK: {}", query, topK);
        List<Document> matches = ragIngestionService.retrieveSimilarQuestions(query, topK);
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Document doc : matches) {
            Map<String, Object> map = Map.of(
                "content", doc.getText(),
                "metadata", doc.getMetadata()
            );
            results.add(map);
        }
        return results;
    }
}
