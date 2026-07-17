package com.gate.mockexam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Application-level configuration.
 * Enables @Async for Pipeline 2 (AI Enrichment) async execution.
 */
@Configuration
@EnableAsync
public class AppConfig {

    /**
     * Thread pool for Pipeline 2 async enrichment tasks.
     * Core pool = 2 (keeps Gemini API concurrency low to avoid rate limits).
     * Max pool  = 5 for burst capacity.
     */
    @Bean(name = "pipelineExecutor")
    public Executor pipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pipeline-");
        executor.initialize();
        return executor;
    }
}
