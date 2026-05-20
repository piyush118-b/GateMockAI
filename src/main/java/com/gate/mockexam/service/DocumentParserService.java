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
}
