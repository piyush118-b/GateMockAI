package com.gate.mockexam.controller;

import com.gate.mockexam.dto.*;
import com.gate.mockexam.entity.User;
import com.gate.mockexam.repository.UserRepository;
import com.gate.mockexam.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/analytics")
@Slf4j
@RequiredArgsConstructor
public class StudentAnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/my-summary")
    public ResponseEntity<?> getMySummary(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
            log.info("Fetching my-summary for student email={}", principal.getName());
            return ResponseEntity.ok(analyticsService.getStudentSummary(user.getId()));
        } catch (Exception e) {
            log.warn("Failed to get student summary: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/my-score-timeline")
    public ResponseEntity<?> getMyScoreTimeline(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
            log.info("Fetching my-score-timeline for student email={}", principal.getName());
            return ResponseEntity.ok(analyticsService.getStudentScoreTimeline(user.getId()));
        } catch (Exception e) {
            log.warn("Failed to get student score timeline: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/my-subject-radar")
    public ResponseEntity<?> getMySubjectRadar(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
            log.info("Fetching my-subject-radar for student email={}", principal.getName());
            return ResponseEntity.ok(analyticsService.getStudentSubjectRadar(user.getId()));
        } catch (Exception e) {
            log.warn("Failed to get student subject radar: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/my-rank")
    public ResponseEntity<?> getMyRank(@RequestParam("testId") UUID testId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
            log.info("Fetching my-rank for student email={}, testId={}", principal.getName(), testId);
            return ResponseEntity.ok(analyticsService.getStudentRank(testId, user.getId()));
        } catch (Exception e) {
            log.warn("Failed to get student rank: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
