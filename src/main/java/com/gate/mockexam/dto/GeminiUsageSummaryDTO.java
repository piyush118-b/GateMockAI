package com.gate.mockexam.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiUsageSummaryDTO {
    private LocalDate date;
    private int totalTokens;
    private int inputTokens;
    private int outputTokens;
    private double estimatedCostUsd;
    private int limitTokens;
    private int remainingTokens;
    private List<CallTypeUsage> byCallType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallTypeUsage {
        private String callType;
        private int tokens;
    }
}
