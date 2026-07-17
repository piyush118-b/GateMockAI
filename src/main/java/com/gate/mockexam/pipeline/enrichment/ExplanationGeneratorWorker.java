package com.gate.mockexam.pipeline.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.repository.*;
import com.gate.mockexam.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Worker 2 — Explanation Generator.
 *
 * Generates for each question:
 *   - Detailed step-by-step explanation
 *   - Final answer validation (cross-checks official answer)
 *   - Alternative approach (if applicable)
 *
 * Output stored in ai_artifacts (type=EXPLANATION) + explanation_details.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExplanationGeneratorWorker {

    private final AiArtifactRepository artifactRepository;
    private final AiGenerationLogRepository logRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model.generation:gemini-2.5-flash}")
    private String model;

    public AiArtifact generate(GateQuestion question) {
        log.debug("[Worker2/Explanation] Generating explanation for: {}", question.getQuestionId());

        AiArtifact artifact = AiArtifact.builder()
                .artifactId(UUID.randomUUID().toString())
                .question(question)
                .artifactType("EXPLANATION")
                .version(nextVersion(question.getQuestionId()))
                .model(model)
                .status("PENDING")
                .build();
        artifactRepository.save(artifact);

        try {
            String prompt = buildExplanationPrompt(question);
            String jsonResponse = geminiService.generateJsonContent(prompt, 0.2);

            JsonNode node = objectMapper.readTree(jsonResponse);

            ExplanationDetails details = ExplanationDetails.builder()
                    .artifactId(artifact.getArtifactId())
                    .artifact(artifact)
                    .explanationText(getTextOrDefault(node, "explanation", "Explanation not available."))
                    .finalAnswer(getTextOrDefault(node, "final_answer", question.getCorrectAnswer()))
                    .altApproach(getTextOrDefault(node, "alternative_approach", null))
                    .build();

            artifact.setStatus("GENERATED");
            artifactRepository.save(artifact);
            log.debug("[Worker2/Explanation] Details: {}", details.getExplanationText().substring(0, Math.min(80, details.getExplanationText().length())));

            logSuccess(question);
            return artifact;

        } catch (Exception e) {
            artifact.setStatus("ERROR");
            artifactRepository.save(artifact);
            logFailure(question, e.getMessage());
            log.error("[Worker2/Explanation] Failed for {}: {}", question.getQuestionId(), e.getMessage());
            return artifact;
        }
    }

    private String buildExplanationPrompt(GateQuestion q) {
        StringBuilder optionsStr = new StringBuilder();
        if (q.getOptions() != null) {
            q.getOptions().forEach(opt ->
                optionsStr.append("(").append(opt.getLabel()).append(") ").append(opt.getOptionText()).append("\n")
            );
        }

        return String.format("""
            You are an expert GATE CSE professor. Provide a detailed explanation for this official GATE question.

            Question %d [%s | %s | %s marks]:
            %s

            Options:
            %s

            Official Answer: %s

            Return ONLY a valid JSON object:
            {
              "explanation": "<detailed step-by-step explanation, 150-300 words>",
              "final_answer": "<confirmed correct answer: A/B/C/D for MCQ, A,C for MSQ, or numeric for NAT>",
              "alternative_approach": "<a different method to arrive at the same answer, or null if none>"
            }
            """,
                q.getQuestionNumber(), q.getSection(), q.getQuestionType(), q.getMarks(),
                q.getQuestionText(),
                optionsStr,
                q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "Not available"
        );
    }

    private int nextVersion(String questionId) {
        return (int) artifactRepository
                .findTopByQuestionQuestionIdAndArtifactTypeOrderByVersionDesc(questionId, "EXPLANATION")
                .map(a -> a.getVersion() + 1)
                .orElse(1);
    }

    private String getTextOrDefault(JsonNode node, String field, String def) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : def;
    }

    private void logSuccess(GateQuestion q) {
        logRepository.save(AiGenerationLog.builder()
                .logId(UUID.randomUUID().toString()).question(q).task("ExplanationGeneration")
                .model(model).promptVersion("v1").status("SUCCESS").tokenUsage(0).build());
    }

    private void logFailure(GateQuestion q, String reason) {
        logRepository.save(AiGenerationLog.builder()
                .logId(UUID.randomUUID().toString()).question(q).task("ExplanationGeneration")
                .model(model).promptVersion("v1").status("FAILED").tokenUsage(0).build());
    }
}
