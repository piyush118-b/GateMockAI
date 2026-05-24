package com.gate.mockexam.controller;

import com.gate.mockexam.entity.*;
import com.gate.mockexam.repository.*;
import com.gate.mockexam.service.AnalyticsService;
import com.gate.mockexam.service.MockTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@Slf4j
public class StudentPortalApiController {

    private final UserRepository userRepository;
    private final MockTestRepository mockTestRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final MockTestService mockTestService;
    private final AnalyticsService analyticsService;

    public StudentPortalApiController(
            UserRepository userRepository,
            MockTestRepository mockTestRepository,
            AttemptRepository attemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            QuestionRepository questionRepository,
            MockTestService mockTestService,
            AnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.mockTestRepository = mockTestRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.questionRepository = questionRepository;
        this.mockTestService = mockTestService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());

            List<Map<String, Object>> attemptsList = attempts.stream().map(att -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", att.getId().toString());
                map.put("testTitle", att.getTest().getTitle());
                map.put("testId", att.getTest().getId().toString());
                map.put("score", att.getScore());
                map.put("startedAt", att.getStartedAt());
                map.put("submittedAt", att.getSubmittedAt());
                map.put("status", att.getStatus().name());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("attemptsCount", attemptsList.size());
            response.put("attempts", attemptsList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load student REST dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading dashboard data: " + e.getMessage());
        }
    }

    @GetMapping("/tests")
    public ResponseEntity<?> getTests(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            List<MockTest> tests = mockTestRepository.findByIsPublished(true);
            List<Map<String, Object>> testList = tests.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId().toString());
                map.put("title", t.getTitle());
                map.put("durationMinutes", t.getDurationMinutes());
                map.put("totalMarks", t.getTotalMarks());
                map.put("subject", t.getSubject());
                map.put("topic", t.getTopic());
                map.put("branch", t.getBranch());
                map.put("yearLabel", t.getYearLabel());
                map.put("totalQuestions", t.getQuestions().size());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(testList);
        } catch (Exception e) {
            log.error("Failed to list mock tests via REST: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading mock tests: " + e.getMessage());
        }
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ResponseEntity<?> getAttemptResult(@PathVariable("attemptId") UUID attemptId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            Attempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt record not found."));

            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            // Secure validation: candidate can only view their own scorecards!
            if (!attempt.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Access Denied: unauthorized attempt lookup.");
            }

            MockTest test = attempt.getTest();
            List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);

            int correctCount = 0;
            int incorrectCount = 0;
            int skippedCount = 0;

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

            // Accuracy Calculation
            double accuracy = 0.0;
            int attemptedCount = correctCount + incorrectCount;
            if (attemptedCount > 0) {
                accuracy = ((double) correctCount / attemptedCount) * 100.0;
            }

            List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(test.getId());

            // Build detailed questions list with correct options, student answers, and explanation reviews
            List<Map<String, Object>> questionsReviewList = questions.stream().map(q -> {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId().toString());
                qMap.put("questionText", q.getQuestionText());
                qMap.put("type", q.getType().name());
                qMap.put("marks", q.getMarks());
                qMap.put("negativeMarks", q.getNegativeMarks());
                qMap.put("sequenceNo", q.getSequenceNo());
                qMap.put("explanation", q.getExplanation());
                qMap.put("correctNatValue", q.getCorrectNatValue());
                qMap.put("natTolerance", q.getNatTolerance());
                qMap.put("imagePath", q.getImagePath());

                List<Map<String, Object>> optionsList = q.getOptions().stream().map(o -> {
                    Map<String, Object> oMap = new HashMap<>();
                    oMap.put("id", o.getId().toString());
                    oMap.put("optionLabel", String.valueOf(o.getOptionLabel()));
                    oMap.put("optionText", o.getOptionText());
                    oMap.put("imagePath", o.getImagePath());
                    oMap.put("isCorrect", o.isCorrect()); // Included for post-exam review!
                    return oMap;
                }).collect(Collectors.toList());

                qMap.put("options", optionsList);

                // Add student's submitted response info
                AttemptAnswer studentAns = answerMap.get(q.getId());
                Map<String, Object> ansMap = new HashMap<>();
                if (studentAns != null) {
                    ansMap.put("selectedOptionIds", studentAns.getSelectedOptionIds());
                    ansMap.put("natValueEntered", studentAns.getNatValueEntered());
                    ansMap.put("isCorrect", studentAns.getIsCorrect());
                    ansMap.put("marksAwarded", studentAns.getMarksAwarded());
                } else {
                    ansMap.put("selectedOptionIds", null);
                    ansMap.put("natValueEntered", null);
                    ansMap.put("isCorrect", null);
                    ansMap.put("marksAwarded", 0.0);
                }
                qMap.put("userAnswer", ansMap);

                return qMap;
            }).collect(Collectors.toList());

            // Build payload
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("id", test.getId().toString());
            testMap.put("title", test.getTitle());
            testMap.put("branch", test.getBranch());
            testMap.put("subject", test.getSubject());
            testMap.put("topic", test.getTopic());
            testMap.put("durationMinutes", test.getDurationMinutes());
            testMap.put("totalMarks", test.getTotalMarks());

            Map<String, Object> attemptMap = new HashMap<>();
            attemptMap.put("id", attempt.getId().toString());
            attemptMap.put("score", attempt.getScore());
            attemptMap.put("startedAt", attempt.getStartedAt());
            attemptMap.put("submittedAt", attempt.getSubmittedAt());
            attemptMap.put("status", attempt.getStatus().name());

            Map<String, Object> response = new HashMap<>();
            response.put("attempt", attemptMap);
            response.put("test", testMap);
            response.put("correctCount", correctCount);
            response.put("incorrectCount", incorrectCount);
            response.put("skippedCount", skippedCount);
            response.put("accuracy", accuracy);
            response.put("questions", questionsReviewList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to load attempt result scorecard: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading scorecard: " + e.getMessage());
        }
    }

    // ── GET /api/student/attempts/{attemptId}/analytics ───────────────────────
    /**
     * TRACK 4: Rich post-submission analytics for a single attempt.
     * Returns score, accuracy, time taken, bySubject chart data, byType breakdown,
     * and the top-10 weakest questions (wrong, sorted by marks descending).
     */
    @GetMapping("/attempts/{attemptId}/analytics")
    public ResponseEntity<?> getAttemptAnalytics(
            @PathVariable("attemptId") UUID attemptId,
            java.security.Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            // Ownership check: student can only view their own analytics
            Attempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt not found."));
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));
            if (!attempt.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Access Denied: unauthorized attempt lookup.");
            }

            return ResponseEntity.ok(analyticsService.getAttemptAnalytics(attemptId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to load attempt analytics: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error loading analytics: " + e.getMessage());
        }
    }
}
