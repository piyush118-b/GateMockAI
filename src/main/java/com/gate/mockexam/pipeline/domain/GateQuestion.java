package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single GATE question — the immutable source of truth extracted from
 * the official question paper PDF. No AI-generated fields exist on this entity.
 */
@Entity
@Table(name = "questions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"paper_id", "question_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"paper", "options", "assets"})
@EqualsAndHashCode(exclude = {"paper", "options", "assets"})
public class GateQuestion {

    @Id
    @Column(name = "question_id", length = 100)
    private String questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(length = 100)
    private String section;

    /** MCQ, MSQ, NAT */
    @Column(name = "question_type", length = 50)
    private String questionType;

    @Column(precision = 4, scale = 2)
    private BigDecimal marks;

    @Column(name = "negative_marks", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal negativeMarks = BigDecimal.ZERO;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /** Official correct answer — for MCQ: "A", MSQ: "A,C", NAT: "42.0" */
    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<GateOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Asset> assets = new ArrayList<>();
}
