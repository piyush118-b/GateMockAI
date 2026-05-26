package com.gate.mockexam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GateMockExamApplication {
    static {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                java.nio.file.Files.readAllLines(envPath).forEach(line -> {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        int index = line.indexOf("=");
                        String key = line.substring(0, index).trim();
                        String value = line.substring(index + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                });
            }
        } catch (Exception e) {
            // Ignore env loading errors in environments without local file access
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(GateMockExamApplication.class, args);
    }
}
