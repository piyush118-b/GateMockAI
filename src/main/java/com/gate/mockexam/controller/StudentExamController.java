package com.gate.mockexam.controller;

import com.gate.mockexam.entity.*;
import com.gate.mockexam.enums.AttemptStatus;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// @Controller
// @RequestMapping("/student")
@Slf4j
public class StudentExamController {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final UserRepository userRepository;

    public StudentExamController(
            MockTestRepository mockTestRepository,
            QuestionRepository questionRepository,
            AttemptRepository attemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            UserRepository userRepository) {
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/tests/{id}/take")
    public String takeTest(
            @PathVariable("id") UUID testId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

            MockTest test = mockTestRepository.findById(testId)
                    .orElseThrow(() -> new IllegalArgumentException("Mock test not found."));

            // Check if there is already an IN_PROGRESS attempt for this user and test
            List<Attempt> existingAttempts = attemptRepository.findByUserIdAndTestIdOrderByStartedAtDesc(user.getId(), testId);
            Attempt attempt = existingAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                    .findFirst()
                    .orElse(null);

            if (attempt == null) {
                // Initialize fresh secure attempt
                attempt = Attempt.builder()
                        .user(user)
                        .test(test)
                        .score(BigDecimal.ZERO)
                        .status(AttemptStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build();
                attempt = attemptRepository.save(attempt);
                log.info("Initialized new mock test attempt: {} for user: {}", attempt.getId(), user.getEmail());
            } else {
                log.info("Resumed active IN_PROGRESS mock test attempt: {} for user: {}", attempt.getId(), user.getEmail());
            }

            // Fetch questions sorted by sequence number
            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(testId);

            // Create a lightweight list of maps to prevent infinite recursion loop of JPA entities during JS inlining
            List<Map<String, Object>> questionsMeta = questions.stream()
                    .map(q -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", q.getId().toString());
                        map.put("type", q.getType().name());
                        return map;
                    })
                    .collect(Collectors.toList());

            model.addAttribute("test", test);
            model.addAttribute("attempt", attempt);
            model.addAttribute("questions", questions);
            model.addAttribute("questionsMeta", questionsMeta);
            model.addAttribute("pageTitle", "Mock Exam: " + test.getTitle());

            return "student/take";

        } catch (Exception e) {
            log.error("Failed to load secure mock test: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error launching test session: " + e.getMessage());
            return "redirect:/student/tests";
        }
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public String submitAttempt(
            @PathVariable("attemptId") UUID attemptId,
            HttpServletRequest request,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Attempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Exam session not found."));

            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
                redirectAttributes.addFlashAttribute("error", "This exam attempt has already been submitted.");
                return "redirect:/student/tests";
            }

            MockTest test = attempt.getTest();
            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(test.getId());

            BigDecimal totalScore = BigDecimal.ZERO;
            List<AttemptAnswer> answers = new ArrayList<>();

            for (Question question : questions) {
                AttemptAnswer answer = AttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .marksAwarded(BigDecimal.ZERO)
                        .isCorrect(false)
                        .build();

                if (question.getType() == QuestionType.MCQ) {
                    String mcqVal = request.getParameter("answer_mcq_" + question.getId());
                    if (mcqVal != null && !mcqVal.isBlank()) {
                        UUID selectedOptionId = UUID.fromString(mcqVal);
                        answer.setSelectedOptionIds(selectedOptionId.toString());

                        Option selectedOption = question.getOptions().stream()
                                .filter(o -> o.getId().equals(selectedOptionId))
                                .findFirst()
                                .orElse(null);

                        if (selectedOption != null && selectedOption.isCorrect()) {
                            answer.setIsCorrect(true);
                            answer.setMarksAwarded(question.getMarks());
                        } else {
                            answer.setIsCorrect(false);
                            // Subtract negative marks for MCQ
                            answer.setMarksAwarded(question.getNegativeMarks().negate());
                        }
                    } else {
                        // Unattempted
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                } else if (question.getType() == QuestionType.MSQ) {
                    String[] msqVals = request.getParameterValues("answer_msq_" + question.getId());
                    if (msqVals != null && msqVals.length > 0) {
                        List<UUID> selectedIds = Arrays.stream(msqVals)
                                .map(UUID::fromString)
                                .collect(Collectors.toList());

                        answer.setSelectedOptionIds(selectedIds.stream()
                                .map(UUID::toString)
                                .collect(Collectors.joining(",")));

                        // MSQ correctness rules: Must match all correct options exactly
                        Set<UUID> correctOptionIds = question.getOptions().stream()
                                .filter(Option::isCorrect)
                                .map(Option::getId)
                                .collect(Collectors.toSet());

                        Set<UUID> selectedOptionIds = new HashSet<>(selectedIds);

                        if (correctOptionIds.equals(selectedOptionIds)) {
                            answer.setIsCorrect(true);
                            answer.setMarksAwarded(question.getMarks());
                        } else {
                            answer.setIsCorrect(false);
                            answer.setMarksAwarded(BigDecimal.ZERO); // No negative marking for MSQs
                        }
                    } else {
                        // Unattempted
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                } else if (question.getType() == QuestionType.NAT) {
                    String natVal = request.getParameter("answer_nat_" + question.getId());
                    if (natVal != null && !natVal.isBlank()) {
                        try {
                            double enteredVal = Double.parseDouble(natVal.trim());
                            answer.setNatValueEntered(enteredVal);

                            double correctVal = question.getCorrectNatValue();
                            double tolerance = question.getNatTolerance() != null ? question.getNatTolerance() : 0.01;

                            if (Math.abs(enteredVal - correctVal) <= tolerance) {
                                answer.setIsCorrect(true);
                                answer.setMarksAwarded(question.getMarks());
                            } else {
                                answer.setIsCorrect(false);
                                answer.setMarksAwarded(BigDecimal.ZERO); // No negative marking for NATs
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid decimal number format entered for NAT: {}", natVal);
                            answer.setIsCorrect(false);
                            answer.setMarksAwarded(BigDecimal.ZERO);
                        }
                    } else {
                        // Unattempted
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                }

                totalScore = totalScore.add(answer.getMarksAwarded());
                answers.add(answer);
            }

            // Save attempt answers
            attemptAnswerRepository.saveAll(answers);

            // Persist submitted state
            attempt.setAnswers(answers);
            attempt.setScore(totalScore.setScale(2, RoundingMode.HALF_UP));
            attempt.setStatus(AttemptStatus.SUBMITTED);
            attempt.setSubmittedAt(LocalDateTime.now());
            attemptRepository.save(attempt);

            log.info("Successfully submitted mock exam attempt: {}. Score: {}", attempt.getId(), attempt.getScore());
            redirectAttributes.addFlashAttribute("success", "Congratulations! Your mock test has been submitted successfully.");

            return "redirect:/student/attempts/" + attempt.getId() + "/result";

        } catch (Exception e) {
            log.error("Failed to submit secure mock test attempt: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error submitting mock test: " + e.getMessage());
            return "redirect:/student/tests";
        }
    }

    @GetMapping("/attempts/{attemptId}/result")
    public String showResult(
            @PathVariable("attemptId") UUID attemptId,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Attempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt record not found."));

            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // Safety check: ensure student is viewing their own attempt!
            if (!attempt.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Access denied. You are not authorized to view this test scorecard.");
                return "redirect:/student/tests";
            }

            MockTest test = attempt.getTest();

            // Calculate metrics
            int correctCount = 0;
            int incorrectCount = 0;
            int skippedCount = 0;

            List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
            
            // Map answers by Question ID for fast Thymeleaf lookup
            Map<UUID, AttemptAnswer> answerMap = new HashMap<>();
            for (AttemptAnswer ans : answers) {
                answerMap.put(ans.getQuestion().getId(), ans);
                if (ans.getIsCorrect() == null) {
                    skippedCount++;
                } else if (ans.getIsCorrect()) {
                    correctCount++;
                } else {
                    incorrectCount++;
                }
            }

            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(test.getId());

            // Accuracy %
            double accuracy = 0.0;
            int attemptedCount = correctCount + incorrectCount;
            if (attemptedCount > 0) {
                accuracy = ((double) correctCount / attemptedCount) * 100.0;
            }

            model.addAttribute("attempt", attempt);
            model.addAttribute("test", test);
            model.addAttribute("questions", questions);
            model.addAttribute("answerMap", answerMap);
            model.addAttribute("correctCount", correctCount);
            model.addAttribute("incorrectCount", incorrectCount);
            model.addAttribute("skippedCount", skippedCount);
            model.addAttribute("accuracy", String.format("%.1f", accuracy));
            model.addAttribute("pageTitle", "Mock Exam Scorecard: " + test.getTitle());

            return "student/result";

        } catch (Exception e) {
            log.error("Failed to load mock scorecard: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error loading scorecard: " + e.getMessage());
            return "redirect:/student/tests";
        }
    }
}
