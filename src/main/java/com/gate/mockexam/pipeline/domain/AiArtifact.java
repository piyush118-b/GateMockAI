package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Base AI Artifact — represents a single versioned AI-generated output for a question.
 * Each artifact has a sub-table payload (AiMetadataDetails, ExplanationDetails, etc.)
 *
 * Design Principle: Extraction data (GateQuestion) is immutable.
 * AI data is stored separately in this versioned artifact system.
 */
@Entity
@Table(name = "ai_artifacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "question")
@EqualsAndHashCode(exclude = "question")
public class AiArtifact {

    @Id
    @Column(name = "artifact_id", length = 100)
    private String artifactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    /**
     * Type of AI artifact: METADATA, EXPLANATION, EMBEDDING, HINT, CONCEPT
     */
    @Column(name = "artifact_type", nullable = false, length = 50)
    private String artifactType;

    /** Version counter — incremented on every regeneration */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /** Model used for generation, e.g., "gemini-3.5-flash" */
    @Column(nullable = false, length = 100)
    private String model;

    /**
     * Lifecycle status: PENDING → GENERATED → VERIFIED | ERROR
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
