package com.gate.mockexam.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeaderboardEntryDto(
    UUID userId,
    String fullName,
    long attemptCount,
    double bestScore,
    double avgScore,
    LocalDateTime lastAttemptDate
) {}
