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
 * Worker 3 — Concept Extractor.
 * Extracts concepts, keywords, and prerequisites.
 * Stored in ai_artifacts (type=CONCEPT).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConceptExtractorWorker {

    private final AiArtifactRepository artifactRepository;
    private final AiGenerationLogRepository logRepository;
    private final GeminiService geminiService;

    @Value("${gemini.model.generation:gemini-2.5-flash}")
    private String model;

    public AiArtifact generate(GateQuestion question) {
        log.debug("[Worker3/Concept] Extracting concepts for: {}", question.getQuestionId());

        AiArtifact artifact = AiArtifact.builder()
                .artifactId(UUID.randomUUID().toString())
                .question(question)
                .artifactType("CONCEPT")
                .version(1)
                .model(model)
                .status("PENDING")
                .build();
        artifactRepository.save(artifact);

        try {
            String prompt = String.format("""
                Analyze this GATE CSE question and extract key concepts.
                
                Question: %s
                
                Return ONLY a JSON object:
                {
                  "concepts": ["<concept1>", "<concept2>", ...],
                  "keywords": ["<keyword1>", "<keyword2>", ...],
                  "prerequisites": ["<topic that must be known to answer this>", ...]
                }
                """, question.getQuestionText());

            String response = geminiService.generateJsonContent(prompt, 0.1);
            log.debug("[Worker3/Concept] Response snippet: {}", response.substring(0, Math.min(100, response.length())));

            artifact.setStatus("GENERATED");
            artifactRepository.save(artifact);

            logRepository.save(AiGenerationLog.builder()
                    .logId(UUID.randomUUID().toString()).question(question).task("ConceptExtraction")
                    .model(model).promptVersion("v1").status("SUCCESS").tokenUsage(0).build());

            return artifact;

        } catch (Exception e) {
            artifact.setStatus("ERROR");
            artifactRepository.save(artifact);
            log.error("[Worker3/Concept] Failed for {}: {}", question.getQuestionId(), e.getMessage());
            return artifact;
        }
    }
}
