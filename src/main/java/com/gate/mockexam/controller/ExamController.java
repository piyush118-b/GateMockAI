package com.gate.mockexam.controller;

import com.gate.mockexam.service.MockTestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class ExamController {

    private final MockTestService mockTestService;

    public ExamController(MockTestService mockTestService) {
        this.mockTestService = mockTestService;
    }

    @GetMapping("/tests")
    public String listTests(Model model) {
        model.addAttribute("pageTitle", "Available Mock Exams");
        model.addAttribute("tests", mockTestService.getAllPublishedTests());
        return "student/tests";
    }
}
