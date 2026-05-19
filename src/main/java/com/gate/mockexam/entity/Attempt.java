package com.gate.mockexam.entity;

import com.gate.mockexam.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "answers")
@EqualsAndHashCode(exclude = "answers")
public class Attempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private MockTest test;

    private BigDecimal score;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AttemptAnswer> answers = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.startedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AttemptStatus.IN_PROGRESS;
        }
    }
}
