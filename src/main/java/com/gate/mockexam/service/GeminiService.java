package com.gate.mockexam.service;

public interface GeminiService {
    String transcribePdfToQuestions(byte[] pdfBytes, String manualAnswerKey);
    String generateContent(String prompt);
    String generateJsonContent(String prompt);
    String generateJsonContent(String prompt, double temperature);
    String generateMockQuestions(String topic, String contextSection, String subjectWeightagesList, int totalCount);
    String callGeminiWithImage(String prompt, byte[] imageBytes, String mimeType);
}
