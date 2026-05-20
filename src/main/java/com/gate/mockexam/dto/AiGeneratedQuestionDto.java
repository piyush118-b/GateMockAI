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
    private String subject;         // Auto-classified by local LLM, e.g. "Operating Systems"
    private String topic;           // Auto-classified by local LLM, e.g. "CPU Scheduling"
    private List<AiGeneratedOptionDto> options;
}
