package com.gate.mockexam.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

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
