package com.gate.mockexam.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGeneratedOptionDto {
    @JsonProperty("label")
    private String label;

    @JsonProperty("text")
    private String text;
    
    @JsonProperty("isCorrect")
    private boolean isCorrect;
}
