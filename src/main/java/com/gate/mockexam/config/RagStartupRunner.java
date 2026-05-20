package com.gate.mockexam.config;

import com.gate.mockexam.entity.User;
import com.gate.mockexam.enums.UserRole;
import com.gate.mockexam.repository.UserRepository;
import com.gate.mockexam.service.RagIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
@RequiredArgsConstructor
public class RagStartupRunner implements CommandLineRunner {

    private final RagIngestionService ragIngestionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${gate.rag.auto-ingest-on-startup}")
    private boolean autoIngest;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    @Override
    public void run(String... args) throws Exception {
        // Programmatically seed student account if not present
        if (!userRepository.existsByEmail("student@gate.com")) {
            log.info("Seeding default student user: student@gate.com");
            User student = User.builder()
                    .email("student@gate.com")
                    .fullName("GATE Student")
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .role(UserRole.STUDENT)
                    .build();
            userRepository.save(student);
        }

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

