package com.gate.mockexam.pipeline.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Reference to an extracted asset already uploaded to MinIO by the Python service. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedAssetDto {

    /** Semantic type: Image, Graph, Table, Circuit, Tree, Automata, Flowchart */
    private String assetType;

    /** MinIO object key, e.g. "papers/gate_cse_2020/questions/q17/tree.png" */
    private String objectKey;

    private String mimeType;
    private Integer width;
    private Integer height;

    /** SHA-256 checksum */
    private String checksum;
}
