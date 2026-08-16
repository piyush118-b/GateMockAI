package com.gate.mockexam.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchCatalogResponse {

    private String id;
    private String name;
    private String code;
    private List<SubjectDto> subjects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDto {
        private String id;
        private String name;
        private int defaultMarksWeightage;
    }
}
