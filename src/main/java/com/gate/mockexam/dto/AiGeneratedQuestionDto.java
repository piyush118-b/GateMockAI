package com.gate.mockexam.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiGeneratedQuestionDto {
    private int sequenceNo;
    private String type;            // "MCQ", "MSQ", "NAT"
    private String questionText;
    private double marks;
    private double negativeMarks;
    private Double correctNatValue;
    private Double natTolerance;
    private String explanation;
    private List<AiGeneratedOptionDto> options;
}
