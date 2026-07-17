package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an official GATE question paper.
 * This is the top-level aggregate root for Pipeline 1 (Extraction).
 */
@Entity
@Table(name = "papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paper {

    @Id
    @Column(name = "paper_id", length = 100)
    private String paperId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;

    @Column(length = 100)
    private String branch;

    @Column(length = 50)
    private String session;

    private Integer duration;

    @Column(name = "total_marks", precision = 5, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "paper_type", length = 50)
    @Builder.Default
    private String paperType = "Official";

    /**
     * Pipeline status: Extracted → Validated → Enriched → Published
     */
    @Column(length = 50)
    @Builder.Default
    private String status = "Extracted";

    @Column(name = "uploaded_at")
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
