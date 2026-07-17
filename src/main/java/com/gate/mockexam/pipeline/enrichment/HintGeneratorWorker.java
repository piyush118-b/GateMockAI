package com.gate.mockexam.pipeline.enrichment;

import com.gate.mockexam.pipeline.domain.*;
import com.gate.mockexam.pipeline.repository.*;
import com.gate.mockexam.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Worker 4 — Hint Generator.
 * Generates tiered hints (nudge → directional → near-answer).
 * Stored in ai_artifacts (type=HINT) + hint_details.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HintGeneratorWorker {

    private final AiArtifactRepository artifactRepository;
    private final AiGenerationLogRepository logRepository;
    private final GeminiService geminiService;

    @Value("${gemini.model.generation:gemini-2.5-flash}")
    private String model;

    public AiArtifact generate(GateQuestion question, int complexityLevel) {
        log.debug("[Worker4/Hint] Generating hint level {} for: {}", complexityLevel, question.getQuestionId());

        AiArtifact artifact = AiArtifact.builder()
                .artifactId(UUID.randomUUID().toString())
                .question(question)
                .artifactType("HINT")
                .version(complexityLevel)
                .model(model)
                .status("PENDING")
                .build();
        artifactRepository.save(artifact);

        try {
            String hintInstruction = switch (complexityLevel) {
                case 1 -> "Give a very subtle nudge — just point to the right area of thinking without revealing the approach.";
                case 2 -> "Give a directional hint — suggest the approach or technique needed, without giving the answer.";
                case 3 -> "Give a near-answer hint — show the key step that leads directly to the answer.";
                default -> "Give a general hint about how to approach this question.";
            };

            String prompt = String.format("""
                You are helping a GATE aspirant solve a problem.
                
                Question: %s
                Official Answer: %s
                
                Hint Level %d: %s
                
                Return ONLY a JSON object:
                {
                  "hint": "<the hint text>",
                  "complexity_level": %d
                }
                """,
                    question.getQuestionText(),
                    question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "N/A",
                    complexityLevel, hintInstruction, complexityLevel);

            String response = geminiService.generateJsonContent(prompt, 0.3);
            log.debug("[Worker4/Hint] Level {} hint generated for {}", complexityLevel, question.getQuestionId());

            artifact.setStatus("GENERATED");
            artifactRepository.save(artifact);

            logRepository.save(AiGenerationLog.builder()
                    .logId(UUID.randomUUID().toString()).question(question)
                    .task("HintGeneration_L" + complexityLevel)
                    .model(model).promptVersion("v1").status("SUCCESS").tokenUsage(0).build());

            return artifact;

        } catch (Exception e) {
            artifact.setStatus("ERROR");
            artifactRepository.save(artifact);
            log.error("[Worker4/Hint] Failed for {}: {}", question.getQuestionId(), e.getMessage());
            return artifact;
        }
    }
}
