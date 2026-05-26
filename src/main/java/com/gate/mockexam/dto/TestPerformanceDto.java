package com.gate.mockexam.dto;

import java.util.UUID;

public record TestPerformanceDto(
    UUID testId,
    String title,
    long attemptCount,
    double avgScore,
    double avgTimeMins,
    double passRate,
    double highestScore,
    double lowestScore
) {}
