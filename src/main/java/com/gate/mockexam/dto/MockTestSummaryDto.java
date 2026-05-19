package com.gate.mockexam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestSummaryDto {
    private UUID id;
    private String title;
    private String topic;
    private String subject;
    private int durationMinutes;
    private int totalQuestions;
    private BigDecimal totalMarks;
    private boolean isPublished;
}
