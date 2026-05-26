package com.gate.mockexam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScoreTimelineEntryDto(
    UUID attemptId,
    String testTitle,
    LocalDateTime submittedAt,
    double scorePct
) {}
