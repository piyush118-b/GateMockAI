package com.gate.mockexam.controller;

import com.gate.mockexam.dto.MockTestSummaryDto;
import com.gate.mockexam.service.MockTestService;
import com.gate.mockexam.service.RagIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MockTestService mockTestService;
    private final RagIngestionService ragIngestionService;

    @Value("${gate.rag.vector-store-path}")
    private String vectorStorePath;

    public AdminController(MockTestService mockTestService, RagIngestionService ragIngestionService) {
        this.mockTestService = mockTestService;
        this.ragIngestionService = ragIngestionService;
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

    @GetMapping("/tests/generate")
    public String showGenerateForm(Model model) {
        model.addAttribute("pageTitle", "AI Exam Generator");
        return "admin/generate";
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
