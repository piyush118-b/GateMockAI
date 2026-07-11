package com.gate.mockexam.service;

import com.gate.mockexam.dto.MockTestGenerationRequestDto;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.entity.Option;
import com.gate.mockexam.enums.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.gate.mockexam.service.GeminiService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class MockTestGenerationServiceTest {

    @Autowired
    private MockTestGenerationService mockTestGenerationService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockBean
    private GeminiService geminiService;

    // TRACK 2+3: new deps in RagIngestionService — mock to avoid needing live DB/Ollama
    @MockBean
    private org.springframework.ai.embedding.EmbeddingModel embeddingModel;

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private com.gate.mockexam.repository.UserRepository userRepository;

    @org.springframework.boot.test.mock.mockito.SpyBean
    private com.gate.mockexam.repository.MockTestRepository mockTestRepository;

    @Test
    public void testGenerateAndSaveTestSuccess() {
        when(mockTestRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        
        String mockJson = """
            {
              "title": "GATE Mock Test: CPU Scheduling",
              "topic": "CPU Scheduling",
              "subject": "Operating Systems",
              "durationMinutes": 180,
              "questions": [
                {
                  "sequenceNo": 1,
                  "type": "MCQ",
                  "questionText": "What is the average waiting time?",
                  "marks": 1.0,
                  "negativeMarks": 0.33,
                  "explanation": "Calculated value is 12.",
                  "options": [
                    {"label": "A", "text": "10", "isCorrect": false},
                    {"label": "B", "text": "12", "isCorrect": true},
                    {"label": "C", "text": "14", "isCorrect": false},
                    {"label": "D", "text": "16", "isCorrect": false}
                  ]
                },
                {
                  "sequenceNo": 2,
                  "type": "MSQ",
                  "questionText": "Which are CPU schedulers?",
                  "marks": 2.0,
                  "negativeMarks": 0.0,
                  "explanation": "All three are valid.",
                  "options": [
                    {"label": "A", "text": "Short-term", "isCorrect": true},
                    {"label": "B", "text": "Medium-term", "isCorrect": true},
                    {"label": "C", "text": "Long-term", "isCorrect": true},
                    {"label": "D", "text": "None", "isCorrect": false}
                  ]
                },
                {
                  "sequenceNo": 3,
                  "type": "NAT",
                  "questionText": "Calculate SJF switches.",
                  "marks": 1.0,
                  "negativeMarks": 0.0,
                  "correctNatValue": 4.0,
                  "natTolerance": 0.0,
                  "explanation": "N-1 switches."
                }
              ]
            }
            """;
        
        when(geminiService.generateJsonContent(any(String.class))).thenReturn(mockJson);

        // Prepare request DTO
        MockTestGenerationRequestDto request = new MockTestGenerationRequestDto();
        request.setTopic("CPU Scheduling");
        request.setSubject("Operating Systems");
        request.setMcqCount(1);
        request.setMsqCount(1);
        request.setNatCount(1);

        // Invoke generation service
        MockTest test = mockTestGenerationService.generateAndSaveTest(request);

        // Assert mapping and database saving
        assertThat(test).isNotNull();
        assertThat(test.getId()).isNotNull();
        assertThat(test.getTitle()).isEqualTo("GATE Mock Test: CPU Scheduling");
        assertThat(test.getTopic()).isEqualTo("CPU Scheduling");
        assertThat(test.getSubject()).isEqualTo("Operating Systems");
        assertThat(test.getDurationMinutes()).isEqualTo(180);
        assertThat(test.getTotalMarks()).isEqualByComparingTo(new BigDecimal("4.0"));
        assertThat(test.isPublished()).isFalse();

        assertThat(test.getQuestions()).hasSize(3);
        
        Question q1 = test.getQuestions().stream().filter(q -> q.getSequenceNo() == 1).findFirst().orElseThrow();
        assertThat(q1.getType()).isEqualTo(QuestionType.MCQ);
        assertThat(q1.getMarks()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(q1.getNegativeMarks()).isEqualByComparingTo(new BigDecimal("0.33"));
        assertThat(q1.getOptions()).hasSize(4);
        assertThat(q1.getOptions().stream().filter(Option::isCorrect).map(Option::getOptionLabel).findFirst().orElse('Z')).isEqualTo('B');

        Question q3 = test.getQuestions().stream().filter(q -> q.getSequenceNo() == 3).findFirst().orElseThrow();
        assertThat(q3.getType()).isEqualTo(QuestionType.NAT);
        assertThat(q3.getCorrectNatValue()).isEqualTo(4.0);
        assertThat(q3.getNatTolerance()).isEqualTo(0.0);
        assertThat(q3.getOptions()).isEmpty();
    }

    @Test
    public void testCorrectNatValueArrayDeserialization() throws Exception {
        String jsonWithArray = """
            {
              "sequenceNo": 12,
              "type": "NAT",
              "questionText": "Calculate NAT range.",
              "marks": 2.0,
              "negativeMarks": 0.0,
              "correctNatValue": [4.5, 4.6],
              "explanation": "Test array range"
            }
            """;
        
        com.gate.mockexam.dto.AiGeneratedQuestionDto dto = objectMapper.readValue(jsonWithArray, com.gate.mockexam.dto.AiGeneratedQuestionDto.class);
        assertThat(dto.getCorrectNatValue()).isEqualTo(4.55);
        assertThat(dto.getNatTolerance()).isEqualTo(0.05);

        String jsonWithStringArray = """
            {
              "sequenceNo": 13,
              "type": "NAT",
              "questionText": "Calculate NAT range as string.",
              "marks": 2.0,
              "negativeMarks": 0.0,
              "correctNatValue": "[4.5, 4.6]",
              "explanation": "Test string array range"
            }
            """;
        
        com.gate.mockexam.dto.AiGeneratedQuestionDto dto2 = objectMapper.readValue(jsonWithStringArray, com.gate.mockexam.dto.AiGeneratedQuestionDto.class);
        assertThat(dto2.getCorrectNatValue()).isEqualTo(4.55);
        assertThat(dto2.getNatTolerance()).isEqualTo(0.05);

        String jsonWithDouble = """
            {
              "sequenceNo": 14,
              "type": "NAT",
              "questionText": "Calculate NAT double.",
              "marks": 2.0,
              "negativeMarks": 0.0,
              "correctNatValue": 4.5,
              "explanation": "Test normal double"
            }
            """;
        
        com.gate.mockexam.dto.AiGeneratedQuestionDto dto3 = objectMapper.readValue(jsonWithDouble, com.gate.mockexam.dto.AiGeneratedQuestionDto.class);
        assertThat(dto3.getCorrectNatValue()).isEqualTo(4.5);
    }
}
