package com.gate.mockexam.dto;

import lombok.Data;

@Data
public class AiGeneratedOptionDto {
    private String label;
    private String text;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    private boolean isCorrect;
}
