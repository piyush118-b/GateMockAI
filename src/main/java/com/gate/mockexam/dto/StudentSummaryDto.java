package com.gate.mockexam.dto;

import java.util.List;

public record StudentSummaryDto(
    StudentDetailsDto student,
    long totalAttempts,
    double avgScore,
    double bestScore,
    double totalTimeSpentMins,
    List<SubjectAccuracyDto> bySubject,
    List<AttemptHistoryEntryDto> attemptHistory,
    List<WeakQuestionDto> weakQuestions,
    double scoreTrend,
    int globalRank
) {}
