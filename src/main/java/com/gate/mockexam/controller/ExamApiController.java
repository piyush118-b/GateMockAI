package com.gate.mockexam.controller;

import com.gate.mockexam.entity.*;
import com.gate.mockexam.enums.AttemptStatus;
import com.gate.mockexam.enums.QuestionType;
import com.gate.mockexam.repository.*;
import com.gate.mockexam.service.SpacedRepetitionService;
import com.gate.mockexam.service.GateRankPredictor;
import com.gate.mockexam.service.ExplanationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exam")
@Slf4j
public class ExamApiController {

    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final UserRepository userRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final GateRankPredictor gateRankPredictor;
    private final ExplanationService explanationService;

    public ExamApiController(
            MockTestRepository mockTestRepository,
            QuestionRepository questionRepository,
            AttemptRepository attemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            UserRepository userRepository,
            SpacedRepetitionService spacedRepetitionService,
            GateRankPredictor gateRankPredictor,
            ExplanationService explanationService) {
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.userRepository = userRepository;
        this.spacedRepetitionService = spacedRepetitionService;
        this.gateRankPredictor = gateRankPredictor;
        this.explanationService = explanationService;
    }

    @GetMapping("/{testId}/session")
    public ResponseEntity<?> getSession(@PathVariable("testId") UUID testId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
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

            // Calculate remaining time
            long elapsedSeconds = java.time.Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
            long totalDurationSeconds = test.getDurationMinutes() * 60L;
            long timeLeftSeconds = Math.max(0, totalDurationSeconds - elapsedSeconds);

            // Load saved answers
            List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
            List<Map<String, Object>> savedAnswers = answers.stream().map(ans -> {
                Map<String, Object> map = new HashMap<>();
                map.put("questionId", ans.getQuestion().getId().toString());
                map.put("natValueEntered", ans.getNatValueEntered());
                map.put("selectedOptionIds", ans.getSelectedOptionIds());
                map.put("sequenceNo", ans.getQuestion().getSequenceNo());
                return map;
            }).collect(Collectors.toList());

            // Build payload
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("id", test.getId().toString());
            testMap.put("title", test.getTitle());
            testMap.put("branch", test.getBranch());
            testMap.put("topic", test.getTopic());
            testMap.put("subject", test.getSubject());
            testMap.put("durationMinutes", test.getDurationMinutes());
            testMap.put("totalQuestions", test.getQuestions().size());
            testMap.put("totalMarks", test.getTotalMarks());

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("fullName", user.getFullName());
            userMap.put("email", user.getEmail());

            Map<String, Object> attemptMap = new HashMap<>();
            attemptMap.put("id", attempt.getId().toString());
            attemptMap.put("startedAt", attempt.getStartedAt());
            attemptMap.put("user", userMap);

            Map<String, Object> response = new HashMap<>();
            response.put("test", testMap);
            response.put("attempt", attemptMap);
            response.put("timeLeftSeconds", timeLeftSeconds);
            response.put("savedAnswers", savedAnswers);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to load secure mock test session: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error launching test session: " + e.getMessage());
        }
    }

    @GetMapping("/{testId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable("testId") UUID testId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(testId);

            // Map to client DTO to avoid recursion and hide the correct answers/explanations
            List<Map<String, Object>> list = questions.stream().map(q -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", q.getId().toString());
                map.put("questionText", q.getQuestionText());
                map.put("type", q.getType().name());
                map.put("marks", q.getMarks());
                map.put("negativeMarks", q.getNegativeMarks());
                map.put("sequenceNo", q.getSequenceNo());
                map.put("imagePath", q.getImagePath());

                List<Map<String, Object>> optionsList = q.getOptions().stream().map(o -> {
                    Map<String, Object> oMap = new HashMap<>();
                    oMap.put("id", o.getId().toString());
                    oMap.put("optionLabel", String.valueOf(o.getOptionLabel()));
                    oMap.put("optionText", o.getOptionText());
                    oMap.put("imagePath", o.getImagePath());
                    return oMap;
                }).collect(Collectors.toList());

                map.put("options", optionsList);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("Failed to load questions payload: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading questions: " + e.getMessage());
        }
    }

    @PostMapping("/{testId}/save")
    public ResponseEntity<?> saveProgress(
            @PathVariable("testId") UUID testId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            String questionIdStr = (String) body.get("questionId");
            String responseVal = (String) body.get("response");

            UUID questionId = UUID.fromString(questionIdStr);
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));

            List<Attempt> existingAttempts = attemptRepository.findByUserIdAndTestIdOrderByStartedAtDesc(user.getId(), testId);
            Attempt attempt = existingAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active in-progress test attempt found"));

            // Find or create AttemptAnswer
            List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attempt.getId());
            AttemptAnswer attemptAnswer = answers.stream()
                    .filter(ans -> ans.getQuestion().getId().equals(questionId))
                    .findFirst()
                    .orElse(null);

            if (attemptAnswer == null) {
                attemptAnswer = AttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .marksAwarded(BigDecimal.ZERO)
                        .isCorrect(null)
                        .build();
            }

            if (responseVal == null || responseVal.trim().isEmpty()) {
                attemptAnswer.setSelectedOptionIds(null);
                attemptAnswer.setNatValueEntered(null);
            } else {
                if (question.getType() == QuestionType.NAT) {
                    try {
                        attemptAnswer.setNatValueEntered(Double.parseDouble(responseVal.trim()));
                        attemptAnswer.setSelectedOptionIds(null);
                    } catch (NumberFormatException e) {
                        attemptAnswer.setNatValueEntered(null);
                    }
                } else {
                    attemptAnswer.setSelectedOptionIds(responseVal);
                    attemptAnswer.setNatValueEntered(null);
                }
            }

            attemptAnswerRepository.save(attemptAnswer);
            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            log.error("Failed to save intermediate response: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error saving progress: " + e.getMessage());
        }
    }

    @PostMapping("/{testId}/submit")
    public ResponseEntity<?> submitExam(
            @PathVariable("testId") UUID testId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<Attempt> existingAttempts = attemptRepository.findByUserIdAndTestIdOrderByStartedAtDesc(user.getId(), testId);
            Attempt attempt = existingAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active in-progress test attempt found"));

            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(testId);
            List<Map<String, ?>> responses = (List<Map<String, ?>>) body.get("responses");

            // Build a fast lookup map from request responses
            Map<String, String> responseMap = new HashMap<>();
            Map<String, Integer> timeSpentMap = new HashMap<>();
            if (responses != null) {
                for (Map<String, ?> resp : responses) {
                    String qId = String.valueOf(resp.get("questionId"));
                    responseMap.put(qId, (String) resp.get("response"));
                    
                    Object tSpent = resp.get("timeSpentSeconds");
                    if (tSpent instanceof Number) {
                        timeSpentMap.put(qId, ((Number) tSpent).intValue());
                    } else if (tSpent instanceof String) {
                        try {
                            timeSpentMap.put(qId, Integer.parseInt((String) tSpent));
                        } catch (NumberFormatException e) {
                            timeSpentMap.put(qId, 0);
                        }
                    } else {
                        timeSpentMap.put(qId, 0);
                    }
                }
            }

            BigDecimal totalScore = BigDecimal.ZERO;
            List<AttemptAnswer> answersToSave = new ArrayList<>();

            // Fetch existing answers to update them, or create if missing
            List<AttemptAnswer> existingAnswers = attemptAnswerRepository.findByAttemptId(attempt.getId());
            Map<UUID, AttemptAnswer> existingAnswersMap = existingAnswers.stream()
                    .collect(Collectors.toMap(ans -> ans.getQuestion().getId(), ans -> ans));

            for (Question question : questions) {
                AttemptAnswer answer = existingAnswersMap.get(question.getId());
                if (answer == null) {
                    answer = AttemptAnswer.builder()
                            .attempt(attempt)
                            .question(question)
                            .build();
                }

                String responseVal = responseMap.get(question.getId().toString());
                Integer tSpent = timeSpentMap.getOrDefault(question.getId().toString(), 0);
                answer.setTimeSpentSeconds(tSpent);

                if (question.getType() == QuestionType.MCQ) {
                    if (responseVal != null && !responseVal.isBlank()) {
                        UUID selectedOptionId = UUID.fromString(responseVal);
                        answer.setSelectedOptionIds(selectedOptionId.toString());
                        answer.setNatValueEntered(null);

                        Option selectedOption = question.getOptions().stream()
                                .filter(o -> o.getId().equals(selectedOptionId))
                                .findFirst()
                                .orElse(null);

                        if (selectedOption != null && selectedOption.isCorrect()) {
                            answer.setIsCorrect(true);
                            answer.setMarksAwarded(question.getMarks());
                        } else {
                            answer.setIsCorrect(false);
                            answer.setMarksAwarded(question.getNegativeMarks().negate());
                        }
                    } else {
                        answer.setSelectedOptionIds(null);
                        answer.setNatValueEntered(null);
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                } else if (question.getType() == QuestionType.MSQ) {
                    if (responseVal != null && !responseVal.isBlank()) {
                        List<UUID> selectedIds = Arrays.stream(responseVal.split(","))
                                .map(UUID::fromString)
                                .collect(Collectors.toList());

                        answer.setSelectedOptionIds(selectedIds.stream()
                                .map(UUID::toString)
                                .collect(Collectors.joining(",")));
                        answer.setNatValueEntered(null);

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
                            answer.setMarksAwarded(BigDecimal.ZERO);
                        }
                    } else {
                        answer.setSelectedOptionIds(null);
                        answer.setNatValueEntered(null);
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                } else if (question.getType() == QuestionType.NAT) {
                    if (responseVal != null && !responseVal.isBlank()) {
                        try {
                            double enteredVal = Double.parseDouble(responseVal.trim());
                            answer.setNatValueEntered(enteredVal);
                            answer.setSelectedOptionIds(null);

                            double correctVal = question.getCorrectNatValue();
                            double tolerance = question.getNatTolerance() != null ? question.getNatTolerance() : 0.01;

                            if (Math.abs(enteredVal - correctVal) <= tolerance) {
                                  answer.setIsCorrect(true);
                                  answer.setMarksAwarded(question.getMarks());
                            } else {
                                  answer.setIsCorrect(false);
                                  answer.setMarksAwarded(BigDecimal.ZERO);
                            }
                        } catch (NumberFormatException e) {
                            answer.setNatValueEntered(null);
                            answer.setSelectedOptionIds(null);
                            answer.setIsCorrect(false);
                            answer.setMarksAwarded(BigDecimal.ZERO);
                        }
                    } else {
                        answer.setSelectedOptionIds(null);
                        answer.setNatValueEntered(null);
                        answer.setIsCorrect(null);
                        answer.setMarksAwarded(BigDecimal.ZERO);
                    }
                }

                totalScore = totalScore.add(answer.getMarksAwarded());
                
                // Change 9: update spaced repetition SM-2 schedule
                int quality = spacedRepetitionService.toQuality(answer);
                spacedRepetitionService.updateSchedule(answer, quality);

                answersToSave.add(answer);
            }

            attemptAnswerRepository.saveAll(answersToSave);

            attempt.setAnswers(answersToSave);
            attempt.setScore(totalScore.setScale(2, RoundingMode.HALF_UP));
            attempt.setStatus(AttemptStatus.SUBMITTED);
            attempt.setSubmittedAt(LocalDateTime.now());
            attemptRepository.save(attempt);

            log.info("REST submission complete for mock exam attempt: {}. Score: {}", attempt.getId(), attempt.getScore());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "redirectUrl", "/student/attempts/" + attempt.getId() + "/result"
            ));

        } catch (Exception e) {
            log.error("Failed to submit attempt via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error submitting exam: " + e.getMessage());
        }
    }

    @GetMapping("/attempts/{attemptId}/explanation/{questionId}")
    public ResponseEntity<Map<String, String>> getExplanation(
            @PathVariable("attemptId") UUID attemptId,
            @PathVariable("questionId") UUID questionId) {

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        AttemptAnswer aa = answers.stream()
                .filter(a -> a.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("AttemptAnswer not found"));

        String studentAnswer = "";
        if (aa.getQuestion().getType() == QuestionType.MCQ || aa.getQuestion().getType() == QuestionType.MSQ) {
            studentAnswer = aa.getSelectedOptionIds() != null ? aa.getSelectedOptionIds() : "Unanswered";
        } else if (aa.getQuestion().getType() == QuestionType.NAT) {
            studentAnswer = aa.getNatValueEntered() != null ? String.valueOf(aa.getNatValueEntered()) : "Unanswered";
        }

        String correctAnswer = "";
        if (aa.getQuestion().getType() == QuestionType.MCQ || aa.getQuestion().getType() == QuestionType.MSQ) {
            correctAnswer = aa.getQuestion().getOptions().stream()
                    .filter(Option::isCorrect)
                    .map(o -> o.getOptionLabel() + ": " + o.getOptionText())
                    .collect(Collectors.joining(", "));
        } else if (aa.getQuestion().getType() == QuestionType.NAT) {
            correctAnswer = String.valueOf(aa.getQuestion().getCorrectNatValue());
        }

        String explanation = explanationService.getExplanation(
            aa.getQuestion(), studentAnswer, correctAnswer
        );

        return ResponseEntity.ok(Map.of("explanation", explanation));
    }

    @GetMapping("/attempts/{attemptId}/rank-prediction")
    public ResponseEntity<GateRankPredictor.RankPrediction> getRankPrediction(
            @PathVariable("attemptId") UUID attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        GateRankPredictor.RankPrediction prediction = gateRankPredictor.predict(
            attempt.getScore() != null ? attempt.getScore().doubleValue() : 0.0,
            attempt.getTest().getTotalMarks() != null ? attempt.getTest().getTotalMarks().doubleValue() : 100.0
        );
        return ResponseEntity.ok(prediction);
    }

    @PostMapping("/revision/generate")
    public ResponseEntity<?> generateRevisionTest(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        MockTest revision = spacedRepetitionService.generateRevisionTest(user.getId());
        if (revision == null) {
            return ResponseEntity.ok(Map.of("message", "No questions due for review today. Great job!"));
        }
        return ResponseEntity.ok(Map.of("mockTestId", revision.getId().toString(), "questionCount", revision.getQuestions().size()));
    }

    @GetMapping("/revision/due-count")
    public ResponseEntity<Map<String, Integer>> getDueCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("dueCount", 0));
        }
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("dueCount", 0));
        }
        int count = attemptAnswerRepository.findDueForReview(user.getId(), java.time.LocalDate.now()).size();
        return ResponseEntity.ok(Map.of("dueCount", count));
    }
}
