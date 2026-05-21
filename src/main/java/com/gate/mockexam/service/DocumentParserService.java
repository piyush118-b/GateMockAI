package com.gate.mockexam.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DocumentParserService {

    /**
     * Extract text from a PDF file using Apache PDFBox 3.0.x
     */
    public String parsePdf(byte[] pdfBytes) throws IOException {
        log.info("Parsing PDF document (size: {} bytes) via Apache PDFBox", pdfBytes.length);
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Successfully extracted {} characters from PDF", text.length());
            return text;
        }
    }

    /**
     * Extract text page-by-page from a PDF using PDFBox
     */
    public List<String> parsePdfPages(byte[] pdfBytes) throws IOException {
        log.info("Parsing PDF page-by-page (size: {} bytes) via Apache PDFBox", pdfBytes.length);
        List<String> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(stripper.getText(document));
            }
            log.info("Successfully extracted {} pages from PDF", pages.size());
        }
        return pages;
    }

    /**
     * Extract text from a plain TXT file
     */
    public String parseTxt(byte[] txtBytes) {
        log.info("Parsing text document (size: {} bytes)", txtBytes.length);
        return new String(txtBytes, StandardCharsets.UTF_8);
    }

    /**
     * Parses a string containing answer keys into a structured Map.
     * Supported formats on each line:
     * - "1: A" (MCQ)
     * - "2: A, C" (MSQ)
     * - "3: 42" or "3: 15.2" (NAT)
     */
    public Map<Integer, List<String>> parseAnswerKey(String answerKeyText) {
        log.info("Parsing custom answer key text block");
        Map<Integer, List<String>> keyMap = new HashMap<>();
        if (answerKeyText == null || answerKeyText.isBlank()) {
            return keyMap;
        }

        String[] lines = answerKeyText.split("\\r?\\n");
        Pattern linePattern = Pattern.compile("^\\s*(\\d+)\\s*:\\s*(.+)$");

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            Matcher matcher = linePattern.matcher(line);
            if (matcher.find()) {
                try {
                    int questionNo = Integer.parseInt(matcher.group(1));
                    String valPart = matcher.group(2).trim();
                    
                    // Split options/values by comma
                    String[] tokens = valPart.split(",");
                    List<String> values = new ArrayList<>();
                    for (String t : tokens) {
                        values.add(t.trim());
                    }
                    
                    keyMap.put(questionNo, values);
                    log.debug("Mapped Question {}: {}", questionNo, values);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse question number on line: {}", line);
                }
            } else {
                log.warn("Line did not match answer key format 'number: value': {}", line);
            }
        }

        log.info("Successfully parsed {} answer key mappings", keyMap.size());
        return keyMap;
    }

    @lombok.Data
    public static class AnswerKeyEntry {
        private int questionNo;
        private String type;         // "MCQ", "MSQ", "NAT"
        private String section;      // "GA", "CS"
        private String correctKey;   // e.g. "D", "0.125 to 0.125"
        private double marks;
    }

    /**
     * Parses the answer key PDF/TXT text into a map of SECTION_QNO -> AnswerKeyEntry
     */
    public Map<String, AnswerKeyEntry> parseAnswerKeyToMap(String answerKeyText) {
        log.info("Parsing advanced answer key text block");
        Map<String, AnswerKeyEntry> keyMap = new HashMap<>();
        if (answerKeyText == null || answerKeyText.isBlank()) {
            return keyMap;
        }

        String[] lines = answerKeyText.split("\\r?\\n");
        
        // 1. Table row pattern: e.g. "1 6 MCQ GA D 1"
        Pattern tableRowPattern = Pattern.compile("^\\s*(\\d+)\\s+(\\S+)\\s+(MCQ|NAT|MSQ)\\s+(GA|CS)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*$");
        
        // 2. Prefixed manual pattern: e.g. "GA 1: D" or "CS 17: 0.125 to 0.125"
        Pattern prefixedPattern = Pattern.compile("^\\s*(GA|CS)\\s*[-_]?\\s*(?:Q)?\\s*(\\d+)\\s*[:=-]\\s*(.+)$");
        
        // 3. Simple list pattern: e.g. "1: D"
        Pattern simplePattern = Pattern.compile("^\\s*(\\d+)\\s*[:=-]\\s*(.+)$");

        String currentSection = "GA"; // Default to GA (General Aptitude starts first)

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Look for section headers to switch section context for subsequent simple patterns
            String upperTrimmed = trimmed.toUpperCase();
            if (upperTrimmed.contains("GENERAL APTITUDE") || upperTrimmed.contains("SECTION GA") || (upperTrimmed.startsWith("GA") && !upperTrimmed.contains(":"))) {
                currentSection = "GA";
                log.info("Detected GA section header in answer key text");
                continue;
            } else if (upperTrimmed.contains("COMPUTER SCIENCE") || upperTrimmed.contains("SECTION CS") || (upperTrimmed.startsWith("CS") && !upperTrimmed.contains(":"))) {
                currentSection = "CS";
                log.info("Detected CS section header in answer key text");
                continue;
            }

            // Try Table Row Pattern
            Matcher matcher = tableRowPattern.matcher(trimmed);
            if (matcher.find()) {
                try {
                    int qNo = Integer.parseInt(matcher.group(1));
                    String type = matcher.group(3).toUpperCase();
                    String sec = matcher.group(4).toUpperCase();
                    String key = matcher.group(5).trim();
                    double marks = Double.parseDouble(matcher.group(6));

                    AnswerKeyEntry entry = new AnswerKeyEntry();
                    entry.setQuestionNo(qNo);
                    entry.setType(type);
                    entry.setSection(sec);
                    entry.setCorrectKey(key);
                    entry.setMarks(marks);

                    String mapKey = sec + "_" + qNo;
                    keyMap.put(mapKey, entry);
                    log.debug("Parsed table row key entry: {} -> {}", mapKey, entry);
                    continue;
                } catch (Exception e) {
                    log.warn("Failed to parse table row pattern on line: {} due to: {}", trimmed, e.getMessage());
                }
            }

            // Try Prefixed Manual Pattern
            matcher = prefixedPattern.matcher(trimmed);
            if (matcher.find()) {
                try {
                    String sec = matcher.group(1).toUpperCase();
                    int qNo = Integer.parseInt(matcher.group(2));
                    String key = matcher.group(3).trim();

                    AnswerKeyEntry entry = new AnswerKeyEntry();
                    entry.setQuestionNo(qNo);
                    entry.setSection(sec);
                    entry.setCorrectKey(key);
                    
                    // Deduce type based on key content
                    if (key.equalsIgnoreCase("A") || key.equalsIgnoreCase("B") || key.equalsIgnoreCase("C") || key.equalsIgnoreCase("D")) {
                        entry.setType("MCQ");
                    } else if (key.contains(",") || key.contains("or") || key.contains("OR")) {
                        if (key.matches(".*\\d+.*")) {
                            entry.setType("NAT");
                        } else {
                            entry.setType("MSQ");
                        }
                    } else if (key.matches("-?\\d+(?:\\.\\d+)?.*")) {
                        entry.setType("NAT");
                    } else {
                        entry.setType("MCQ");
                    }

                    // Deduce marks based on standard question number layout
                    if (sec.equals("GA")) {
                        entry.setMarks(qNo <= 5 ? 1.0 : 2.0);
                    } else {
                        entry.setMarks(qNo <= 25 ? 1.0 : 2.0);
                    }

                    String mapKey = sec + "_" + qNo;
                    keyMap.put(mapKey, entry);
                    log.debug("Parsed prefixed manual key entry: {} -> {}", mapKey, entry);
                    continue;
                } catch (Exception e) {
                    log.warn("Failed to parse prefixed pattern on line: {} due to: {}", trimmed, e.getMessage());
                }
            }

            // Try Simple Pattern
            matcher = simplePattern.matcher(trimmed);
            if (matcher.find()) {
                try {
                    int globalOrSecNo = Integer.parseInt(matcher.group(1));
                    String key = matcher.group(2).trim();

                    String sec = currentSection;
                    int qNo = globalOrSecNo;

                    // If global sequence number is provided (1 to 65) while currentSection is GA
                    if (globalOrSecNo > 10 && "GA".equals(currentSection)) {
                        sec = "CS";
                        qNo = globalOrSecNo - 10;
                    }

                    AnswerKeyEntry entry = new AnswerKeyEntry();
                    entry.setQuestionNo(qNo);
                    entry.setSection(sec);
                    entry.setCorrectKey(key);

                    // Deduce type
                    if (key.equalsIgnoreCase("A") || key.equalsIgnoreCase("B") || key.equalsIgnoreCase("C") || key.equalsIgnoreCase("D")) {
                        entry.setType("MCQ");
                    } else if (key.contains(",") || key.contains("or") || key.contains("OR")) {
                        if (key.matches(".*\\d+.*")) {
                            entry.setType("NAT");
                        } else {
                            entry.setType("MSQ");
                        }
                    } else if (key.matches("-?\\d+(?:\\.\\d+)?.*")) {
                        entry.setType("NAT");
                    } else {
                        entry.setType("MCQ");
                    }

                    // Deduce marks
                    if (sec.equals("GA")) {
                        entry.setMarks(qNo <= 5 ? 1.0 : 2.0);
                    } else {
                        entry.setMarks(qNo <= 25 ? 1.0 : 2.0);
                    }

                    String mapKey = sec + "_" + qNo;
                    keyMap.put(mapKey, entry);
                    log.debug("Parsed simple key entry: {} -> {}", mapKey, entry);
                } catch (Exception e) {
                    log.warn("Failed to parse simple pattern on line: {} due to: {}", trimmed, e.getMessage());
                }
            }
        }

        log.info("Successfully parsed {} advanced answer key entries", keyMap.size());
        return keyMap;
    }
}
