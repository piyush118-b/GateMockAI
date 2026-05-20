package com.gate.mockexam.controller;

import com.gate.mockexam.dto.MockTestGenerationRequestDto;
import com.gate.mockexam.dto.MockTestSummaryDto;
import com.gate.mockexam.entity.MockTest;
import com.gate.mockexam.service.MockTestGenerationService;
import com.gate.mockexam.service.MockTestService;
import com.gate.mockexam.service.RagIngestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MockTestService mockTestService;
    private final RagIngestionService ragIngestionService;
    private final MockTestGenerationService generationService;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    public AdminController(MockTestService mockTestService, 
                           RagIngestionService ragIngestionService, 
                           MockTestGenerationService generationService) {
        this.mockTestService = mockTestService;
        this.ragIngestionService = ragIngestionService;
        this.generationService = generationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        List<MockTestSummaryDto> tests = mockTestService.getAllTests();
        model.addAttribute("testsCount", tests.size());
        model.addAttribute("publishedCount", tests.stream().filter(MockTestSummaryDto::isPublished).count());
        
        // Expose RAG details to the template directly
        model.addAttribute("vectorCount", ragIngestionService.getVectorCount());
        model.addAttribute("storePath", vectorStorePath);
        
        return "admin/dashboard";
    }

    @GetMapping("/tests")
    public String viewTests(Model model) {
        model.addAttribute("pageTitle", "Manage Tests");
        model.addAttribute("tests", mockTestService.getAllTests());
        return "admin/tests";
    }

    // GET /admin/tests/generate — show the form
    @GetMapping("/tests/generate")
    public String generateForm(Model model) {
        model.addAttribute("pageTitle", "AI Exam Generator");
        model.addAttribute("request", new MockTestGenerationRequestDto());
        model.addAttribute("subjects", List.of(
            "Operating Systems", "DBMS", "Computer Networks",
            "Data Structures & Algorithms", "Theory of Computation",
            "Computer Organization", "Discrete Mathematics"
        ));
        return "admin/generate";
    }

    // POST /admin/tests/generate — trigger AI generation
    @PostMapping("/tests/generate")
    public String generate(@Valid @ModelAttribute("request") MockTestGenerationRequestDto request,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "AI Exam Generator");
            model.addAttribute("subjects", List.of(
                "Operating Systems", "DBMS", "Computer Networks",
                "Data Structures & Algorithms", "Theory of Computation",
                "Computer Organization", "Discrete Mathematics"
            ));
            return "admin/generate";
        }
        try {
            MockTest test = generationService.generateAndSaveTest(request);
            redirectAttributes.addFlashAttribute("success",
                "Test generated: " + test.getTitle() + " (" + test.getQuestions().size() + " questions)");
            return "redirect:/admin/tests/" + test.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Generation failed: " + e.getMessage());
            return "redirect:/admin/tests/generate";
        }
    }

    // GET /admin/tests/{id} — view generated test with all questions
    @GetMapping("/tests/{id}")
    public String viewTest(@PathVariable UUID id, Model model) {
        MockTest test = mockTestService.getTestById(id);
        if (test == null) {
            return "redirect:/admin/tests";
        }
        model.addAttribute("pageTitle", test.getTitle());
        model.addAttribute("test", test);
        return "admin/test-detail";
    }

    // POST /admin/tests/{id}/publish — make test visible to students
    @PostMapping("/tests/{id}/publish")
    public String publishTest(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            mockTestService.publishTest(id);
            ra.addFlashAttribute("success", "Test published successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Publish failed: " + e.getMessage());
        }
        return "redirect:/admin/tests/" + id;
    }

    // GET /admin/rag/status — returns count of embedded docs
    @GetMapping("/rag/status")
    @ResponseBody
    public Map<String, Object> ragStatus() {
        return Map.of(
            "vectorCount", ragIngestionService.getVectorCount(),
            "storePath", vectorStorePath
        );
    }

    // POST /admin/rag/reingest — force re-embed all seed questions
    @PostMapping("/rag/reingest")
    public String reingest(RedirectAttributes redirectAttributes) {
        try {
            int count = ragIngestionService.ingestSeedQuestions();
            redirectAttributes.addFlashAttribute("success", "Re-ingested " + count + " questions.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ingestion failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
