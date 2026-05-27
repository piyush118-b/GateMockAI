package com.gate.mockexam.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiUsageHistoryDTO {
    private LocalDate date;
    private int totalTokens;
    private double estimatedCostUsd;
}
