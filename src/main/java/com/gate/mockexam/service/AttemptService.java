package com.gate.mockexam.service;

import com.gate.mockexam.entity.Attempt;
import com.gate.mockexam.repository.AttemptRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class AttemptService {

    private final AttemptRepository attemptRepository;

    public AttemptService(AttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public List<Attempt> getAttemptsByUser(UUID userId) {
        return attemptRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    public Attempt getAttemptById(UUID id) {
        return attemptRepository.findById(id).orElse(null);
    }
}
