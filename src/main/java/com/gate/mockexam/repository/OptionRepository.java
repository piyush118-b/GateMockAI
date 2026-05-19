package com.gate.mockexam.repository;

import com.gate.mockexam.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OptionRepository extends JpaRepository<Option, UUID> {
    List<Option> findByQuestionIdOrderByOptionLabelAsc(UUID questionId);
}
