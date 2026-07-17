package com.gate.mockexam.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * An answer option for a GATE question (MCQ or MSQ).
 * For NAT questions, no options are stored.
 */
@Entity
@Table(name = "options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "question")
@EqualsAndHashCode(exclude = "question")
public class GateOption {

    @Id
    @Column(name = "option_id", length = 100)
    private String optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private GateQuestion question;

    /** A, B, C, D */
    @Column(nullable = false, length = 1)
    private Character label;

    @Column(name = "option_text", columnDefinition = "TEXT", nullable = false)
    private String optionText;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
