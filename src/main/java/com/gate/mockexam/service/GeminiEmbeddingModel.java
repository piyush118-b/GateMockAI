package com.gate.mockexam.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class GeminiEmbeddingModel implements EmbeddingModel {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String EMBED_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("${gemini.api.key}")) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }
        if (apiKey != null) {
            apiKey = apiKey.trim();
        }
        String url = EMBED_URL + "?key=" + apiKey;

        Map<String, Object> body = Map.of(
            "model", "models/gemini-embedding-001",
            "content", Map.of("parts", List.of(Map.of("text", text))),
            "outputDimensionality", 768
        );

        try {
            Map<?, ?> response = restTemplate.postForObject(url, body, Map.class);
            if (response != null && response.containsKey("embedding")) {
                Map<?, ?> embedding = (Map<?, ?>) response.get("embedding");
                List<?> values = (List<?>) embedding.get("values");
                float[] result = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    result[i] = ((Number) values.get(i)).floatValue();
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to embed text using Gemini embedding API", e);
        }
        throw new RuntimeException("Empty response from Gemini embedding API");
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            float[] vec = embed(instructions.get(i));
            embeddings.add(new Embedding(vec, i));
        }
        return new EmbeddingResponse(embeddings);
    }
}
