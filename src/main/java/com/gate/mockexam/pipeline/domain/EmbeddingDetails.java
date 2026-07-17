package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;

/** Links pgvector embedding row to its source AI artifact (EMBEDDING type). */
@Entity
@Table(name = "embedding_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingDetails {

    @Id
    @Column(name = "artifact_id", length = 100)
    private String artifactId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "artifact_id")
    private AiArtifact artifact;

    /** e.g., "gemini", "ollama" */
    @Column(nullable = false, length = 100)
    private String provider;

    /** Row ID in gate_vector_store table */
    @Column(name = "vector_db_id", nullable = false, length = 100)
    private String vectorDbId;

    @Column(name = "embedding_version", length = 50)
    private String embeddingVersion;
}
