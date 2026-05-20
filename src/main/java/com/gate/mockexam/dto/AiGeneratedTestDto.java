package com.gate.mockexam.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGeneratedTestDto {
    private String title;
    private String topic;
    private String subject;
    private int durationMinutes;
    private List<AiGeneratedQuestionDto> questions;
}
