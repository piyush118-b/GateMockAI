package com.gate.mockexam.dto;

public record PlatformSummaryDto(
    long totalStudents,
    long totalAttempts,
    double avgScore,
    double passRate,
    double avgTimeTakenMinutes
) {}
