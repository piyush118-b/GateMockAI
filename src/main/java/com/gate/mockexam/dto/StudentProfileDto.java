package com.gate.mockexam.dto;

import java.util.List;

public record StudentProfileDto(
    StudentDetailsDto student,
    long totalAttempts,
    double avgScore,
    double bestScore,
    double totalTimeSpentMins,
    List<SubjectAccuracyDto> bySubject,
    List<AttemptHistoryEntryDto> attemptHistory,
    List<WeakQuestionDto> weakQuestions
) {}
