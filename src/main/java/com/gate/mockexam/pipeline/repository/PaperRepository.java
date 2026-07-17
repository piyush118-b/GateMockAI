package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, String> {

    Optional<Paper> findByExamNameAndYearAndBranch(String examName, int year, String branch);

    List<Paper> findAllByOrderByYearDesc();

    List<Paper> findByStatus(String status);

    boolean existsByExamNameAndYearAndBranch(String examName, int year, String branch);
}
