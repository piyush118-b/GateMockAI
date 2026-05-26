package com.gate.mockexam.dto;

import java.util.UUID;

public record WeakQuestionDto(
    UUID questionId,
    int sequenceNo,
    String questionText,
    long wrongCount,
    long totalSeen,
    String explanation
) {}
