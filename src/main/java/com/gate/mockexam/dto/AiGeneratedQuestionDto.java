package com.gate.mockexam.dto;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGeneratedQuestionDto {
    @JsonProperty("sequenceNo")
    private int sequenceNo;

    @JsonProperty("section")
    private String section;         // "GA", "CS"

    @JsonProperty("type")
    private String type;            // "MCQ", "MSQ", "NAT"

    @JsonProperty("questionText")
    private String questionText;

    @JsonProperty("marks")
    private double marks;

    @JsonProperty("negativeMarks")
    private double negativeMarks;

    @JsonProperty("correctNatValue")
    @JsonDeserialize(using = CorrectNatValueDeserializer.class)
    private Double correctNatValue;

    @JsonProperty("natTolerance")
    private Double natTolerance;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("subject")
    private String subject;         // Auto-classified by local LLM, e.g. "Operating Systems"

    @JsonProperty("topic")
    private String topic;           // Auto-classified by local LLM, e.g. "CPU Scheduling"

    @JsonProperty("options")
    private List<AiGeneratedOptionDto> options;

    @JsonProperty("hasImage")
    private Boolean hasImage;

    public static class CorrectNatValueDeserializer extends JsonDeserializer<Double> {
        private double roundTo4Decimals(double val) {
            return Math.round(val * 10000.0) / 10000.0;
        }

        @Override
        public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.START_ARRAY) {
                JsonNode node = p.getCodec().readTree(p);
                if (node.isArray() && node.size() > 0) {
                    double low = node.get(0).asDouble();
                    double high = node.size() > 1 ? node.get(1).asDouble() : low;
                    
                    Object parent = p.getCurrentValue();
                    if (parent instanceof AiGeneratedQuestionDto) {
                        AiGeneratedQuestionDto dto = (AiGeneratedQuestionDto) parent;
                        dto.setNatTolerance(roundTo4Decimals((high - low) / 2.0));
                    }
                    
                    return roundTo4Decimals((low + high) / 2.0);
                }
                return null;
            } else if (token == JsonToken.VALUE_STRING) {
                String val = p.getValueAsString().trim();
                if (val.startsWith("[") && val.endsWith("]")) {
                    val = val.substring(1, val.length() - 1).trim();
                    String[] parts = val.split(",");
                    if (parts.length > 0) {
                        try {
                            double low = Double.parseDouble(parts[0].trim());
                            double high = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : low;
                            
                            Object parent = p.getCurrentValue();
                            if (parent instanceof AiGeneratedQuestionDto) {
                                AiGeneratedQuestionDto dto = (AiGeneratedQuestionDto) parent;
                                dto.setNatTolerance(roundTo4Decimals((high - low) / 2.0));
                            }
                            
                            return roundTo4Decimals((low + high) / 2.0);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                try {
                    return Double.parseDouble(val);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
                return p.getDoubleValue();
            }
            
            // Fallback general parsing
            try {
                JsonNode node = p.getCodec().readTree(p);
                if (node.isNumber()) {
                    return node.asDouble();
                } else if (node.isArray() && node.size() > 0) {
                    double low = node.get(0).asDouble();
                    double high = node.size() > 1 ? node.get(1).asDouble() : low;
                    
                    Object parent = p.getCurrentValue();
                    if (parent instanceof AiGeneratedQuestionDto) {
                        AiGeneratedQuestionDto dto = (AiGeneratedQuestionDto) parent;
                        dto.setNatTolerance(roundTo4Decimals((high - low) / 2.0));
                    }
                    
                    return roundTo4Decimals((low + high) / 2.0);
                } else if (node.isTextual()) {
                    return Double.parseDouble(node.asText().trim());
                }
            } catch (Exception e) {
                // ignore
            }
            return null;
        }
    }
}
