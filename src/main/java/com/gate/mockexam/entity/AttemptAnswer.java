package com.gate.mockexam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attempt_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "attempt")
@EqualsAndHashCode(exclude = "attempt")
public class AttemptAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_option_ids")
    private String selectedOptionIds; // comma-separated for MSQ

    @Column(name = "nat_value_entered")
    private Double natValueEntered;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "marks_awarded")
    private BigDecimal marksAwarded;
}
