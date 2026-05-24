package com.gate.mockexam.service;

import com.gate.mockexam.entity.Attempt;
import com.gate.mockexam.entity.AttemptAnswer;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.repository.AttemptAnswerRepository;
import com.gate.mockexam.repository.AttemptRepository;
import com.gate.mockexam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;

    /**
     * TRACK 4 — Returns a rich analytics payload for a submitted attempt.
     * Ownership must be validated by the controller before calling this method.
     *
     * @param attemptId UUID of the attempt
     * @return analytics map suitable for direct JSON serialisation
     */
    public Map<String, Object> getAttemptAnalytics(UUID attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(attempt.getTest().getId());

        // ── Top-level metrics ─────────────────────────────────────────────────
        BigDecimal totalMarks = attempt.getTest().getTotalMarks();
        BigDecimal scored = attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO;

        long attempted = answers.stream().filter(a -> isAttempted(a)).count();
        long correct   = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

        double accuracy = attempted == 0 ? 0.0
                : Math.round((correct * 100.0 / attempted) * 100.0) / 100.0;

        long timeTakenSeconds = 0L;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            timeTakenSeconds = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        // ── Index answers by questionId ───────────────────────────────────────
        Map<UUID, AttemptAnswer> answerByQId = new HashMap<>();
        for (AttemptAnswer a : answers) {
            answerByQId.put(a.getQuestion().getId(), a);
        }

        // ── byType breakdown ──────────────────────────────────────────────────
        Map<String, long[]> byTypeMap = new LinkedHashMap<>(); // type → [attempted, correct]
        for (Question q : questions) {
            String type = q.getType().name();
            byTypeMap.putIfAbsent(type, new long[]{0, 0});
            AttemptAnswer ans = answerByQId.get(q.getId());
            if (ans != null && isAttempted(ans)) {
                byTypeMap.get(type)[0]++;
                if (Boolean.TRUE.equals(ans.getIsCorrect())) byTypeMap.get(type)[1]++;
            }
        }
        List<Map<String, Object>> byType = byTypeMap.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.getKey());
            m.put("attempted", e.getValue()[0]);
            m.put("correct", e.getValue()[1]);
            return m;
        }).collect(Collectors.toList());

        // ── bySubject breakdown ───────────────────────────────────────────────
        // Group by question subject/topic (test-level metadata). If the test has a single
        // subject (most ingested papers), all questions fall in one group.
        // For weighted papers, subject is set per-question via explanation metadata;
        // here we use the test-level subject as a reasonable fallback.
        String testSubject = attempt.getTest().getSubject() != null
                ? attempt.getTest().getSubject() : "General";

        Map<String, long[]> bySubjectMap = new LinkedHashMap<>(); // subject → [attempted, correct]
        Map<String, BigDecimal> marksEarnedBySubject = new LinkedHashMap<>();

        for (Question q : questions) {
            // Use test subject as grouping key (extend here if per-question subject is added later)
            String subj = testSubject;
            bySubjectMap.putIfAbsent(subj, new long[]{0, 0});
            marksEarnedBySubject.putIfAbsent(subj, BigDecimal.ZERO);

            AttemptAnswer ans = answerByQId.get(q.getId());
            if (ans != null && isAttempted(ans)) {
                bySubjectMap.get(subj)[0]++;
                if (Boolean.TRUE.equals(ans.getIsCorrect())) bySubjectMap.get(subj)[1]++;
                if (ans.getMarksAwarded() != null) {
                    marksEarnedBySubject.merge(subj, ans.getMarksAwarded(), BigDecimal::add);
                }
            }
        }
        List<Map<String, Object>> bySubject = bySubjectMap.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subject", e.getKey());
            m.put("attempted", e.getValue()[0]);
            m.put("correct", e.getValue()[1]);
            m.put("marks", marksEarnedBySubject.getOrDefault(e.getKey(), BigDecimal.ZERO));
            return m;
        }).collect(Collectors.toList());

        // ── weakQuestions: wrong answers sorted by marks desc, capped at 10 ──
        List<Map<String, Object>> weakQuestions = questions.stream()
                .filter(q -> {
                    AttemptAnswer ans = answerByQId.get(q.getId());
                    return ans != null && Boolean.FALSE.equals(ans.getIsCorrect());
                })
                .sorted(Comparator.comparing(Question::getMarks, Comparator.reverseOrder()))
                .limit(10)
                .map(q -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("questionId", q.getId().toString());
                    m.put("sequenceNo", q.getSequenceNo());
                    m.put("questionText", q.getQuestionText());
                    m.put("type", q.getType().name());
                    m.put("marks", q.getMarks());
                    m.put("explanation", q.getExplanation());
                    return m;
                })
                .collect(Collectors.toList());

        // ── Assemble response ──────────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attemptId.toString());
        result.put("testTitle", attempt.getTest().getTitle());
        result.put("totalMarks", totalMarks);
        result.put("scored", scored);
        result.put("accuracy", accuracy);
        result.put("timeTakenSeconds", timeTakenSeconds);
        result.put("questionsAttempted", attempted);
        result.put("questionsCorrect", correct);
        result.put("bySubject", bySubject);
        result.put("byType", byType);
        result.put("weakQuestions", weakQuestions);
        return result;
    }

    // An answer is "attempted" if at least one option was selected OR a NAT value entered
    private boolean isAttempted(AttemptAnswer a) {
        return (a.getSelectedOptionIds() != null && !a.getSelectedOptionIds().isBlank())
                || a.getNatValueEntered() != null;
    }
}
