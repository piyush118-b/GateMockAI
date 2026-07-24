package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A question flagged by the AnswerConfidenceGate because the Gemini
 * confidence score was below the configured threshold.
 *
 * One record per question (UNIQUE constraint on question_id).
 * Resolved when an admin approves or corrects the answer.
 */
@Entity
@Table(name = "admin_review_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "question")
@EqualsAndHashCode(exclude = "question")
public class AdminReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    @Column(name = "flagged_reason", columnDefinition = "TEXT")
    private String flaggedReason;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "flagged_at", nullable = false)
    @Builder.Default
    private LocalDateTime flaggedAt = LocalDateTime.now();

    @Column(name = "reviewed_by", length = 150)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;
}
