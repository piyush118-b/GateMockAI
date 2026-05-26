package com.gate.mockexam.controller;

import com.gate.mockexam.dto.*;
import com.gate.mockexam.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/analytics")
@Slf4j
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/platform-summary")
    public ResponseEntity<PlatformSummaryDto> getPlatformSummary() {
        log.info("Fetching admin platform summary stats");
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }

    @GetMapping("/score-distribution")
    public ResponseEntity<List<ScoreBucketDto>> getScoreDistribution() {
        log.info("Fetching score distribution buckets");
        return ResponseEntity.ok(analyticsService.getScoreDistribution());
    }

    @GetMapping("/test-performance")
    public ResponseEntity<List<TestPerformanceDto>> getTestPerformance() {
        log.info("Fetching tests performance statistics");
        return ResponseEntity.ok(analyticsService.getTestPerformance());
    }

    @GetMapping("/subject-weakness")
    public ResponseEntity<List<SubjectWeaknessDto>> getSubjectWeakness() {
        log.info("Fetching platform subject weakness statistics");
        return ResponseEntity.ok(analyticsService.getSubjectWeakness());
    }

    @GetMapping("/student-leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getStudentLeaderboard(
            @RequestParam(value = "testId", required = false) UUID testId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("Fetching leaderboard for testId={}, limit={}", testId, limit);
        return ResponseEntity.ok(analyticsService.getStudentLeaderboard(testId, limit));
    }

    @GetMapping("/student/{userId}/profile")
    public ResponseEntity<?> getStudentProfile(@PathVariable("userId") UUID userId) {
        log.info("Fetching student profile statistics for userId={}", userId);
        try {
            return ResponseEntity.ok(analyticsService.getStudentProfile(userId));
        } catch (IllegalArgumentException e) {
            log.warn("Student profile not found: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
