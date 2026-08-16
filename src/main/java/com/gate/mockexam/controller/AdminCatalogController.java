package com.gate.mockexam.controller;

import com.gate.mockexam.dto.BranchCatalogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Serves the static GATE branch/subject catalog for the admin UI.
 * This is static configuration data — no database involved.
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminCatalogController {

    /**
     * GET /api/admin/branches
     *
     * Returns the GATE CSE branch catalog with subjects and default weightages.
     * This matches what WeightedGenerator.jsx expects on mount.
     */
    @GetMapping("/branches")
    public ResponseEntity<?> getBranches(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        BranchCatalogResponse cseBranch = BranchCatalogResponse.builder()
                .id("cse")
                .name("Computer Science and Information Technology")
                .code("CS")
                .subjects(List.of(
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("ga").name("General Aptitude").defaultMarksWeightage(15).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("em").name("Engineering Mathematics").defaultMarksWeightage(13).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("ds").name("Data Structures and Algorithms").defaultMarksWeightage(9).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("co").name("Computer Organization and Architecture").defaultMarksWeightage(5).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("os").name("Operating Systems").defaultMarksWeightage(5).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("dbms").name("Database Management Systems").defaultMarksWeightage(5).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("toc").name("Theory of Computation").defaultMarksWeightage(5).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("cn").name("Computer Networks").defaultMarksWeightage(5).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("se").name("Software Engineering").defaultMarksWeightage(3).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("comp").name("Compilers").defaultMarksWeightage(3).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("ai").name("Artificial Intelligence").defaultMarksWeightage(3).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("dig").name("Digital Logic").defaultMarksWeightage(3).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("is").name("Information Systems").defaultMarksWeightage(1).build(),
                        BranchCatalogResponse.SubjectDto.builder()
                                .id("web").name("Web Technologies").defaultMarksWeightage(1).build()
                ))
                .build();

        return ResponseEntity.ok(List.of(cseBranch));
    }
}
