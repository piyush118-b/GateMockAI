package com.gate.mockexam.config;

import com.gate.mockexam.entity.User;
import com.gate.mockexam.enums.UserRole;
import com.gate.mockexam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Startup runner that seeds the default admin and student users
 * if they do not already exist in the database.
 *
 * RAG ingestion has been removed — question ingestion is now handled
 * by Pipeline 1 (ExtractionPipelineService) via the /api/pipeline/ingest endpoint.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppStartupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin@gate.com",   "Admin@123",   UserRole.ADMIN,   "GATE Admin");
        seedUser("student@gate.com", "Student@123", UserRole.STUDENT, "GATE Student");
        log.info("GATE Question Intelligence Platform started successfully.");
        log.info("Pipeline API available at: POST /api/pipeline/ingest");
    }

    private void seedUser(String email, String password, UserRole role, String fullName) {
        if (!userRepository.existsByEmail(email)) {
            log.info("Seeding default user: {} ({})", email, role);
            User user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .build();
            userRepository.save(user);
        }
    }
}
