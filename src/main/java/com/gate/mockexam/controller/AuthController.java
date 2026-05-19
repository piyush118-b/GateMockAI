package com.gate.mockexam.controller;

import com.gate.mockexam.dto.UserRegistrationDto;
import com.gate.mockexam.entity.User;
import com.gate.mockexam.enums.UserRole;
import com.gate.mockexam.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }
        model.addAttribute("userDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userDto") UserRegistrationDto registrationDto,
                               BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
            return "auth/register";
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "An account already exists with this email");
            return "auth/register";
        }

        User user = User.builder()
                .email(registrationDto.getEmail())
                .fullName(registrationDto.getFullName())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .role(UserRole.STUDENT)
                .build();

        userRepository.save(user);

        return "redirect:/login?registered=true";
    }

    @GetMapping("/home")
    public String homeRedirect(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isStudent) {
            return "redirect:/student/tests";
        }

        return "redirect:/login";
    }
}
