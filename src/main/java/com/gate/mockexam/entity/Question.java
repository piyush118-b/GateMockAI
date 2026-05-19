package com.gate.mockexam.entity;

import com.gate.mockexam.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"test", "options"})
@EqualsAndHashCode(exclude = {"test", "options"})
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private MockTest test;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(name = "correct_nat_value")
    private Double correctNatValue;

    @Column(name = "nat_tolerance")
    private Double natTolerance;

    @Column(nullable = false)
    private BigDecimal marks;

    @Column(name = "negative_marks", nullable = false)
    private BigDecimal negativeMarks;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("optionLabel ASC")
    @Builder.Default
    private List<Option> options = new ArrayList<>();
}
