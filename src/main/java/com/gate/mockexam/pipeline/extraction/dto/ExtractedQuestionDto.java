package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * A single extracted question from the Python microservice.
 * Represents ONE complete question candidate after parsing.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedQuestionDto {

    private Integer questionNumber;

    /** e.g., "GA", "CS", "Section A", "Section B" */
    private String section;

    /** MCQ, MSQ, NAT */
    private String questionType;

    private String questionText;

    private Double marks;
    private Double negativeMarks;

    private List<ExtractedOptionDto> options;

    /**
     * List of asset references — objects already uploaded to MinIO by the Python service.
     * Each entry contains the MinIO object key.
     */
    private List<ExtractedAssetDto> assets;

    /** Validation flag set by the Python extractor */
    private boolean valid;
    private String validationMessage;
}
