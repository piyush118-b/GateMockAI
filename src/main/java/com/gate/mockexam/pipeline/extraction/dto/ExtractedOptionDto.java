package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/** A single answer option extracted from the question paper. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedOptionDto {

    /** A, B, C, D */
    private String label;

    private String optionText;

    /** Asset attached to this specific option (e.g., image option) */
    private List<ExtractedAssetDto> assets;
}
