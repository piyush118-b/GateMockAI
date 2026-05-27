package com.gate.mockexam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptionDto {
    @JsonProperty("label")
    private String label;

    @JsonProperty("text")
    private String text;

    @JsonProperty("isCorrect")
    private boolean isCorrect;
}
