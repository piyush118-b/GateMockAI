package com.gate.mockexam.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockTestGenerationRequestDto {
    @NotBlank(message = "Topic is required")
    private String topic;

    @NotBlank(message = "Subject is required")
    private String subject;

    @Min(value = 1, message = "Must generate at least 1 MCQ question")
    @Max(value = 20, message = "Cannot generate more than 20 MCQ questions")
    private int mcqCount = 5;

    @Min(value = 0, message = "MSQ count cannot be negative")
    @Max(value = 10, message = "Cannot generate more than 10 MSQ questions")
    private int msqCount = 2;

    @Min(value = 0, message = "NAT count cannot be negative")
    @Max(value = 10, message = "Cannot generate more than 10 NAT questions")
    private int natCount = 3;
}
