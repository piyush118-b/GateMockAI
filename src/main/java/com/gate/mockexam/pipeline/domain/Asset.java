package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents an extracted asset (image, graph, table, circuit diagram, etc.)
 * linked to a question or option.
 *
 * IMPORTANT: This entity stores only MinIO object references — never binary data.
 * Binary data is stored in MinIO; only the bucket name and object key are persisted here.
 */
@Entity
@Table(name = "assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"question", "option"})
@EqualsAndHashCode(exclude = {"question", "option"})
public class Asset {

    @Id
    @Column(name = "asset_id", length = 100)
    private String assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    /** Null if asset belongs to the question body, non-null if it belongs to a specific option */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private GateOption option;

    /**
     * Semantic type of the asset: Image, Graph, Table, Circuit, Tree, Automata, Flowchart
     */
    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    /** MinIO bucket name */
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    /** MinIO object key, e.g., "papers/gate_cse_2020/questions/q17/tree.png" */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    private Integer width;
    private Integer height;

    /** SHA-256 checksum of the file for deduplication */
    @Column(length = 64)
    private String checksum;

    @Column(name = "uploaded_at")
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
