package com.gate.mockexam.controller;

import com.gate.mockexam.dto.GeminiUsageSummaryDTO;
import com.gate.mockexam.dto.GeminiUsageHistoryDTO;
import com.gate.mockexam.service.GeminiUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/token-usage")
@Slf4j
@RequiredArgsConstructor
public class AdminTokenUsageController {

    private final GeminiUsageService geminiUsageService;

    @GetMapping("/today")
    public ResponseEntity<GeminiUsageSummaryDTO> getTodayUsage() {
        log.info("Fetching Gemini today token usage summary");
        return ResponseEntity.ok(geminiUsageService.getDailySummary());
    }

    @GetMapping("/history")
    public ResponseEntity<List<GeminiUsageHistoryDTO>> getHistoryUsage(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        log.info("Fetching Gemini historical token usage summary for last {} days", days);
        return ResponseEntity.ok(geminiUsageService.getHistoricalUsage(days));
    }
}
