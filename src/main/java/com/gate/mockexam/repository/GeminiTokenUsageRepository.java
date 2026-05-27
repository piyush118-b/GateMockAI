package com.gate.mockexam.repository;

import com.gate.mockexam.entity.GeminiTokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GeminiTokenUsageRepository extends JpaRepository<GeminiTokenUsage, UUID> {

    @Query("SELECT COALESCE(SUM(g.totalTokens), 0) FROM GeminiTokenUsage g WHERE g.usageDate = :date")
    int getSumTotalTokensByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(g.inputTokens), 0) FROM GeminiTokenUsage g WHERE g.usageDate = :date")
    int getSumInputTokensByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(g.outputTokens), 0) FROM GeminiTokenUsage g WHERE g.usageDate = :date")
    int getSumOutputTokensByDate(@Param("date") LocalDate date);

    @Query("SELECT g.callType, COALESCE(SUM(g.totalTokens), 0) FROM GeminiTokenUsage g WHERE g.usageDate = :date GROUP BY g.callType")
    List<Object[]> getUsageByCallTypeForDate(@Param("date") LocalDate date);

    @Query("SELECT g.usageDate, COALESCE(SUM(g.totalTokens), 0), COALESCE(SUM(g.inputTokens), 0), COALESCE(SUM(g.outputTokens), 0) " +
           "FROM GeminiTokenUsage g " +
           "WHERE g.usageDate >= :startDate " +
           "GROUP BY g.usageDate " +
           "ORDER BY g.usageDate DESC")
    List<Object[]> getHistoricalUsage(@Param("startDate") LocalDate startDate);

    GeminiTokenUsage findFirstByOrderByCreatedAtDesc();
}
