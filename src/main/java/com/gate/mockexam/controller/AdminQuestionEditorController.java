package com.gate.mockexam.controller;

import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.entity.Option;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.MockTestRepository;
import com.gate.mockexam.repository.OptionRepository;
import com.gate.mockexam.repository.QuestionRepository;
import com.gate.mockexam.service.MockTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/tests")
@RequiredArgsConstructor
@Slf4j
public class AdminQuestionEditorController {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final MockTestService mockTestService;

    // ── GET /admin/tests/{testId}/edit ───────────────────────────────────────
    @GetMapping("/{testId}/edit")
    public String editTest(@PathVariable UUID testId, Model model) {
        MockTest test = mockTestService.getTestById(testId);
        if (test == null) return "redirect:/admin/tests";
        model.addAttribute("test", test);
        model.addAttribute("pageTitle", "Edit Paper: " + test.getTitle());
        return "admin/test-edit";
    }

    // ── PATCH /admin/tests/{testId}/questions/{questionId} ───────────────────
    @PatchMapping("/{testId}/questions/{questionId}")
    @ResponseBody
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
            if (payload.containsKey("correctNatValue") && payload.get("correctNatValue") != null)
                q.setCorrectNatValue(Double.parseDouble(payload.get("correctNatValue").toString()));
            questionRepository.save(q);
            return ResponseEntity.ok(Map.of("status", "updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── PATCH /admin/tests/{testId}/options/{optionId} ───────────────────────
    @PatchMapping("/{testId}/options/{optionId}")
    @ResponseBody
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

    // ── POST /admin/tests/{testId}/questions ─────────────────────────────────
    @PostMapping("/{testId}/questions")
    @ResponseBody
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
            // Default 4 empty options
            for (char label : new char[]{'A', 'B', 'C', 'D'}) {
                Option opt = Option.builder()
                    .question(q).optionLabel(label).optionText("Option " + label).isCorrect(false).build();
                optionRepository.save(opt);
            }
            return ResponseEntity.ok(Map.of("questionId", q.getId().toString()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /admin/tests/{testId}/questions/{questionId} ──────────────────
    @DeleteMapping("/{testId}/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @PathVariable UUID testId,
            @PathVariable UUID questionId) {
        if (questionRepository.existsById(questionId)) {
            questionRepository.deleteById(questionId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    // ── GET /admin/tests/{testId}/export ─────────────────────────────────────
    @GetMapping("/{testId}/export")
    public String exportTest(@PathVariable UUID testId, Model model) {
        MockTest test = mockTestService.getTestById(testId);
        if (test == null) return "redirect:/admin/tests";
        model.addAttribute("test", test);
        return "admin/exam-print";
    }
}
