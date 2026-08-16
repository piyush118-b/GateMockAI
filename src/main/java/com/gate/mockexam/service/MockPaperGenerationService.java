package com.gate.mockexam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the full mock paper generation pipeline:
 * 1. For each subject bucket, query the question bank for reference context
 * 2. Fill shortfalls via GeminiServiceImpl.generateMockQuestions()
 * 3. Persist the assembled Paper with GateQuestion entities
 * 4. Stream progress via SseEmitter
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPaperGenerationService {

    private final GateQuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final GateOptionRepository optionRepository;
    private final AiArtifactRepository artifactRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    private static final int FEWSHOT_MIN = 3;

    @Async("pipelineExecutor")
    public CompletableFuture<Void> generateMockPaperAsync(
            String branchCode,
            String yearLabel,
            Map<String, Integer> weightages,
            SseEmitter emitter) {

        try {
            send(emitter, "progress", "[System] Starting AI mock paper generation...");

            int total = weightages.values().stream().mapToInt(Integer::intValue).sum();
            if (total != 100) {
                send(emitter, "error", "Weightages must sum to 100 (got " + total + ").");
                emitter.complete();
                return CompletableFuture.completedFuture(null);
            }

            List<SubjectBucket> buckets = buildBuckets(weightages);
            send(emitter, "progress", "[System] Distributing across " + buckets.size() + " subjects.");

            List<GeneratedQuestion> allQuestions = new ArrayList<>();
            int questionNumber = 1;

            for (SubjectBucket bucket : buckets) {
                send(emitter, "progress",
                        "[Gemini] Preparing " + bucket.displayName() + " (" + bucket.marks() + " marks)...");

                List<GateQuestion> references = fetchReferences(bucket.canonicalName());
                String contextSection = buildContextSection(references, bucket.canonicalName());

                int needed = bucket.marks();
                send(emitter, "progress",
                        "[Gemini] Generating " + needed + " questions for " + bucket.displayName() +
                                (references.size() >= FEWSHOT_MIN ? " (few-shot)" : " (cold-start)"));

                String weightagePrompt = String.format("%s: %d marks, %d questions",
                        bucket.displayName(), bucket.marks(), needed);

                String rawJson = geminiService.generateMockQuestions(
                        bucket.canonicalName(),
                        contextSection,
                        weightagePrompt,
                        needed);

                List<GeneratedQuestion> generated =
                        parseGeneratedQuestions(rawJson, bucket.canonicalName(), questionNumber);
                questionNumber += generated.size();
                allQuestions.addAll(generated);

                send(emitter, "progress",
                        "[Gemini] Generated " + generated.size() + " questions for " + bucket.displayName());

                Thread.sleep(300); // rate-limit protection
            }

            send(emitter, "progress", "[System] Persisting mock paper to database...");
            Paper paper = persistMockPaper(branchCode, yearLabel, allQuestions);

            send(emitter, "progress", "[System] Paper ready with " + paper.getTotalQuestions() + " questions.");
            send(emitter, "complete", "/admin/tests/" + paper.getPaperId());

        } catch (Exception e) {
            log.error("Mock paper generation failed", e);
            send(emitter, "error", "Generation failed: " + e.getMessage());
        } finally {
            emitter.complete();
        }

        return CompletableFuture.completedFuture(null);
    }

    // -------------------------------------------------------------------------
    // Bucket building
    // -------------------------------------------------------------------------
    private List<SubjectBucket> buildBuckets(Map<String, Integer> weightages) {
        return weightages.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new SubjectBucket(
                        normalizeSubjectName(e.getKey()),
                        e.getKey(),
                        e.getValue()))
                .collect(Collectors.toList());
    }

    private String normalizeSubjectName(String displayName) {
        return switch (displayName) {
            case "Data Structures and Algorithms" -> "Data Structures and Algorithms";
            case "Engineering Mathematics" -> "Engineering Mathematics";
            case "General Aptitude" -> "General Aptitude";
            case "Computer Organization and Architecture" -> "Computer Organization";
            case "Database Management Systems" -> "Databases";
            case "Theory of Computation" -> "Theory of Computation";
            case "Computer Networks" -> "Computer Networks";
            case "Operating Systems" -> "Operating Systems";
            case "Software Engineering" -> "Software Engineering";
            case "Compilers" -> "Compilers";
            case "Artificial Intelligence" -> "Artificial Intelligence";
            case "Digital Logic", "Digital Logic and Computer Organization" -> "Digital Logic";
            case "Information Systems" -> "Information Systems";
            case "Web Technologies" -> "Web Technologies";
            default -> displayName;
        };
    }

    // -------------------------------------------------------------------------
    // Reference fetching
    // -------------------------------------------------------------------------
    private List<GateQuestion> fetchReferences(String subject) {
        try {
            return questionRepository.findPublishedBySubject(subject, PageRequest.of(0, 10));
        } catch (Exception e) {
            log.warn("Failed to fetch references for {}: {}", subject, e.getMessage());
            return List.of();
        }
    }

    private String buildContextSection(List<GateQuestion> references, String subject) {
        if (references.isEmpty()) {
            return "Generate fresh GATE-quality " + subject + " questions based on the standard GATE CSE syllabus. Focus on conceptual clarity and typical GATE difficulty.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Reference real GATE questions for ").append(subject).append(":\n\n");
        int count = 0;
        for (GateQuestion q : references) {
            if (count++ >= 5) break;
            sb.append("- [").append(q.getQuestionType()).append("] ");
            sb.append(q.getQuestionText());
            if (q.getCorrectAnswer() != null) {
                sb.append(" | Answer: ").append(q.getCorrectAnswer());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // JSON parsing
    // -------------------------------------------------------------------------
    private List<GeneratedQuestion> parseGeneratedQuestions(
            String rawJson, String subject, int startSequence) throws Exception {

        List<GeneratedQuestion> results = new ArrayList<>();
        JsonNode root = objectMapper.readTree(rawJson);
        if (!root.isArray()) {
            throw new RuntimeException("Gemini returned non-array JSON");
        }

        int seq = startSequence;
        for (JsonNode node : root) {
            String type = node.path("type").asText("MCQ");
            BigDecimal marks = BigDecimal.valueOf(node.path("marks").asDouble(1));
            double negVal = "MCQ".equals(type) || "MSQ".equals(type) ? 0.33 : 0;
            BigDecimal negMarks = BigDecimal.valueOf(node.path("negativeMarks").asDouble(negVal));

            GeneratedQuestion gq = new GeneratedQuestion();
            gq.questionNumber = seq++;
            gq.section = node.path("section").asText("CS");
            gq.questionType = type;
            gq.marks = marks;
            gq.negativeMarks = negMarks;
            gq.questionText = node.path("questionText").asText();
            gq.explanation = node.path("explanation").asText();
            gq.marksWeightage = 0;
            gq.subject = subject;

            // Options
            List<GeneratedOption> opts = new ArrayList<>();
            JsonNode optsNode = node.path("options");
            if (!optsNode.isMissingNode() && optsNode.isArray()) {
                for (JsonNode o : optsNode) {
                    GeneratedOption opt = new GeneratedOption();
                    opt.label = o.path("label").asText("A");
                    opt.text = o.path("text").asText();
                    opts.add(opt);
                }
            }
            gq.options = opts;

            // Correct answer
            if ("NAT".equals(type)) {
                gq.correctAnswer = node.path("correctNatValue").asText();
            } else {
                List<String> correct = new ArrayList<>();
                JsonNode coNode = node.path("correctOptions");
                if (!coNode.isMissingNode() && coNode.isArray()) {
                    for (JsonNode c : coNode) correct.add(c.asText());
                }
                gq.correctAnswer = String.join(",", correct);
            }

            results.add(gq);
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------
    @Transactional
    public Paper persistMockPaper(String branchCode, String yearLabel, List<GeneratedQuestion> questions) {
        String paperId = "mock_" + UUID.randomUUID().toString();
        int totalQuestions = questions.size();
        int totalMarks = questions.stream()
                .mapToInt(q -> q.marks != null ? q.marks.intValue() : 0)
                .sum();

        Paper paper = Paper.builder()
                .paperId(paperId)
                .examName(yearLabel)
                .year(extractYear(yearLabel))
                .branch(branchCode)
                .paperType("Mock")
                .status("Enriched")
                .totalQuestions(totalQuestions)
                .totalMarks(BigDecimal.valueOf(totalMarks))
                .duration(180)
                .uploadedAt(LocalDateTime.now())
                .build();
        paperRepository.save(paper);

        int qNum = 1;
        for (GeneratedQuestion gq : questions) {
            String questionId = paperId + "_Q" + qNum;

            GateQuestion question = GateQuestion.builder()
                    .questionId(questionId)
                    .paper(paper)
                    .questionNumber(qNum++)
                    .section(gq.section)
                    .questionType(gq.questionType)
                    .marks(gq.marks)
                    .negativeMarks(gq.negativeMarks != null ? gq.negativeMarks : BigDecimal.ZERO)
                    .questionText(gq.questionText)
                    .correctAnswer(gq.correctAnswer)
                    .answerSource("AI_GENERATED")
                    .confidenceScore(BigDecimal.valueOf(0.85))
                    .reviewStatus("PUBLISHED")
                    .build();
            questionRepository.save(question);

            for (GeneratedOption opt : gq.options) {
                GateOption option = GateOption.builder()
                        .optionId(questionId + "_" + opt.label)
                        .question(question)
                        .label(opt.label.charAt(0))
                        .optionText(opt.text)
                        .displayOrder(opt.label.charAt(0) - 'A' + 1)
                        .build();
                optionRepository.save(option);
            }

            // Persist METADATA artifact
            String artifactId = "meta_" + questionId;
            AiArtifact artifact = AiArtifact.builder()
                    .artifactId(artifactId)
                    .question(question)
                    .artifactType("METADATA")
                    .version(1)
                    .model("mock-generation")
                    .status("GENERATED")
                    .createdAt(LocalDateTime.now())
                    .build();
            artifactRepository.save(artifact);
        }

        log.info("Persisted mock paper {} with {} questions", paperId, totalQuestions);
        return paper;
    }

    private int extractYear(String yearLabel) {
        for (int i = 0; i < yearLabel.length() - 3; i++) {
            String chunk = yearLabel.substring(i, i + 4);
            if (chunk.matches("\\d{4}")) return Integer.parseInt(chunk);
        }
        return java.time.Year.now().getValue();
    }

    // -------------------------------------------------------------------------
    // SSE
    // -------------------------------------------------------------------------
    private void send(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.debug("SSE send failed (client disconnected): {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal DTOs — plain static classes with public fields
    // -------------------------------------------------------------------------
    private record SubjectBucket(String canonicalName, String displayName, int marks) {}

    private static class GeneratedQuestion {
        int questionNumber;
        String section;
        String questionType;
        BigDecimal marks;
        BigDecimal negativeMarks;
        String questionText;
        String correctAnswer;
        String explanation;
        int marksWeightage;
        String subject;
        List<GeneratedOption> options = new ArrayList<>();
    }

    private static class GeneratedOption {
        String label;
        String text;
    }
}
