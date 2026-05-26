package com.gate.mockexam.dto;

public record SubjectWeaknessDto(
    String subject,
    long totalAnswered,
    long totalCorrect,
    double accuracyPct
) {}
