package com.gate.mockexam.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = new File(vectorStorePath);
        if (storeFile.exists()) {
            try {
                store.load(storeFile);
                log.info("Loaded existing vector store from {}", vectorStorePath);
            } catch (Exception e) {
                log.error("Failed to load existing vector store from {}: {}", vectorStorePath, e.getMessage(), e);
            }
        } else {
            log.info("No existing vector store found at {}. A new one will be created upon ingestion.", vectorStorePath);
        }
        return store;
    }
}
