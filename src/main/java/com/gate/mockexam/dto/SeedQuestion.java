package com.gate.mockexam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeedQuestion {
    private String id;
    private String topic;
    private String subject;
    private String type;
    private String questionText;
    private List<OptionDto> options;
    private Double correctNatValue;
    private Double natTolerance;
    private String explanation;
    private List<String> tags;
}
