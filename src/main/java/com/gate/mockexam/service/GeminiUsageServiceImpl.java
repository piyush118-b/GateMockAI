package com.gate.mockexam.service;

import com.gate.mockexam.dto.GeminiUsageSummaryDTO;
import com.gate.mockexam.dto.GeminiUsageHistoryDTO;
import com.gate.mockexam.entity.GeminiTokenUsage;
import com.gate.mockexam.exception.QuotaExceededException;
import com.gate.mockexam.repository.GeminiTokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiUsageServiceImpl implements GeminiUsageService {

    private final GeminiTokenUsageRepository repository;

    @Value("${gemini.quota.daily-token-limit:500000}")
    private int dailyTokenLimit;

    @Value("${gemini.quota.warn-threshold:400000}")
    private int warnThreshold;

    @Override
    public int getDailyTokensUsed() {
        return repository.getSumTotalTokensByDate(LocalDate.now());
    }

    @Override
    public void checkDailyLimit() throws QuotaExceededException {
        int used = getDailyTokensUsed();
        if (used >= dailyTokenLimit) {
            log.error("Gemini daily token quota exceeded. Limit: {}, Current Usage: {}", dailyTokenLimit, used);
            throw new QuotaExceededException(
                "Daily Gemini token limit reached (" + (dailyTokenLimit / 1000) + "K tokens). " +
                "Resets at midnight. Current usage: " + used + " tokens."
            );
        }
    }

    @Override
    @Transactional
    public void recordUsage(String callType, int inputTokens, int outputTokens) {
        int total = inputTokens + outputTokens;
        GeminiTokenUsage usage = GeminiTokenUsage.builder()
                .usageDate(LocalDate.now())
                .callType(callType)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(total)
                .modelUsed("gemini-2.5-flash")
                .build();
        repository.save(usage);
        log.info("Recorded Gemini token usage for callType={}: input={}, output={}, total={}", 
                callType, inputTokens, outputTokens, total);
    }

    @Override
    public GeminiUsageSummaryDTO getDailySummary() {
        LocalDate today = LocalDate.now();
        int total = repository.getSumTotalTokensByDate(today);
        int input = repository.getSumInputTokensByDate(today);
        int output = repository.getSumOutputTokensByDate(today);

        double cost = (input * 0.0000003) + (output * 0.0000025);
        int remaining = Math.max(0, dailyTokenLimit - total);

        List<Object[]> rawCallTypes = repository.getUsageByCallTypeForDate(today);
        List<GeminiUsageSummaryDTO.CallTypeUsage> byCallType = new ArrayList<>();
        for (Object[] row : rawCallTypes) {
            String type = (String) row[0];
            int tokens = ((Number) row[1]).intValue();
            byCallType.add(new GeminiUsageSummaryDTO.CallTypeUsage(type, tokens));
        }

        return GeminiUsageSummaryDTO.builder()
                .date(today)
                .totalTokens(total)
                .inputTokens(input)
                .outputTokens(output)
                .estimatedCostUsd(cost)
                .limitTokens(dailyTokenLimit)
                .remainingTokens(remaining)
                .byCallType(byCallType)
                .build();
    }

    @Override
    public List<GeminiUsageHistoryDTO> getHistoricalUsage(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Object[]> rows = repository.getHistoricalUsage(startDate);
        List<GeminiUsageHistoryDTO> history = new ArrayList<>();

        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            int total = ((Number) row[1]).intValue();
            int input = ((Number) row[2]).intValue();
            int output = ((Number) row[3]).intValue();
            double cost = (input * 0.0000003) + (output * 0.0000025);

            history.add(GeminiUsageHistoryDTO.builder()
                    .date(date)
                    .totalTokens(total)
                    .estimatedCostUsd(cost)
                    .build());
        }
        return history;
    }

    @Override
    public GeminiTokenUsage getLastUsageRecord() {
        return repository.findFirstByOrderByCreatedAtDesc();
    }
}
