package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;
import java.util.List;

/**
 * Top-level response from the Python extraction microservice for a question paper.
 * Represents the complete structured extraction result from Pipeline 1 Sub-Pipeline A.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExtractedPaperDto {

    /** e.g., "GATE CSE 2020" */
    private String examName;
    private Integer year;
    private String branch;
    private String session;
    private Integer duration;
    private Double totalMarks;
    private Integer totalQuestions;

    /** "digital" or "scanned" */
    private String pdfType;

    private List<ExtractedQuestionDto> questions;
}
