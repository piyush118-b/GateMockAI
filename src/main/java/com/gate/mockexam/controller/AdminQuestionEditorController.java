package com.gate.mockexam.controller;

import com.gate.mockexam.entity.Option;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.MockTestRepository;
import com.gate.mockexam.repository.OptionRepository;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.service.MockTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tests")
@RequiredArgsConstructor
@Slf4j
public class AdminQuestionEditorController {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final MockTestService mockTestService;

    // ── GET /api/admin/tests/{testId} ─────────────────────────────────────────
    @GetMapping("/{testId}")
    public ResponseEntity<?> getTestDetail(@PathVariable UUID testId) {
        MockTest test = mockTestService.getTestById(testId);
        if (test == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> questionsPayload = test.getQuestions().stream().map(q -> {
            Map<String, Object> qMap = new LinkedHashMap<>();
            qMap.put("id", q.getId().toString());
            qMap.put("sequenceNo", q.getSequenceNo());
            qMap.put("questionText", q.getQuestionText());
            qMap.put("type", q.getType().name());
            qMap.put("marks", q.getMarks());
            qMap.put("negativeMarks", q.getNegativeMarks());
            qMap.put("explanation", q.getExplanation());
            qMap.put("correctNatValue", q.getCorrectNatValue());
            qMap.put("natTolerance", q.getNatTolerance());
            qMap.put("imagePath", q.getImagePath());

            List<Map<String, Object>> optionsPayload = q.getOptions().stream().map(o -> {
                Map<String, Object> oMap = new LinkedHashMap<>();
                oMap.put("id", o.getId().toString());
                oMap.put("optionLabel", String.valueOf(o.getOptionLabel()));
                oMap.put("optionText", o.getOptionText());
                oMap.put("isCorrect", o.isCorrect());
                oMap.put("imagePath", o.getImagePath());
                return oMap;
            }).collect(Collectors.toList());

            qMap.put("options", optionsPayload);
            return qMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", test.getId().toString());
        response.put("title", test.getTitle());
        response.put("subject", test.getSubject());
        response.put("topic", test.getTopic());
        response.put("branch", test.getBranch());
        response.put("durationMinutes", test.getDurationMinutes());
        response.put("totalMarks", test.getTotalMarks());
        response.put("isPublished", test.isPublished());
        response.put("questions", questionsPayload);

        return ResponseEntity.ok(response);
    }

    // ── PATCH /api/admin/tests/{testId}/questions/{questionId} ───────────────
    @PatchMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<Map<String, String>> updateQuestion(
            @PathVariable UUID testId,
            @PathVariable UUID questionId,
            @RequestBody Map<String, Object> payload) {
        return questionRepository.findById(questionId).map(q -> {
            if (payload.containsKey("questionText"))
                q.setQuestionText((String) payload.get("questionText"));
            if (payload.containsKey("explanation"))
                q.setExplanation((String) payload.get("explanation"));
            if (payload.containsKey("marks"))
                q.setMarks(new BigDecimal(payload.get("marks").toString()));
            if (payload.containsKey("negativeMarks"))
                q.setNegativeMarks(new BigDecimal(payload.get("negativeMarks").toString()));
            if (payload.containsKey("imagePath"))
                q.setImagePath((String) payload.get("imagePath"));
            if (payload.containsKey("imageUrl"))
                q.setImageUrl((String) payload.get("imageUrl"));
            if (payload.containsKey("imageAltText"))
                q.setImageAltText((String) payload.get("imageAltText"));
            if (payload.containsKey("correctNatValue") && payload.get("correctNatValue") != null)
                q.setCorrectNatValue(Double.parseDouble(payload.get("correctNatValue").toString()));
            questionRepository.save(q);
            return ResponseEntity.ok(Map.of("status", "updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── PATCH /api/admin/tests/{testId}/options/{optionId} ───────────────────
    @PatchMapping("/{testId}/options/{optionId}")
    public ResponseEntity<Map<String, String>> updateOption(
            @PathVariable UUID testId,
            @PathVariable UUID optionId,
            @RequestBody Map<String, Object> payload) {
        return optionRepository.findById(optionId).map(o -> {
            if (payload.containsKey("optionText"))
                o.setOptionText((String) payload.get("optionText"));
            if (payload.containsKey("isCorrect"))
                o.setCorrect((Boolean) payload.get("isCorrect"));
            if (payload.containsKey("imagePath"))
                o.setImagePath((String) payload.get("imagePath"));
            optionRepository.save(o);
            return ResponseEntity.ok(Map.of("status", "updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/admin/tests/{testId}/questions ──────────────────────────────
    @PostMapping("/{testId}/questions")
    public ResponseEntity<Map<String, String>> addQuestion(
            @PathVariable UUID testId,
            @RequestBody Map<String, Object> payload) {
        return mockTestRepository.findById(testId).map(test -> {
            int maxSeq = test.getQuestions().stream()
                    .mapToInt(Question::getSequenceNo).max().orElse(0);
            Question q = Question.builder()
                    .test(test)
                    .questionText((String) payload.getOrDefault("questionText", "New Question"))
                    .type(QuestionType.MCQ)
                    .marks(BigDecimal.ONE)
                    .negativeMarks(new BigDecimal("0.33"))
                    .sequenceNo(maxSeq + 1)
                    .explanation("")
                    .build();
            q = questionRepository.save(q);
            for (char label : new char[]{'A', 'B', 'C', 'D'}) {
                Option opt = Option.builder()
                        .question(q).optionLabel(label).optionText("Option " + label).isCorrect(false).build();
                optionRepository.save(opt);
            }
            return ResponseEntity.ok(Map.of("questionId", q.getId().toString()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /api/admin/tests/{testId}/questions/{questionId} ──────────────
    @DeleteMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @PathVariable UUID testId,
            @PathVariable UUID questionId) {
        if (questionRepository.existsById(questionId)) {
            questionRepository.deleteById(questionId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }
}
