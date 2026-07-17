package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Tracks manual quality review decisions for extracted questions. */
@Entity
@Table(name = "review_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "question")
@EqualsAndHashCode(exclude = "question")
public class ReviewHistory {

    @Id
    @Column(name = "review_id", length = 100)
    private String reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    @Column(nullable = false, length = 100)
    private String reviewer;

    /** PASSED, FAILED, NEEDS_REVIEW */
    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "reviewed_at")
    @Builder.Default
    private LocalDateTime reviewedAt = LocalDateTime.now();
}
