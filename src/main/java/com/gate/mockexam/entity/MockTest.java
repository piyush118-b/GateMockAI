package com.gate.mockexam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "questions")
@EqualsAndHashCode(exclude = "questions")
public class MockTest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String topic;

    private String subject;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "total_marks")
    private BigDecimal totalMarks;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceNo ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
