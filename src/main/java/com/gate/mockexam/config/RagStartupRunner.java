package com.gate.mockexam.config;

import com.gate.mockexam.service.RagIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
@RequiredArgsConstructor
public class RagStartupRunner implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;

    @Value("${gate.rag.auto-ingest-on-startup}")
    private boolean autoIngest;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    @Override
    public void run(String... args) throws Exception {
        File storeFile = new File(vectorStorePath);
        if (autoIngest && !storeFile.exists()) {
            log.info("Vector store not found at {}. Running initial RAG ingestion...", vectorStorePath);
            try {
                int count = ragIngestionService.ingestSeedQuestions();
                log.info("RAG ingestion complete. {} documents embedded and persisted.", count);
            } catch (Exception e) {
                log.error("Failed to perform initial RAG ingestion: {}", e.getMessage(), e);
            }
        } else {
            log.info("Vector store already exists at {}. Skipping auto-ingestion on startup.", vectorStorePath);
        }
    }
}
