package com.gate.mockexam.dto;

public record RankResponseDto(
    int rank,
    int totalStudents,
    double percentile,
    String testTitle
) {}
