package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Audit log for every LLM API call made during AI enrichment. */
@Entity
@Table(name = "ai_generation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "question")
@EqualsAndHashCode(exclude = "question")
public class AiGenerationLog {

    @Id
    @Column(name = "log_id", length = 100)
    private String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    /** e.g., "MetadataGeneration", "ExplanationGeneration", "ConceptExtraction" */
    @Column(nullable = false, length = 100)
    private String task;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    /** SUCCESS, FAILED, SKIPPED */
    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "token_usage", nullable = false)
    @Builder.Default
    private Integer tokenUsage = 0;

    @Column(name = "generated_at")
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
