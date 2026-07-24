package com.gate.mockexam.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gemini_token_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiTokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "usage_date", nullable = false)
    @Builder.Default
    private LocalDate usageDate = LocalDate.now();

    @Column(name = "call_type", nullable = false, length = 30)
    private String callType;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "model_used", nullable = false, length = 50)
    @Builder.Default
    private String modelUsed = "gemini-3.5-flash";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
