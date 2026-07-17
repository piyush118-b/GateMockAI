package com.gate.mockexam.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/** One day of Gemini token usage history */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiUsageHistoryDTO {
    private LocalDate date;
    private int totalTokens;
    private double estimatedCostUsd;
}
