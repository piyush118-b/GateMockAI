package com.gate.mockexam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "branch_subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "branch")
@EqualsAndHashCode(exclude = "branch")
public class BranchSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_marks_weightage", nullable = false)
    private int defaultMarksWeightage;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
