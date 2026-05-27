package com.gate.mockexam.service;

import com.gate.mockexam.dto.GeminiUsageSummaryDTO;
import com.gate.mockexam.dto.GeminiUsageHistoryDTO;
import com.gate.mockexam.entity.GeminiTokenUsage;
import com.gate.mockexam.exception.QuotaExceededException;

import java.util.List;

public interface GeminiUsageService {
    int getDailyTokensUsed();
    void checkDailyLimit() throws QuotaExceededException;
    void recordUsage(String callType, int inputTokens, int outputTokens);
    GeminiUsageSummaryDTO getDailySummary();
    List<GeminiUsageHistoryDTO> getHistoricalUsage(int days);
    GeminiTokenUsage getLastUsageRecord();
}
