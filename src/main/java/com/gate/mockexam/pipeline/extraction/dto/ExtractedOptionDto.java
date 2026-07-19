package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Data;
import java.util.List;

/** A single answer option extracted from the question paper. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExtractedOptionDto {

    /** A, B, C, D */
    private String label;

    private String optionText;

    /** Asset attached to this specific option (e.g., image option) */
    private List<ExtractedAssetDto> assets;
}
