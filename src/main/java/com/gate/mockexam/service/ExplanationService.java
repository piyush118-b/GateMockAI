package com.gate.mockexam.service;

import com.gate.mockexam.entity.ExplanationCache;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.repository.ExplanationCacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExplanationService {

    @Autowired 
    private ExplanationCacheRepository explanationCacheRepository;
    
    @Autowired 
    private GeminiService geminiService;

    public String getExplanation(Question question, String studentAnswer, String correctAnswer) {
        // Check cache first
        return explanationCacheRepository
            .findByQuestionIdAndStudentAnswer(question.getId(), studentAnswer)
            .map(ExplanationCache::getExplanation)
            .orElseGet(() -> {
                String explanation = generateExplanation(question, studentAnswer, correctAnswer);
                ExplanationCache cache = new ExplanationCache();
                cache.setQuestion(question);
                cache.setStudentAnswer(studentAnswer);
                cache.setExplanation(explanation);
                explanationCacheRepository.save(cache);
                return explanation;
            });
    }

    private String generateExplanation(Question question, String studentAnswer, String correctAnswer) {
        String prompt = """
            Question: %s
            Student's answer: %s
            Correct answer: %s

            Explain in 3-5 numbered steps:
            1. Why the student's answer is incorrect
            2. The key concept being tested
            3. How to arrive at the correct answer step by step
            4. A memory tip to avoid this mistake in future

            Keep the total explanation under 200 words. Be direct and specific.
            Do not include preamble like "Sure!" or "Great question!".
            """.formatted(question.getQuestionText(), studentAnswer, correctAnswer);

        return geminiService.generateContent(prompt);
    }
}
