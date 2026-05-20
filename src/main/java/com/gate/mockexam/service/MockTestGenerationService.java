package com.gate.mockexam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.entity.Option;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.MockTestRepository;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class MockTestGenerationService {

    private final ChatClient chatClient;
    private final RagIngestionService ragIngestionService;
    private final ObjectMapper objectMapper;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    @Value("classpath:prompts/generate_mock_test.st")
    private Resource promptTemplate;

    @Value("classpath:prompts/generate_full_gate_segment.st")
    private Resource segmentPromptTemplate;

    @jakarta.annotation.PostConstruct
    public void init() {
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
    }



    public MockTest generateAndSaveTest(MockTestGenerationRequestDto request) {
        // Enforce limits to control Gemini API costs
        int totalRequested = request.getMcqCount() + request.getMsqCount() + request.getNatCount();
        if (totalRequested > 15) {
            throw new IllegalArgumentException("Cannot generate more than 15 total questions per test to control API costs.");
        }

        java.time.LocalDateTime oneHourAgo = java.time.LocalDateTime.now().minusHours(1);
        long recentGenerations = mockTestRepository.countByCreatedAtAfter(oneHourAgo);
        if (recentGenerations >= 5) {
            throw new IllegalStateException("AI Generation limit exceeded (Max 5 tests per hour to control API costs). Please try again later.");
        }

        // Step 1: RAG retrieval — get top 5 similar past questions as context
        List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(
            request.getTopic() + " " + request.getSubject(), 5
        );

        String contextQuestions = IntStream.range(0, contextDocs.size())
            .mapToObj(i -> (i + 1) + ". " + contextDocs.get(i).getText())
            .collect(Collectors.joining("\n\n"));

        // Step 2: Build and send prompt to Gemini with manual template placeholder replacements
        String renderedPrompt = loadTemplate()
            .replace("{contextCount}", String.valueOf(contextDocs.size()))
            .replace("{contextQuestions}", contextQuestions)
            .replace("{topic}", request.getTopic())
            .replace("{subject}", request.getSubject())
            .replace("{mcqCount}", String.valueOf(request.getMcqCount()))
            .replace("{msqCount}", String.valueOf(request.getMsqCount()))
            .replace("{natCount}", String.valueOf(request.getNatCount()));

        String rawJson = chatClient.prompt()
            .user(renderedPrompt)
            .call()
            .content();

        log.debug("Raw Gemini response: {}", rawJson);

        // Step 3: Parse JSON → DTOs
        AiGeneratedTestDto dto = parseResponse(rawJson);

        // Step 4: Map DTOs → JPA entities and persist
        return persistTest(dto);
    }

    private AiGeneratedTestDto parseResponse(String rawJson) {
        // Strip potential markdown fences if Gemini adds them despite instructions
        String cleaned = rawJson.trim()
            .replaceAll("^```json\\s*", "")
            .replaceAll("^```\\s*", "")
            .replaceAll("```$", "")
            .trim();
        try {
            return objectMapper.readValue(cleaned, AiGeneratedTestDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini JSON response: {}", cleaned, e);
            throw new RuntimeException("AI returned invalid JSON. Please retry.", e);
        }
    }

    @Transactional
    public MockTest persistTest(AiGeneratedTestDto dto) {
        MockTest test = MockTest.builder()
            .title(dto.getTitle())
            .topic(dto.getTopic())
            .subject(dto.getSubject())
            .durationMinutes(dto.getDurationMinutes())
            .isPublished(false)
            .build();

        // Compute total marks
        BigDecimal totalMarks = dto.getQuestions().stream()
            .map(q -> BigDecimal.valueOf(q.getMarks()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        test.setTotalMarks(totalMarks);

        test = mockTestRepository.save(test);

        for (AiGeneratedQuestionDto qDto : dto.getQuestions()) {
            Question question = Question.builder()
                .test(test)
                .questionText(qDto.getQuestionText())
                .type(QuestionType.valueOf(qDto.getType()))
                .marks(BigDecimal.valueOf(qDto.getMarks()))
                .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                .correctNatValue(qDto.getCorrectNatValue())
                .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                .sequenceNo(qDto.getSequenceNo())
                .explanation(qDto.getExplanation())
                .build();

            question = questionRepository.save(question);

            if (qDto.getOptions() != null) {
                for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                    Option opt = Option.builder()
                        .question(question)
                        .optionLabel(oDto.getLabel().charAt(0))
                        .optionText(oDto.getText())
                        .isCorrect(oDto.isCorrect())
                        .build();
                    opt = optionRepository.save(opt);
                    question.getOptions().add(opt);
                }
            }
            test.getQuestions().add(question);
        }

        return test;
    }

    @Transactional
    public MockTest generateFullGateCsePaper(java.util.function.Consumer<String> progressCallback) {
        log.info("Starting Full 65-Question GATE CSE AI Paper compilation...");
        
        // Step 1: Create new MockTest parent
        MockTest test = MockTest.builder()
            .title("AI-Generated Full 100-Mark GATE CSE Exam")
            .topic("Full Syllabus")
            .subject("Computer Science")
            .durationMinutes(180)
            .isPublished(false)
            .build();
        test = mockTestRepository.save(test);

        // Define segment definitions matching the syllabus blueprint
        String[][] segments = {
            {"General Aptitude", "General Aptitude", "Verbal Ability, Reading Comprehension, Sentence Correction, Quantitative Aptitude, Ratios, Percentages, Probability, Logical Reasoning", "1", "6", "2", "2"},
            {"Engineering Mathematics", "Discrete Mathematics, Linear Algebra, Probability, Calculus", "Logic, Sets, Relations, Functions, Recurrence, Graph Theory, Combinatorics, Matrices, Eigenvalues, Bayes Theorem, Limits, Differentiation", "11", "5", "2", "3"},
            {"Data Structures & Algorithms", "Programming, Data Structures, Algorithms", "Arrays, Linked Lists, Trees, Graphs, Hashing, Heaps, Time Complexity, Output Prediction, Pointer Analysis, Greedy, Dynamic Programming, Divide & Conquer, Backtracking, NP-Completeness", "21", "7", "4", "4"},
            {"Theory of Computation", "Theory of Computation, Compiler Design", "DFA/NFA, Regular Expressions, CFG, PDA, Turing Machines, Decidability, Parsing, Lexical Analysis, LR Parsing, Syntax Trees, Intermediate Code Generation", "36", "4", "2", "3"},
            {"Operating Systems & DBMS", "Operating Systems, Databases (DBMS)", "CPU Scheduling, Deadlocks, Memory Management, Paging, Synchronization, Semaphores, SQL, Normalization, Transactions, Concurrency Control, Serializability, Indexing", "45", "6", "3", "3"},
            {"Networks, COA & Logic", "Computer Networks, Computer Organization & Architecture (COA), Digital Logic", "TCP/IP, Routing, Congestion Control, Subnetting, Pipelining, Cache Memory, Cache Calculations, Addressing Modes, K-map, Sequential Circuits, Flip-flops", "57", "4", "2", "3"}
        };

        BigDecimal aggregatedTotalMarks = BigDecimal.ZERO;

        for (int i = 0; i < segments.length; i++) {
            String[] seg = segments[i];
            String segmentName = seg[0];
            String subjects = seg[1];
            String syllabusFocus = seg[2];
            String startSeq = seg[3];
            String mcqCount = seg[4];
            String msqCount = seg[5];
            String natCount = seg[6];

            int stepNo = i + 1;
            int percent = (stepNo * 100) / segments.length;
            
            // Notify progress
            progressCallback.accept(String.format("{\"step\": %d, \"message\": \"Compiling %s Section...\", \"percent\": %d}", stepNo, segmentName, percent));

            try {
                // RAG context retrieval
                List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(subjects + " " + syllabusFocus, 5);
                String contextQuestions = IntStream.range(0, contextDocs.size())
                    .mapToObj(idx -> (idx + 1) + ". " + contextDocs.get(idx).getText())
                    .collect(Collectors.joining("\n\n"));

                // Render segment prompt
                String prompt = loadSegmentTemplate()
                    .replace("{segmentName}", segmentName)
                    .replace("{subjects}", subjects)
                    .replace("{syllabusFocus}", syllabusFocus)
                    .replace("{startSequenceNo}", startSeq)
                    .replace("{mcqCount}", mcqCount)
                    .replace("{msqCount}", msqCount)
                    .replace("{natCount}", natCount)
                    .replace("{contextCount}", String.valueOf(contextDocs.size()))
                    .replace("{contextQuestions}", contextQuestions);

                // Call Gemini LLM
                String rawJson = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                String cleaned = rawJson.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("```$", "")
                    .trim();

                AiGeneratedQuestionsListDto listDto = objectMapper.readValue(cleaned, AiGeneratedQuestionsListDto.class);

                // Persist segment questions
                for (AiGeneratedQuestionDto qDto : listDto.getQuestions()) {
                    Question question = Question.builder()
                        .test(test)
                        .questionText(qDto.getQuestionText())
                        .type(QuestionType.valueOf(qDto.getType()))
                        .marks(BigDecimal.valueOf(qDto.getMarks()))
                        .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                        .correctNatValue(qDto.getCorrectNatValue())
                        .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                        .sequenceNo(qDto.getSequenceNo())
                        .explanation(qDto.getExplanation())
                        .build();

                    question = questionRepository.save(question);
                    aggregatedTotalMarks = aggregatedTotalMarks.add(question.getMarks());

                    if (qDto.getOptions() != null) {
                        for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                            Option opt = Option.builder()
                                .question(question)
                                .optionLabel(oDto.getLabel().charAt(0))
                                .optionText(oDto.getText())
                                .isCorrect(oDto.isCorrect())
                                .build();
                            opt = optionRepository.save(opt);
                            question.getOptions().add(opt);
                        }
                    }
                    test.getQuestions().add(question);
                }

            } catch (Exception e) {
                log.error("Failed to compile segment {}: {}", segmentName, e.getMessage(), e);
                throw new RuntimeException("AI Segment Compilation failed: " + e.getMessage(), e);
            }
        }

        test.setTotalMarks(aggregatedTotalMarks);
        test = mockTestRepository.save(test);

        log.info("Full 65-Question GATE CSE AI Paper compilation complete! Total Questions: {}, Total Marks: {}", 
            test.getQuestions().size(), test.getTotalMarks());

        return test;
    }

    private String loadTemplate() {
        try {
            return new String(promptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load prompt template", e);
        }
    }

    private String loadSegmentTemplate() {
        try {
            return new String(segmentPromptTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load segment prompt template", e);
        }
    }

    /**
     * Generates a full GATE paper based on admin-specified per-subject mark allocations.
     * subjectWeightages: Map<SubjectName, AllocatedMarks>
     */
    @Transactional
    public MockTest generateWeightedGatePaper(
            String branchCode,
            String yearLabel,
            java.util.Map<String, Integer> subjectWeightages,
            java.util.function.Consumer<String> progressCallback) {

        log.info("Starting Weighted GATE Paper compilation for branch={} year={}", branchCode, yearLabel);

        MockTest test = MockTest.builder()
            .title("GATE " + yearLabel + " " + branchCode + " — AI Generated Mock")
            .topic("Full Syllabus")
            .subject(branchCode)
            .branch(branchCode)
            .yearLabel(yearLabel)
            .durationMinutes(180)
            .isPublished(false)
            .build();
        test = mockTestRepository.save(test);

        BigDecimal aggregatedTotalMarks = BigDecimal.ZERO;
        int globalSeqNo = 1;
        int stepNo = 0;
        int total = subjectWeightages.size();

        for (java.util.Map.Entry<String, Integer> entry : subjectWeightages.entrySet()) {
            String subjectName = entry.getKey();
            int allocatedMarks = entry.getValue();
            if (allocatedMarks <= 0) continue;

            stepNo++;
            int percent = (stepNo * 100) / total;
            progressCallback.accept(String.format(
                "{\"step\": %d, \"message\": \"Compiling %s (%d marks)...\", \"percent\": %d}",
                stepNo, subjectName, allocatedMarks, percent));

            // Compute question split: 1-mark MCQs = 60%, 2-mark MCQs = 30%, NAT = 10%
            int oneMarkMcq = Math.max(1, (int) Math.round(allocatedMarks * 0.6));
            int twoMarkMcq = Math.max(0, (int) Math.round(allocatedMarks * 0.3 / 2));
            int nat       = Math.max(0, allocatedMarks - oneMarkMcq - (twoMarkMcq * 2));

            try {
                List<Document> contextDocs = ragIngestionService.retrieveSimilarQuestions(subjectName, 5);
                String contextQuestions = IntStream.range(0, contextDocs.size())
                    .mapToObj(idx -> (idx + 1) + ". " + contextDocs.get(idx).getText())
                    .collect(Collectors.joining("\n\n"));

                String prompt = loadSegmentTemplate()
                    .replace("{segmentName}", subjectName)
                    .replace("{subjects}", subjectName)
                    .replace("{syllabusFocus}", subjectName + " (comprehensive GATE syllabus)")
                    .replace("{startSequenceNo}", String.valueOf(globalSeqNo))
                    .replace("{mcqCount}", String.valueOf(oneMarkMcq + twoMarkMcq))
                    .replace("{msqCount}", "0")
                    .replace("{natCount}", String.valueOf(nat))
                    .replace("{contextCount}", String.valueOf(contextDocs.size()))
                    .replace("{contextQuestions}", contextQuestions);

                String rawJson = chatClient.prompt().user(prompt).call().content();
                String cleaned = rawJson.trim()
                    .replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("```$", "").trim();

                AiGeneratedQuestionsListDto listDto = objectMapper.readValue(cleaned, AiGeneratedQuestionsListDto.class);

                for (AiGeneratedQuestionDto qDto : listDto.getQuestions()) {
                    Question question = Question.builder()
                        .test(test)
                        .questionText(qDto.getQuestionText())
                        .type(QuestionType.valueOf(qDto.getType()))
                        .marks(BigDecimal.valueOf(qDto.getMarks()))
                        .negativeMarks(BigDecimal.valueOf(qDto.getNegativeMarks()))
                        .correctNatValue(qDto.getCorrectNatValue())
                        .natTolerance(qDto.getNatTolerance() != null ? qDto.getNatTolerance() : 0.0)
                        .sequenceNo(globalSeqNo++)
                        .explanation(qDto.getExplanation())
                        .build();

                    question = questionRepository.save(question);
                    aggregatedTotalMarks = aggregatedTotalMarks.add(question.getMarks());

                    if (qDto.getOptions() != null) {
                        for (AiGeneratedOptionDto oDto : qDto.getOptions()) {
                            Option opt = Option.builder()
                                .question(question)
                                .optionLabel(oDto.getLabel().charAt(0))
                                .optionText(oDto.getText())
                                .isCorrect(oDto.isCorrect())
                                .build();
                            opt = optionRepository.save(opt);
                            question.getOptions().add(opt);
                        }
                    }
                    test.getQuestions().add(question);
                }

            } catch (Exception e) {
                log.error("Failed to compile subject {}: {}", subjectName, e.getMessage(), e);
                throw new RuntimeException("AI Segment Compilation failed for " + subjectName + ": " + e.getMessage(), e);
            }
        }

        test.setTotalMarks(aggregatedTotalMarks);
        test = mockTestRepository.save(test);
        log.info("Weighted GATE Paper complete! Questions: {}, Marks: {}", test.getQuestions().size(), test.getTotalMarks());
        return test;
    }

    @lombok.Data
    public static class AiGeneratedQuestionsListDto {
        private List<AiGeneratedQuestionDto> questions;
    }
}
