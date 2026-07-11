package com.gate.mockexam.service;

import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.Attempt;
import com.gate.mockexam.entity.AttemptAnswer;
import com.gate.mockexam.entity.Question;
import com.gate.mockexam.entity.User;
import com.gate.mockexam.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final BranchSubjectRepository branchSubjectRepository;

    /**
     * TRACK 4 — Returns a rich analytics payload for a submitted attempt.
     */
    public Map<String, Object> getAttemptAnalytics(UUID attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        List<Question> questions = questionRepository.findByTestIdOrderBySequenceNoAsc(attempt.getTest().getId());

        BigDecimal totalMarks = attempt.getTest().getTotalMarks();
        BigDecimal scored = attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO;

        long attempted = answers.stream().filter(this::isAttempted).count();
        long correct   = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();

        double accuracy = attempted == 0 ? 0.0
                : Math.round((correct * 100.0 / attempted) * 100.0) / 100.0;

        long timeTakenSeconds = 0L;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            timeTakenSeconds = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        Map<UUID, AttemptAnswer> answerByQId = new HashMap<>();
        for (AttemptAnswer a : answers) {
            answerByQId.put(a.getQuestion().getId(), a);
        }

        Map<String, long[]> byTypeMap = new LinkedHashMap<>();
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

        String testSubject = attempt.getTest().getSubject() != null
                ? attempt.getTest().getSubject() : "General";

        Map<String, long[]> bySubjectMap = new LinkedHashMap<>();
        Map<String, BigDecimal> marksEarnedBySubject = new LinkedHashMap<>();

        for (Question q : questions) {
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

    private boolean isAttempted(AttemptAnswer a) {
        return (a.getSelectedOptionIds() != null && !a.getSelectedOptionIds().isBlank())
                || a.getNatValueEntered() != null;
    }

    // ── 1. GET /platform-summary ─────────────────────────────────────────────
    public PlatformSummaryDto getPlatformSummary() {
        long totalStudents = userRepository.countByRole(com.gate.mockexam.enums.UserRole.STUDENT);
        List<Attempt> submittedAttempts = attemptRepository.findAllSubmittedAttemptsWithTest();

        long totalAttempts = submittedAttempts.size();
        if (totalAttempts == 0) {
            return new PlatformSummaryDto(totalStudents, 0, 0.0, 0.0, 0.0);
        }

        double sumScores = 0.0;
        long passedCount = 0;
        double sumTimeMins = 0.0;

        for (Attempt a : submittedAttempts) {
            double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
            double totalMarks = a.getTest().getTotalMarks() != null ? a.getTest().getTotalMarks().doubleValue() : 0.0;
            sumScores += score;
            if (score >= (totalMarks * 0.5)) {
                passedCount++;
            }
            if (a.getStartedAt() != null && a.getSubmittedAt() != null) {
                sumTimeMins += Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes();
            }
        }

        double avgScore = sumScores / totalAttempts;
        double passRate = (double) passedCount * 100.0 / totalAttempts;
        double avgTimeTakenMinutes = sumTimeMins / totalAttempts;

        avgScore = Math.round(avgScore * 100.0) / 100.0;
        passRate = Math.round(passRate * 100.0) / 100.0;
        avgTimeTakenMinutes = Math.round(avgTimeTakenMinutes * 100.0) / 100.0;

        return new PlatformSummaryDto(totalStudents, totalAttempts, avgScore, passRate, avgTimeTakenMinutes);
    }

    // ── 2. GET /score-distribution ───────────────────────────────────────────
    public List<ScoreBucketDto> getScoreDistribution() {
        List<Attempt> submittedAttempts = attemptRepository.findAllSubmittedAttemptsWithTest();
        Map<String, Integer> bucketMap = new LinkedHashMap<>();
        bucketMap.put("0-10", 0);
        bucketMap.put("10-20", 0);
        bucketMap.put("20-30", 0);
        bucketMap.put("30-40", 0);
        bucketMap.put("40-50", 0);
        bucketMap.put("50-60", 0);
        bucketMap.put("60-70", 0);
        bucketMap.put("70-80", 0);
        bucketMap.put("80-90", 0);
        bucketMap.put("90-100", 0);

        for (Attempt a : submittedAttempts) {
            double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
            double totalMarks = a.getTest().getTotalMarks() != null ? a.getTest().getTotalMarks().doubleValue() : 0.0;
            if (totalMarks <= 0) continue;
            double pct = (score / totalMarks) * 100.0;

            String bucket;
            if (pct < 10) bucket = "0-10";
            else if (pct < 20) bucket = "10-20";
            else if (pct < 30) bucket = "20-30";
            else if (pct < 40) bucket = "30-40";
            else if (pct < 50) bucket = "40-50";
            else if (pct < 60) bucket = "50-60";
            else if (pct < 70) bucket = "60-70";
            else if (pct < 80) bucket = "70-80";
            else if (pct < 90) bucket = "80-90";
            else bucket = "90-100";

            bucketMap.put(bucket, bucketMap.get(bucket) + 1);
        }

        List<ScoreBucketDto> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bucketMap.entrySet()) {
            list.add(new ScoreBucketDto(e.getKey(), e.getValue()));
        }
        return list;
    }

    // ── 3. GET /test-performance ─────────────────────────────────────────────
    public List<TestPerformanceDto> getTestPerformance() {
        List<Attempt> submittedAttempts = attemptRepository.findAllSubmittedAttemptsWithTest();
        Map<com.gate.mockexam.entity.MockTest, List<Attempt>> attemptsByTest = submittedAttempts.stream()
                .collect(Collectors.groupingBy(Attempt::getTest));

        List<TestPerformanceDto> list = new ArrayList<>();
        for (Map.Entry<com.gate.mockexam.entity.MockTest, List<Attempt>> entry : attemptsByTest.entrySet()) {
            com.gate.mockexam.entity.MockTest test = entry.getKey();
            List<Attempt> testAttempts = entry.getValue();

            long count = testAttempts.size();
            double sumScores = 0.0;
            double sumTime = 0.0;
            long passedCount = 0;
            double maxScore = Double.NEGATIVE_INFINITY;
            double minScore = Double.POSITIVE_INFINITY;
            double totalMarks = test.getTotalMarks() != null ? test.getTotalMarks().doubleValue() : 0.0;

            for (Attempt a : testAttempts) {
                double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
                sumScores += score;
                maxScore = Math.max(maxScore, score);
                minScore = Math.min(minScore, score);
                if (score >= (totalMarks * 0.5)) {
                    passedCount++;
                }
                if (a.getStartedAt() != null && a.getSubmittedAt() != null) {
                    sumTime += Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes();
                }
            }

            double avgScore = Math.round((sumScores / count) * 100.0) / 100.0;
            double avgTime = Math.round((sumTime / count) * 100.0) / 100.0;
            double passRate = Math.round(((double) passedCount * 100.0 / count) * 100.0) / 100.0;
            maxScore = maxScore == Double.NEGATIVE_INFINITY ? 0.0 : Math.round(maxScore * 100.0) / 100.0;
            minScore = minScore == Double.POSITIVE_INFINITY ? 0.0 : Math.round(minScore * 100.0) / 100.0;

            list.add(new TestPerformanceDto(
                test.getId(),
                test.getTitle(),
                count,
                avgScore,
                avgTime,
                passRate,
                maxScore,
                minScore
            ));
        }

        list.sort(Comparator.comparingLong(TestPerformanceDto::attemptCount).reversed());
        return list;
    }

    // ── 4. GET /subject-weakness ─────────────────────────────────────────────
    public List<SubjectWeaknessDto> getSubjectWeakness() {
        List<AttemptAnswer> allAnswers = attemptAnswerRepository.findAll().stream()
                .filter(aa -> aa.getAttempt().getStatus() == com.gate.mockexam.enums.AttemptStatus.SUBMITTED)
                .collect(Collectors.toList());

        List<String> activeSubjectNames = branchSubjectRepository.findAllActiveSubjectNames();
        Map<String, long[]> subjectStats = new HashMap<>();
        for (String sub : activeSubjectNames) {
            subjectStats.put(sub.toUpperCase(), new long[]{0, 0});
        }

        for (AttemptAnswer aa : allAnswers) {
            String sub = aa.getQuestion().getTest().getSubject();
            if (sub == null) continue;
            String key = sub.toUpperCase().trim();
            if (subjectStats.containsKey(key)) {
                long[] stats = subjectStats.get(key);
                stats[0]++;
                if (Boolean.TRUE.equals(aa.getIsCorrect())) {
                    stats[1]++;
                }
            }
        }

        List<SubjectWeaknessDto> result = new ArrayList<>();
        for (String subName : activeSubjectNames) {
            long[] stats = subjectStats.get(subName.toUpperCase());
            long total = stats[0];
            long correct = stats[1];
            double accuracyPct = total > 0 ? (correct * 100.0 / total) : 0.0;
            accuracyPct = Math.round(accuracyPct * 100.0) / 100.0;
            result.add(new SubjectWeaknessDto(subName, total, correct, accuracyPct));
        }

        result.sort(Comparator.comparingDouble(SubjectWeaknessDto::accuracyPct));
        return result;
    }

    // ── 5. GET /student-leaderboard ──────────────────────────────────────────
    public List<LeaderboardEntryDto> getStudentLeaderboard(UUID testId, Integer limit) {
        List<Attempt> submittedAttempts = attemptRepository.findAllSubmittedAttemptsWithUserAndTest();
        if (testId != null) {
            submittedAttempts = submittedAttempts.stream()
                    .filter(a -> a.getTest().getId().equals(testId))
                    .collect(Collectors.toList());
        }

        int maxLimit = limit != null ? limit : 10;
        Map<User, List<Attempt>> attemptsByUser = submittedAttempts.stream()
                .collect(Collectors.groupingBy(Attempt::getUser));

        List<LeaderboardEntryDto> list = new ArrayList<>();
        for (Map.Entry<User, List<Attempt>> entry : attemptsByUser.entrySet()) {
            User user = entry.getKey();
            List<Attempt> userAttempts = entry.getValue();

            long attemptCount = userAttempts.size();
            LocalDateTime lastAttemptDate = userAttempts.stream()
                    .map(Attempt::getSubmittedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            double bestScore;
            double avgScore = userAttempts.stream()
                    .mapToDouble(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                    .average()
                    .orElse(0.0);

            if (testId != null) {
                bestScore = userAttempts.stream()
                        .mapToDouble(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                        .max()
                        .orElse(0.0);
            } else {
                Map<com.gate.mockexam.entity.MockTest, Double> bestPerTest = new HashMap<>();
                for (Attempt a : userAttempts) {
                    double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
                    bestPerTest.merge(a.getTest(), score, Math::max);
                }
                bestScore = bestPerTest.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }

            bestScore = Math.round(bestScore * 100.0) / 100.0;
            avgScore = Math.round(avgScore * 100.0) / 100.0;

            list.add(new LeaderboardEntryDto(
                user.getId(),
                user.getFullName(),
                attemptCount,
                bestScore,
                avgScore,
                lastAttemptDate
            ));
        }

        list.sort((e1, e2) -> Double.compare(e2.bestScore(), e1.bestScore()));
        return list.stream().limit(maxLimit).collect(Collectors.toList());
    }

    // ── 6. GET /student/{userId}/profile ─────────────────────────────────────
    public StudentProfileDto getStudentProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + userId));

        List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<AttemptAnswer> submittedAnswers = attemptAnswerRepository.findSubmittedAnswersByUserId(userId);

        StudentDetailsDto student = new StudentDetailsDto(user.getId(), user.getFullName(), user.getEmail());

        long totalAttempts = attempts.size();
        List<Attempt> submittedAttempts = attempts.stream()
                .filter(a -> a.getStatus() == com.gate.mockexam.enums.AttemptStatus.SUBMITTED)
                .collect(Collectors.toList());

        double avgScore = submittedAttempts.stream()
                .mapToDouble(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                .average()
                .orElse(0.0);

        double bestScore = submittedAttempts.stream()
                .mapToDouble(a -> a.getScore() != null ? a.getScore().doubleValue() : 0.0)
                .max()
                .orElse(0.0);

        double totalTimeSpentMins = submittedAttempts.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null)
                .mapToDouble(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes())
                .sum();

        avgScore = Math.round(avgScore * 100.0) / 100.0;
        bestScore = Math.round(bestScore * 100.0) / 100.0;

        Map<String, List<AttemptAnswer>> answersBySubject = submittedAnswers.stream()
                .filter(aa -> aa.getQuestion().getTest().getSubject() != null)
                .collect(Collectors.groupingBy(aa -> aa.getQuestion().getTest().getSubject()));

        List<SubjectAccuracyDto> bySubject = new ArrayList<>();
        for (Map.Entry<String, List<AttemptAnswer>> entry : answersBySubject.entrySet()) {
            String subject = entry.getKey();
            List<AttemptAnswer> list = entry.getValue();
            long attempted = list.size();
            long correct = list.stream().filter(aa -> Boolean.TRUE.equals(aa.getIsCorrect())).count();
            double accuracy = attempted > 0 ? (correct * 100.0 / attempted) : 0.0;
            accuracy = Math.round(accuracy * 100.0) / 100.0;
            bySubject.add(new SubjectAccuracyDto(subject, attempted, correct, accuracy));
        }

        List<AttemptHistoryEntryDto> attemptHistory = attempts.stream().map(a -> {
            double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
            double totalMarks = a.getTest().getTotalMarks() != null ? a.getTest().getTotalMarks().doubleValue() : 0.0;
            double time = a.getStartedAt() != null && a.getSubmittedAt() != null 
                    ? Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes() : 0.0;

            return new AttemptHistoryEntryDto(
                a.getId(),
                a.getTest().getId(),
                a.getTest().getTitle(),
                score,
                totalMarks,
                time,
                a.getSubmittedAt(),
                a.getStatus().name()
            );
        }).collect(Collectors.toList());

        Map<Question, List<AttemptAnswer>> answersByQuestion = submittedAnswers.stream()
                .collect(Collectors.groupingBy(AttemptAnswer::getQuestion));

        List<WeakQuestionDto> weakQuestions = new ArrayList<>();
        for (Map.Entry<Question, List<AttemptAnswer>> entry : answersByQuestion.entrySet()) {
            Question q = entry.getKey();
            List<AttemptAnswer> qAnswers = entry.getValue();

            long wrongCount = qAnswers.stream().filter(aa -> Boolean.FALSE.equals(aa.getIsCorrect())).count();
            long totalSeen = qAnswers.size();

            if (wrongCount > 0) {
                weakQuestions.add(new WeakQuestionDto(
                    q.getId(),
                    q.getSequenceNo(),
                    q.getQuestionText(),
                    wrongCount,
                    totalSeen,
                    q.getExplanation()
                ));
            }
        }

        weakQuestions.sort((wq1, wq2) -> Long.compare(wq2.wrongCount(), wq1.wrongCount()));
        weakQuestions = weakQuestions.stream().limit(10).collect(Collectors.toList());

        return new StudentProfileDto(
            student,
            totalAttempts,
            avgScore,
            bestScore,
            totalTimeSpentMins,
            bySubject,
            attemptHistory,
            weakQuestions
        );
    }

    // ── 7. GET /my-summary ───────────────────────────────────────────────────
    public StudentSummaryDto getStudentSummary(UUID userId) {
        StudentProfileDto profile = getStudentProfile(userId);

        double scoreTrend = 0.0;
        List<AttemptHistoryEntryDto> submittedHistory = profile.attemptHistory().stream()
                .filter(h -> "SUBMITTED".equals(h.status()))
                .sorted(Comparator.comparing(AttemptHistoryEntryDto::submittedAt).reversed())
                .collect(Collectors.toList());

        if (submittedHistory.size() >= 2) {
            scoreTrend = submittedHistory.get(0).score() - submittedHistory.get(1).score();
            scoreTrend = Math.round(scoreTrend * 100.0) / 100.0;
        }

        List<Object[]> avgScores = attemptRepository.getAverageScoresByStudent();
        List<Map.Entry<UUID, Double>> sortedStudents = avgScores.stream()
                .map(row -> Map.entry((UUID) row[0], ((Double) row[1])))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        int globalRank = 0;
        for (int i = 0; i < sortedStudents.size(); i++) {
            if (sortedStudents.get(i).getKey().equals(userId)) {
                globalRank = i + 1;
                break;
            }
        }
        if (globalRank == 0 && !submittedHistory.isEmpty()) {
            globalRank = sortedStudents.size() + 1;
        }

        return new StudentSummaryDto(
            profile.student(),
            profile.totalAttempts(),
            profile.avgScore(),
            profile.bestScore(),
            profile.totalTimeSpentMins(),
            profile.bySubject(),
            profile.attemptHistory(),
            profile.weakQuestions(),
            scoreTrend,
            globalRank
        );
    }

    // ── 8. GET /my-score-timeline ────────────────────────────────────────────
    public List<ScoreTimelineEntryDto> getStudentScoreTimeline(UUID userId) {
        List<Attempt> attempts = attemptRepository.findSubmittedAttemptsWithTestChronological(userId);
        return attempts.stream().map(a -> {
            double score = a.getScore() != null ? a.getScore().doubleValue() : 0.0;
            double totalMarks = a.getTest().getTotalMarks() != null ? a.getTest().getTotalMarks().doubleValue() : 0.0;
            double pct = totalMarks > 0 ? (score / totalMarks) * 100.0 : 0.0;
            pct = Math.round(pct * 10.0) / 10.0;

            return new ScoreTimelineEntryDto(
                a.getId(),
                a.getTest().getTitle(),
                a.getSubmittedAt(),
                pct
            );
        }).collect(Collectors.toList());
    }

    // ── 9. GET /my-subject-radar ─────────────────────────────────────────────
    public List<SubjectAccuracyDto> getStudentSubjectRadar(UUID userId) {
        List<String> activeSubjects = branchSubjectRepository.findAllActiveSubjectNames();
        List<AttemptAnswer> submittedAnswers = attemptAnswerRepository.findSubmittedAnswersByUserId(userId);

        Map<String, List<AttemptAnswer>> answersBySubject = submittedAnswers.stream()
                .filter(aa -> aa.getQuestion().getTest().getSubject() != null)
                .collect(Collectors.groupingBy(aa -> aa.getQuestion().getTest().getSubject().toUpperCase().trim()));

        List<SubjectAccuracyDto> result = new ArrayList<>();
        for (String subjectName : activeSubjects) {
            List<AttemptAnswer> qAnswers = answersBySubject.get(subjectName.toUpperCase().trim());
            long attempted = qAnswers != null ? qAnswers.size() : 0;
            long correct = qAnswers != null ? qAnswers.stream().filter(aa -> Boolean.TRUE.equals(aa.getIsCorrect())).count() : 0;
            double accuracy = attempted > 0 ? (correct * 100.0 / attempted) : 0.0;
            accuracy = Math.round(accuracy * 100.0) / 100.0;

            result.add(new SubjectAccuracyDto(subjectName, attempted, correct, accuracy));
        }

        return result;
    }

    // ── 10. GET /my-rank ─────────────────────────────────────────────────────
    public RankResponseDto getStudentRank(UUID testId, UUID userId) {
        com.gate.mockexam.entity.MockTest test = attemptRepository.findAllSubmittedAttemptsWithTest().stream()
                .map(Attempt::getTest)
                .filter(t -> t.getId().equals(testId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testId));

        List<Object[]> bestScores = attemptRepository.getBestScoresByStudentForTest(testId);
        List<Map.Entry<UUID, Double>> sortedScores = bestScores.stream()
                .map(row -> Map.entry((UUID) row[0], ((Double) row[1])))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .collect(Collectors.toList());

        int rank = 0;
        int totalStudents = sortedScores.size();
        for (int i = 0; i < totalStudents; i++) {
            if (sortedScores.get(i).getKey().equals(userId)) {
                rank = i + 1;
                break;
            }
        }

        double percentile = 0.0;
        if (totalStudents > 0 && rank > 0) {
            percentile = ((double) (totalStudents - rank) / totalStudents) * 100.0;
            percentile = Math.round(percentile * 10.0) / 10.0;
        }

        return new RankResponseDto(rank, totalStudents, percentile, test.getTitle());
    }

    public Map<UUID, Integer> getTimePerQuestion(UUID attemptId) {
        return attemptAnswerRepository.findByAttemptId(attemptId)
            .stream()
            .collect(Collectors.toMap(
                aa -> aa.getQuestion().getId(),
                AttemptAnswer::getTimeSpentSeconds
            ));
    }

    public double getAverageTimePerMark(UUID attemptId, int marks) {
        return attemptAnswerRepository.findByAttemptId(attemptId)
            .stream()
            .filter(aa -> aa.getQuestion().getMarks() != null && aa.getQuestion().getMarks().intValue() == marks)
            .mapToInt(AttemptAnswer::getTimeSpentSeconds)
            .average()
            .orElse(0.0);
    }

    public Map<String, Double> getSubjectAccuracy(UUID userId) {
        return attemptAnswerRepository
            .findSubmittedAnswersByUserId(userId)
            .stream()
            .collect(Collectors.groupingBy(
                aa -> aa.getQuestion().getTest().getSubject() != null ? aa.getQuestion().getTest().getSubject() : "General",
                Collectors.averagingDouble(aa -> aa.getIsCorrect() != null && aa.getIsCorrect() ? 1.0 : 0.0)
            ));
    }
}
