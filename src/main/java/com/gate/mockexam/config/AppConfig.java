package com.gate.mockexam.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Bean
    public OllamaApi ollamaApi(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(600000);  // 10 minutes
        
        RestClient.Builder customizedBuilder = restClientBuilder.clone().requestFactory(factory);
        
        return OllamaApi.builder()
            .baseUrl(ollamaBaseUrl)
            .restClientBuilder(customizedBuilder)
            .build();
    }

    /**
     * Fixed thread pool for parallel Ollama chunk calls.
     * Size of 3 matches the Semaphore permit count in AdminRagController —
     * prevents VRAM OOM while still cutting ingestion time ~3x.
     */
    @Bean(name = "ollamaChunkExecutor", destroyMethod = "shutdown")
    public ExecutorService ollamaChunkExecutor() {
        return Executors.newFixedThreadPool(3);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are an expert GATE (Graduate Aptitude Test in Engineering) question setter with 15 years of experience.
                You create mathematically rigorous, conceptually deep questions that perfectly match GATE exam patterns.
                You ALWAYS respond with raw JSON only. No markdown. No explanation. No preamble.
                """)
            .build();
    }
}
