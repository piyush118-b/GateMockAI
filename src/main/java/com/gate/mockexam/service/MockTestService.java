package com.gate.mockexam.service;

import com.gate.mockexam.dto.MockTestSummaryDto;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.repository.AttemptRepository;
import com.gate.mockexam.repository.MockTestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MockTestService {

    private final MockTestRepository mockTestRepository;
    private final AttemptRepository attemptRepository;

    public MockTestService(MockTestRepository mockTestRepository, AttemptRepository attemptRepository) {
        this.mockTestRepository = mockTestRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<MockTestSummaryDto> getAllPublishedTests() {
        return mockTestRepository.findByIsPublished(true).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public List<MockTestSummaryDto> getAllTests() {
        return mockTestRepository.findAll().stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public MockTest getTestById(UUID id) {
        return mockTestRepository.findById(id).orElse(null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void publishTest(UUID id) {
        MockTest test = mockTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
        test.setPublished(true);
        mockTestRepository.save(test);
    }

    @org.springframework.transaction.annotation.Transactional
    public void unpublishTest(UUID id) {
        MockTest test = mockTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
        test.setPublished(false);
        mockTestRepository.save(test);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteTest(UUID id) {
        MockTest test = mockTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
        attemptRepository.deleteByTestId(id);
        mockTestRepository.delete(test);
    }

    private MockTestSummaryDto mapToSummary(MockTest test) {
        return MockTestSummaryDto.builder()
                .id(test.getId())
                .title(test.getTitle())
                .topic(test.getTopic())
                .subject(test.getSubject())
                .durationMinutes(test.getDurationMinutes())
                .totalQuestions(test.getQuestions() != null ? test.getQuestions().size() : 0)
                .totalMarks(test.getTotalMarks())
                .isPublished(test.isPublished())
                .build();
    }
}
