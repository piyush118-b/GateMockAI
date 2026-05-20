package com.gate.mockexam.controller;

import com.gate.mockexam.entity.Attempt;
import com.gate.mockexam.entity.User;
import com.gate.mockexam.repository.UserRepository;
import com.gate.mockexam.service.AttemptService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;
import java.util.List;

// @Controller
// @RequestMapping("/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final AttemptService attemptService;

    public DashboardController(UserRepository userRepository, AttemptService attemptService) {
        this.userRepository = userRepository;
        this.attemptService = attemptService;
    }

    @GetMapping
    public String studentDashboard(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Student Dashboard");

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
            
            List<Attempt> attempts = attemptService.getAttemptsByUser(user.getId());
            model.addAttribute("attempts", attempts);
            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("attemptsCount", attempts.size());
        } else {
            model.addAttribute("attempts", Collections.emptyList());
            model.addAttribute("fullName", "Student");
            model.addAttribute("attemptsCount", 0);
        }

        return "student/dashboard";
    }
}
