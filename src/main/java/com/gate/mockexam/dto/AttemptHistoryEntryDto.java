package com.gate.mockexam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttemptHistoryEntryDto(
    UUID attemptId,
    String testTitle,
    double score,
    double totalMarks,
    double timeMins,
    LocalDateTime submittedAt,
    String status
) {}
