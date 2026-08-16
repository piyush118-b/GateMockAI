package com.gate.mockexam.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTestDetailResponse {

    private String id;
    private String title;
    private String branch;
    private String yearLabel;
    private Integer durationMinutes;
    private BigDecimal totalMarks;
    private boolean isPublished;
    private List<QuestionDetail> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetail {
        private String id;
        private Integer sequenceNo;
        private String type;
        private BigDecimal marks;
        private BigDecimal negativeMarks;
        private String questionText;
        private String explanation;
        private String correctNatValue;
        private String imagePath;
        private List<OptionDetail> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDetail {
        private String id;
        private String optionLabel;
        private String optionText;
        private boolean isCorrect;
        private String imagePath;
    }
}
