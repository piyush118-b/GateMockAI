package com.gate.mockexam.service;

import com.gate.mockexam.dto.GeminiUsageSummaryDTO;
import com.gate.mockexam.entity.GeminiTokenUsage;
import com.gate.mockexam.exception.QuotaExceededException;
import com.gate.mockexam.repository.GeminiTokenUsageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class GeminiUsageServiceTest {

    @Autowired
    private GeminiUsageService geminiUsageService;

    @MockBean
    private GeminiTokenUsageRepository repository;

    @MockBean
    private org.springframework.ai.embedding.EmbeddingModel embeddingModel;

    @MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void testGetDailyTokensUsed() {
        when(repository.getSumTotalTokensByDate(any(LocalDate.class))).thenReturn(25000);
        
        int used = geminiUsageService.getDailyTokensUsed();
        assertThat(used).isEqualTo(25000);
    }

    @Test
    public void testCheckDailyLimitUnderQuota() {
        when(repository.getSumTotalTokensByDate(any(LocalDate.class))).thenReturn(450000);
        
        // Should not throw exception
        geminiUsageService.checkDailyLimit();
    }

    @Test
    public void testCheckDailyLimitExceeded() {
        when(repository.getSumTotalTokensByDate(any(LocalDate.class))).thenReturn(505000);
        
        assertThatThrownBy(() -> geminiUsageService.checkDailyLimit())
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("Daily Gemini token limit reached");
    }

    @Test
    public void testGetDailySummary() {
        when(repository.getSumTotalTokensByDate(any(LocalDate.class))).thenReturn(300000);
        when(repository.getSumInputTokensByDate(any(LocalDate.class))).thenReturn(200000);
        when(repository.getSumOutputTokensByDate(any(LocalDate.class))).thenReturn(100000);
        
        Object[] msqRow = new Object[]{"GENERATION", 180000};
        Object[] ingestionRow = new Object[]{"INGESTION", 120000};
        when(repository.getUsageByCallTypeForDate(any(LocalDate.class))).thenReturn(List.of(msqRow, ingestionRow));

        GeminiUsageSummaryDTO summary = geminiUsageService.getDailySummary();
        
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalTokens()).isEqualTo(300000);
        assertThat(summary.getInputTokens()).isEqualTo(200000);
        assertThat(summary.getOutputTokens()).isEqualTo(100000);
        
        // Estimated Cost: 200,000 * 0.0000003 + 100,000 * 0.0000025 = 0.06 + 0.25 = 0.31
        assertThat(summary.getEstimatedCostUsd()).isEqualTo(0.31);
        assertThat(summary.getRemainingTokens()).isEqualTo(200000); // 500k limit - 300k used
        
        assertThat(summary.getByCallType()).hasSize(2);
        assertThat(summary.getByCallType().get(0).getCallType()).isEqualTo("GENERATION");
        assertThat(summary.getByCallType().get(0).getTokens()).isEqualTo(180000);
    }
}
