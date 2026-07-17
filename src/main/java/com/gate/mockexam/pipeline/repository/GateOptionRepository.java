package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.GateOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GateOptionRepository extends JpaRepository<GateOption, String> {

    List<GateOption> findByQuestionQuestionIdOrderByDisplayOrderAsc(String questionId);
}
