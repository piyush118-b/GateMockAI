package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;
import java.util.Map;

/**
 * Response from the Python answer key extraction service (Sub-Pipeline B).
 * Contains the normalized answer map: questionNumber → correct answer string.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExtractedAnswerKeyDto {

    /**
     * Map of question number (as String key) to correct answer.
     * MCQ: "A", "B", "C", or "D"
     * MSQ: "A,C" (comma-separated)
     * NAT: "42.0" or "15.2 to 15.4" (range)
     */
    private Map<String, String> answers;

    /** "digital" or "scanned" */
    private String pdfType;

    private Integer totalParsed;
    private String parserWarnings;
}
