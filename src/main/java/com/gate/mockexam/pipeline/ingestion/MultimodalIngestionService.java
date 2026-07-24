package com.gate.mockexam.pipeline.ingestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.config.MinioStorageService;
import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.repository.*;
import com.gate.mockexam.service.GeminiUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v2.1 — Multimodal Ingestion Service (Pass 1).
 *
 * Replaces BOTH:
 *   - ExtractionPipelineService (which called Python OCR)
 *   - The 4 merged enrichment workers (Metadata, Explanation, Concept, Hint)
 *
 * A SINGLE Gemini call reads the raw PDF natively (no OCR pre-processing)
 * and simultaneously:
 *   1. Extracts all question text and options
 *   2. Solves each question (derives correct answer)
 *   3. Enriches with explanation, difficulty, bloom's level, hints, etc.
 *   4. Detects diagram bounding boxes
 *
 * Total Gemini calls per paper = 2 (this pass + QualityReviewWorker pass)
 * vs. 5 × N calls in v1.0.
 */
@Service
@Slf4j
public class MultimodalIngestionService {

    private final PaperRepository paperRepository;
    private final GateQuestionRepository questionRepository;
    private final GateOptionRepository optionRepository;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;
    private final GeminiUsageService geminiUsageService;
    private final RestClient restClient;

    private final String apiKey;
    private final String solveModel;

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com";

    public MultimodalIngestionService(
            PaperRepository paperRepository,
            GateQuestionRepository questionRepository,
            GateOptionRepository optionRepository,
            AssetRepository assetRepository,
            ObjectMapper objectMapper,
            GeminiUsageService geminiUsageService,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model.solve:gemini-3.5-flash}") String solveModel) {
        this.paperRepository = paperRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.assetRepository = assetRepository;
        this.objectMapper = objectMapper;
        this.geminiUsageService = geminiUsageService;
        this.apiKey = apiKey.trim();
        this.solveModel = solveModel;

        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(600_000); // 10 min — full paper PDF may be large
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Main entry point: ingest a GATE paper PDF.
     *
     * @param paperId       unique ID (e.g. "gate_cse_2020")
     * @param examName      e.g. "GATE CSE 2020"
     * @param year          exam year
     * @param branch        e.g. "CSE"
     * @param pdfBytes      raw bytes of the question paper PDF
     * @return              persisted Paper entity (status = Extracted)
     */
    @Transactional
    public Paper ingest(String paperId, String examName, int year, String branch, byte[] pdfBytes) {
        log.info("=== MULTIMODAL INGESTION START: paperId={} size={}KB ===", paperId, pdfBytes.length / 1024);

        geminiUsageService.checkDailyLimit();

        // ── Pass 1: Send PDF to Gemini, get structured extraction+enrichment ─
        String rawJson = callGeminiWithPdf(pdfBytes, paperId);
        List<IngestedQuestionResult> results = parseGeminiResponse(rawJson, paperId);
        log.info("[Ingestion] Gemini returned {} question results for paperId={}", results.size(), paperId);

        // ── Persist Paper entity ─────────────────────────────────────────────
        Paper paper = Paper.builder()
                .paperId(paperId)
                .examName(examName)
                .year(year)
                .branch(branch)
                .totalQuestions(results.size())
                .paperType("Official")
                .status("Extracted")
                .uploadedAt(LocalDateTime.now())
                .build();
        paperRepository.save(paper);
        log.info("[Ingestion] Saved Paper: {}", paperId);

        // ── Persist questions and options ────────────────────────────────────
        int savedCount = 0;
        int failedCount = 0;
        for (IngestedQuestionResult result : results) {
            if (!result.isSuccess()) {
                log.warn("[Ingestion] Skipping failed result: {}", result.getFailureReason());
                failedCount++;
                continue;
            }
            try {
                persistQuestion(paper, result);
                savedCount++;
            } catch (Exception e) {
                log.error("[Ingestion] Failed to persist question {}: {}",
                        result.getQuestionNumber(), e.getMessage(), e);
                failedCount++;
            }
        }

        paper.setTotalQuestions(savedCount);
        paper.setStatus(failedCount == 0 ? "Extracted" : "PartiallyExtracted");
        paperRepository.save(paper);

        log.info("=== MULTIMODAL INGESTION COMPLETE: paperId={} saved={} failed={} ===",
                paperId, savedCount, failedCount);
        return paper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gemini API call — sends PDF inline as base64
    // ─────────────────────────────────────────────────────────────────────────

    private String callGeminiWithPdf(byte[] pdfBytes, String paperId) {
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        String url = GEMINI_API_BASE + "/v1beta/models/" + solveModel + ":generateContent?key=" + apiKey;

        String prompt = buildExtractionPrompt();

        // Gemini inline PDF part
        Map<String, Object> pdfPart = Map.of(
                "inline_data", Map.of(
                        "mime_type", "application/pdf",
                        "data", base64Pdf
                )
        );
        Map<String, Object> textPart = Map.of("text", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(pdfPart, textPart)
        )));
        body.put("generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.1,
                "maxOutputTokens", 65536
        ));

        try {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("[Ingestion] Calling Gemini ({}) with PDF of size {}KB", solveModel, pdfBytes.length / 1024);

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);

            // Handle empty/blocked responses (e.g. safety filters or invalid model)
            JsonNode candidatesNode = root.path("candidates");
            if (candidatesNode.isMissingNode() || !candidatesNode.isArray() || candidatesNode.size() == 0) {
                // Log the full response for diagnostics
                String promptFeedback = root.path("promptFeedback").toString();
                String fullResp = response.length() > 500 ? response.substring(0, 500) + "..." : response;
                log.error("[Ingestion] Gemini returned empty candidates for paperId={}. promptFeedback={} fullResponse={}",
                        paperId, promptFeedback, fullResp);
                throw new RuntimeException("Gemini returned no candidates. Possible causes: invalid model name, safety filter block, or quota exceeded. promptFeedback=" + promptFeedback);
            }

            JsonNode candidate = candidatesNode.get(0);
            JsonNode partsNode = candidate.path("content").path("parts");
            if (partsNode.isMissingNode() || !partsNode.isArray() || partsNode.size() == 0) {
                String finishReason = candidate.path("finishReason").asText("UNKNOWN");
                log.error("[Ingestion] Gemini candidate has no parts for paperId={}, finishReason={}", paperId, finishReason);
                throw new RuntimeException("Gemini candidate has no content parts. finishReason=" + finishReason);
            }

            String text = partsNode.get(0).path("text").asText();

            // Record usage
            JsonNode usage = root.path("usageMetadata");
            if (!usage.isMissingNode()) {
                geminiUsageService.recordUsage(
                        "MULTIMODAL_INGEST",
                        usage.path("promptTokenCount").asInt(0),
                        usage.path("candidatesTokenCount").asInt(0)
                );
            }

            log.info("[Ingestion] Gemini response received for paperId={}", paperId);
            return text;

        } catch (Exception e) {
            log.error("[Ingestion] Gemini call failed for paperId={}: {}", paperId, e.getMessage(), e);
            throw new RuntimeException("Multimodal ingestion Gemini call failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt — instructs Gemini to extract + solve + enrich in one pass
    // ─────────────────────────────────────────────────────────────────────────

    private String buildExtractionPrompt() {
        return """
                You are an expert GATE (Graduate Aptitude Test in Engineering) exam digitizer
                and solver. The attached PDF is an official GATE CSE question paper.
                
                YOUR TASK — do ALL of the following in a single pass:
                1. READ every question from the paper exactly as printed.
                2. SOLVE each question — derive the correct answer using your knowledge.
                3. SELF-SCORE your confidence for each answer (0.0 to 1.0).
                4. ENRICH each question with metadata, explanation, and pedagogical data.
                5. DETECT if any question contains a diagram, graph, circuit, tree,
                   automaton, or flowchart. If yes, return the normalized bounding box
                   coordinates (0-1000 grid, format: [yMin, xMin, yMax, xMax]) and page number.
                
                CONFIDENCE SCORING RULES:
                - 0.95 – 1.00 = solved with complete certainty
                - 0.70 – 0.94 = solved with high confidence but some reasoning steps needed
                - 0.50 – 0.69 = moderate confidence (flag for human review)
                - below 0.50  = uncertain (definitely flag for human review)
                Use LOWER scores for NAT questions where an exact numeric answer is needed.
                
                Return ONLY a valid JSON object — no markdown fences, no preamble, no explanation.
                The root object must have a single key "questions" containing an array.
                
                JSON Schema (one element per question):
                {
                  "questions": [
                    {
                      "questionNumber": <integer, sequential from 1>,
                      "section": "<GA or CS>",
                      "questionType": "<MCQ | MSQ | NAT>",
                      "marks": <number>,
                      "negativeMarks": <number, 0 if no negative marking>,
                      "questionText": "<exact question text from paper, preserve LaTeX formatting>",
                      "options": [
                        {"label": "A", "text": "<option text>"},
                        {"label": "B", "text": "<option text>"},
                        {"label": "C", "text": "<option text>"},
                        {"label": "D", "text": "<option text>"}
                      ],
                      "correctAnswer": "<A | B | C | D for MCQ; comma-separated for MSQ e.g. A,C; numeric for NAT e.g. 42.5>",
                      "confidenceScore": <number 0.0-1.0>,
                      "explanation": "<step-by-step solution, use LaTeX for math expressions>",
                      "difficulty": "<EASY | MEDIUM | HARD>",
                      "bloomsLevel": "<REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE>",
                      "subject": "<e.g. Data Structures, Algorithms, Theory of Computation>",
                      "topic": "<specific topic within subject>",
                      "subtopic": "<specific subtopic if applicable, else empty string>",
                      "estimatedSolveTimeSecs": <integer seconds>,
                      "prerequisites": ["<concept1>", "<concept2>"],
                      "hintTier1": "<subtle direction-only hint>",
                      "hintTier2": "<moderate hint revealing key insight>",
                      "hintTier3": "<near-direct hint almost giving away the approach>",
                      "hasDiagram": <true | false>,
                      "diagramBoundingBox": {
                        "yMin": <number 0-1000 or null if hasDiagram=false>,
                        "xMin": <number 0-1000 or null if hasDiagram=false>,
                        "yMax": <number 0-1000 or null if hasDiagram=false>,
                        "xMax": <number 0-1000 or null if hasDiagram=false>,
                        "pageNumber": <1-indexed page number or null if hasDiagram=false>
                      }
                    }
                  ]
                }
                
                IMPORTANT:
                - For NAT questions, the options array should be empty [].
                - For MSQ questions, correctAnswer is a comma-separated list of correct option labels.
                - Always include all 4 options for MCQ and MSQ questions.
                - diagramBoundingBox should be null (not an object with null fields) if hasDiagram is false.
                - questionText must preserve any mathematical notation exactly.
                - Do NOT skip any questions. Process every question on every page.
                """;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse Gemini JSON response → List<IngestedQuestionResult>
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<IngestedQuestionResult> parseGeminiResponse(String rawJson, String paperId) {
        try {
            String cleaned = rawJson.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
            }

            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode questionsNode = root.path("questions");
            if (questionsNode.isMissingNode() || !questionsNode.isArray()) {
                log.error("[Ingestion] Gemini response has no 'questions' array for paperId={}", paperId);
                return List.of(IngestedQuestionResult.failed("No 'questions' array in Gemini response"));
            }

            List<IngestedQuestionResult> results = new ArrayList<>();
            for (JsonNode qNode : questionsNode) {
                try {
                    results.add(mapNodeToResult(qNode));
                } catch (Exception e) {
                    log.warn("[Ingestion] Failed to map question node: {}", e.getMessage());
                    results.add(IngestedQuestionResult.failed("Mapping error: " + e.getMessage()));
                }
            }
            return results;

        } catch (Exception e) {
            log.error("[Ingestion] Failed to parse Gemini response for paperId={}: {}", paperId, e.getMessage());
            return List.of(IngestedQuestionResult.failed("Parse error: " + e.getMessage()));
        }
    }

    private IngestedQuestionResult mapNodeToResult(JsonNode q) {
        IngestedQuestionResult.IngestedQuestionResultBuilder b = IngestedQuestionResult.builder()
                .success(true)
                .questionNumber(q.path("questionNumber").asInt(0))
                .section(q.path("section").asText("CS"))
                .questionType(q.path("questionType").asText("MCQ"))
                .marks(jsonDecimal(q, "marks"))
                .negativeMarks(jsonDecimal(q, "negativeMarks"))
                .questionText(q.path("questionText").asText(""))
                .correctAnswer(q.path("correctAnswer").asText(""))
                .confidenceScore(jsonDecimal(q, "confidenceScore"))
                .explanation(q.path("explanation").asText(""))
                .difficulty(q.path("difficulty").asText("MEDIUM"))
                .bloomsLevel(q.path("bloomsLevel").asText("APPLY"))
                .subject(q.path("subject").asText(""))
                .topic(q.path("topic").asText(""))
                .subtopic(q.path("subtopic").asText(""))
                .estimatedSolveTimeSecs(q.path("estimatedSolveTimeSecs").asInt(60))
                .hintTier1(q.path("hintTier1").asText(""))
                .hintTier2(q.path("hintTier2").asText(""))
                .hintTier3(q.path("hintTier3").asText(""))
                .hasDiagram(q.path("hasDiagram").asBoolean(false));

        // Parse prerequisites array
        List<String> prereqs = new ArrayList<>();
        q.path("prerequisites").forEach(p -> prereqs.add(p.asText()));
        b.prerequisites(prereqs);

        // Parse options
        List<IngestedQuestionResult.IngestedOptionResult> opts = new ArrayList<>();
        q.path("options").forEach(o -> opts.add(
                IngestedQuestionResult.IngestedOptionResult.builder()
                        .label(o.path("label").asText("A"))
                        .text(o.path("text").asText(""))
                        .build()
        ));
        b.options(opts);

        // Parse diagram bounding box
        JsonNode bbox = q.path("diagramBoundingBox");
        if (!bbox.isMissingNode() && !bbox.isNull() && q.path("hasDiagram").asBoolean(false)) {
            b.diagramBoundingBox(IngestedQuestionResult.DiagramBoundingBox.builder()
                    .yMin(bbox.path("yMin").asDouble(0))
                    .xMin(bbox.path("xMin").asDouble(0))
                    .yMax(bbox.path("yMax").asDouble(0))
                    .xMax(bbox.path("xMax").asDouble(0))
                    .pageNumber(bbox.path("pageNumber").asInt(1))
                    .build());
        }

        return b.build();
    }

    private BigDecimal jsonDecimal(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        try { return BigDecimal.valueOf(n.asDouble()); } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Persist a single question (entity + options) to DB
    // ─────────────────────────────────────────────────────────────────────────

    private void persistQuestion(Paper paper, IngestedQuestionResult result) {
        String questionId = paper.getPaperId() + "_Q" + result.getQuestionNumber();
        result.setQuestionId(questionId);

        GateQuestion question = GateQuestion.builder()
                .questionId(questionId)
                .paper(paper)
                .questionNumber(result.getQuestionNumber())
                .section(result.getSection())
                .questionType(result.getQuestionType())
                .marks(result.getMarks())
                .negativeMarks(result.getNegativeMarks() != null ? result.getNegativeMarks() : BigDecimal.ZERO)
                .questionText(result.getQuestionText())
                .correctAnswer(result.getCorrectAnswer())   // LLM-derived
                .answerSource("LLM_DERIVED")
                .confidenceScore(result.getConfidenceScore())
                .reviewStatus("PENDING_GATE")  // AnswerConfidenceGate will set PUBLISHED or NEEDS_REVIEW
                .build();
        questionRepository.save(question);

        // Persist options
        if (result.getOptions() != null) {
            int order = 1;
            for (IngestedQuestionResult.IngestedOptionResult opt : result.getOptions()) {
                if (opt.getLabel() == null || opt.getLabel().isBlank()) continue;
                String optionId = questionId + "_" + opt.getLabel();
                GateOption option = GateOption.builder()
                        .optionId(optionId)
                        .question(question)
                        .label(opt.getLabel().charAt(0))
                        .optionText(opt.getText() != null ? opt.getText() : "")
                        .displayOrder(order++)
                        .build();
                optionRepository.save(option);
            }
        }

        log.debug("[Ingestion] Saved Q{} ({}): confidence={}",
                result.getQuestionNumber(), result.getQuestionType(), result.getConfidenceScore());
    }
}
