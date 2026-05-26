package com.gate.mockexam.dto;

public record SubjectAccuracyDto(
    String subject,
    long attempted,
    long correct,
    double accuracyPct
) {}
