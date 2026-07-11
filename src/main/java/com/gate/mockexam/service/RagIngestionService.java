package com.gate.mockexam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gate.mockexam.dto.SeedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gate.mockexam.service.GeminiService;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    @Value("${gate.rag.seed-questions-path}")
    private String seedQuestionsPath;

    @Value("${gate.rag.seed-questions-path}")
    private Resource seedQuestionsResource;

    /** TRACK 3: Toggle HyDE + multi-query. Set to false for bare-topic fallback. */
    @Value("${hyde.enabled:true}")
    private boolean hydeEnabled;

    // ─────────────────────────────────────────────────────────────────────────
    // Ingestion
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads seed questions from JSON, converts each to a Spring AI Document,
     * adds to the vector store in batches of 10.
     */
    public int ingestSeedQuestions() throws IOException {
        log.info("Truncating vector store table to clear out old embeddings...");
        try {
            jdbcTemplate.execute("TRUNCATE TABLE gate_vector_store");
        } catch (Exception e) {
            log.warn("Failed to truncate table gate_vector_store: {}", e.getMessage());
        }

        log.info("Loading seed questions from resource: {}", seedQuestionsResource.getFilename());
        List<SeedQuestion> questions = objectMapper.readValue(
            seedQuestionsResource.getInputStream(),
            new TypeReference<List<SeedQuestion>>() {}
        );

        List<Document> documents = questions.stream().map(q -> {
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

        int batchSize = 10;
        int totalIngested = 0;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
            totalIngested += batch.size();
            log.info("Successfully embedded and added batch: {} to {} / Total: {}", i, i + batch.size(), documents.size());
        }

        log.info("Successfully persisted vector store elements directly into PGVector store.");
        return totalIngested;
    }

    /**
     * Ingestion support for dynamic document chunking (used in past paper uploading).
     */
    public void ingestDocumentChunks(List<Document> chunks) {
        log.info("Ingesting {} dynamic document chunks into PGVector store", chunks.size());
        int batchSize = 10;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<Document> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            vectorStore.add(batch);
            log.info("Added batch of dynamic chunks: {} to {} / Total: {}", i, i + batch.size(), chunks.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRACK 2 + TRACK 3: Retrieval
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TRACK 2 + TRACK 3 — upgraded retrieval entry point.
     *
     * When {@code hyde.enabled=true}:
     *   1. HyDE: generate a hypothetical GATE question for the topic → embed the response.
     *   2. Multi-query: generate 3 topic paraphrases → retrieve top-3 each → union-deduplicate.
     *   3. Return the union, capped at {@code topK} (default 8 in generation calls).
     *
     * When {@code hyde.enabled=false}: falls back to the original bare-topic search.
     *
     * TRACK 2: sets {@code hnsw.ef_search = 64} before every similarity search so the
     * HNSW index uses a larger candidate list for higher recall.
     */
    public List<Document> retrieveSimilarQuestions(String topic, int topK) {
        log.info("Performing retrieval for topic: '{}' topK: {} hydeEnabled: {}", topic, topK, hydeEnabled);

        if (!hydeEnabled) {
            return bareSearch(topic, topK);
        }

        // ── Step 1: HyDE ────────────────────────────────────────────────────
        String hydeQuery = generateHydeQuery(topic);
        log.debug("HyDE query: {}", hydeQuery);

        // ── Step 2: Multi-query paraphrases ──────────────────────────────────
        List<String> paraphrases = generateParaphrases(topic);
        log.debug("Generated {} paraphrases for topic '{}'", paraphrases.size(), topic);

        // ── Step 3: Retrieve for HyDE + each paraphrase, then deduplicate ────
        Set<String> seenContent = new LinkedHashSet<>();
        List<Document> union = new ArrayList<>();

        // HyDE results (top topK)
        for (Document d : bareSearch(hydeQuery, topK)) {
            if (seenContent.add(d.getText())) union.add(d);
        }

        // Paraphrase results (top 3 each)
        for (String para : paraphrases) {
            for (Document d : bareSearch(para, 3)) {
                if (seenContent.add(d.getText())) union.add(d);
            }
        }

        // Cap at topK
        List<Document> result = union.stream().limit(topK).collect(Collectors.toList());
        log.info("Multi-query fusion returned {} unique documents (cap={})", result.size(), topK);
        return result;
    }

    /**
     * Returns count of embedded docs in PGVector.
     */
    public int getVectorCount() {
        try {
            return vectorStore.similaritySearch(
                SearchRequest.builder().query("GATE exam").topK(1000).build()
            ).size();
        } catch (Exception e) {
            log.error("Failed to retrieve vector count: {}", e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TRACK 2: Runs a single cosine similarity search, first setting hnsw.ef_search=64
     * so the HNSW index uses an extended candidate list for higher recall.
     * Degrades gracefully if the index or SET command is unavailable.
     */
    private List<Document> bareSearch(String query, int topK) {
        try {
            jdbcTemplate.execute("SET LOCAL hnsw.ef_search = 64");
        } catch (Exception e) {
            log.debug("Could not set hnsw.ef_search (index may not exist yet): {}", e.getMessage());
        }
        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build()
        );
    }

    /**
     * TRACK 3 — HyDE: calls Ollama to write a sample GATE question for the topic.
     * The response is richer and more domain-specific than the raw topic string,
     * so its embedding lands closer to real stored questions in vector space.
     * Falls back to the raw topic on any error.
     */
    private String generateHydeQuery(String topic) {
        try {
            String prompt = String.format(
                "Write a sample GATE exam question about %s at 2-mark MCQ difficulty. " +
                "Include the question stem and four plausible answer options (A, B, C, D). " +
                "Return only the question text and options, no explanation.",
                topic);
            String response = geminiService.generateContent(prompt);
            return response != null && !response.isBlank() ? response.trim() : topic;
        } catch (Exception e) {
            log.warn("HyDE query generation failed for topic '{}': {} — using raw topic", topic, e.getMessage());
            return topic;
        }
    }

    /**
     * TRACK 3 — Multi-query: generates 3 paraphrased topic strings via Ollama.
     * Falls back to a single bare topic entry on any error.
     */
    private List<String> generateParaphrases(String topic) {
        try {
            String prompt = String.format(
                "Generate exactly 3 different paraphrases of this GATE exam topic for a semantic search query. " +
                "Output each paraphrase on its own line, no numbering, no explanation: %s",
                topic);
            String response = geminiService.generateContent(prompt);
            if (response == null || response.isBlank()) return List.of(topic);

            List<String> lines = Arrays.stream(response.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(l -> !l.isBlank())
                    .limit(3)
                    .collect(Collectors.toList());
            return lines.isEmpty() ? List.of(topic) : lines;
        } catch (Exception e) {
            log.warn("Paraphrase generation failed for topic '{}': {} — using raw topic only", topic, e.getMessage());
            return List.of(topic);
        }
    }
}
