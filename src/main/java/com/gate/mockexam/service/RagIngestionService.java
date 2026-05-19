package com.gate.mockexam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.SeedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagIngestionService {

    private final SimpleVectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    @Value("${gate.rag.seed-questions-path}")
    private Resource seedQuestionsResource;

    /**
     * Loads seed questions from JSON, converts each to a Spring AI Document,
     * adds to the vector store in batches of 10 to avoid OOM or timeouts, then persists to disk.
     */
    public int ingestSeedQuestions() throws IOException {
        log.info("Loading seed questions from resource: {}", seedQuestionsResource.getFilename());
        List<SeedQuestion> questions = objectMapper.readValue(
            seedQuestionsResource.getInputStream(),
            new TypeReference<List<SeedQuestion>>() {}
        );

        List<Document> documents = questions.stream().map(q -> {
            // Build a rich text representation that embeds well
            String content = String.format(
                "Subject: %s | Topic: %s | Type: %s\nQuestion: %s\nExplanation: %s\nTags: %s",
                q.getSubject(), q.getTopic(), q.getType(),
                q.getQuestionText(),
                q.getExplanation() != null ? q.getExplanation() : "",
                q.getTags() != null ? String.join(", ", q.getTags()) : ""
            );
            Map<String, Object> metadata = Map.of(
                "id", q.getId(),
                "topic", q.getTopic(),
                "subject", q.getSubject(),
                "type", q.getType()
            );
            return new Document(content, metadata);
        }).toList();

        // Batch in groups of 10 to prevent OOM/timeouts during Gemini API calls
        int batchSize = 10;
        int totalIngested = 0;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
            totalIngested += batch.size();
            log.info("Successfully embedded and added batch: {} to {} / Total: {}", i, i + batch.size(), documents.size());
        }

        // Save the updated store to disk
        File saveFile = new File(vectorStorePath);
        File parentDir = saveFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        vectorStore.save(saveFile);
        log.info("Successfully persisted vector store to disk: {}", vectorStorePath);

        return totalIngested;
    }

    /**
     * Semantic similarity search — returns top-k most relevant past questions for a topic.
     */
    public List<Document> retrieveSimilarQuestions(String topic, int topK) {
        log.info("Performing similarity search for query: '{}' with topK: {}", topic, topK);
        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(topic)
                .topK(topK)
                .build()
        );
    }

    /**
     * Returns count of embedded docs by searching with a wildcard/common exam query.
     */
    public int getVectorCount() {
        try {
            // SimpleVectorStore doesn't expose count directly; track via metadata
            return vectorStore.similaritySearch(
                SearchRequest.builder().query("GATE exam").topK(1000).build()
            ).size();
        } catch (Exception e) {
            log.error("Failed to retrieve vector count: {}", e.getMessage());
            return 0;
        }
    }
}
