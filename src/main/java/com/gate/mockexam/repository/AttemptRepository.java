package com.gate.mockexam.repository;

import com.gate.mockexam.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    List<Attempt> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<Attempt> findByUserIdAndTestIdOrderByStartedAtDesc(UUID userId, UUID testId);
}
