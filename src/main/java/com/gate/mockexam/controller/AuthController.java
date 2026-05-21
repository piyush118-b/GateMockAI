package com.gate.mockexam.controller;

import com.gate.mockexam.dto.UserRegistrationDto;
import com.gate.mockexam.entity.User;
import com.gate.mockexam.enums.UserRole;
import com.gate.mockexam.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /api/register
     * React SPA submits registration data as JSON, receives JSON error or success.
     */
    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto,
            BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = new HashMap<>();
            for (FieldError fe : bindingResult.getFieldErrors()) {
                fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            }
            response.put("status", "error");
            response.put("fieldErrors", fieldErrors);
            return ResponseEntity.badRequest().body(response);
        }

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            response.put("status", "error");
            response.put("fieldErrors", Map.of("confirmPassword", "Passwords do not match"));
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            response.put("status", "error");
            response.put("fieldErrors", Map.of("email", "An account already exists with this email"));
            return ResponseEntity.badRequest().body(response);
        }

        User user = User.builder()
                .email(registrationDto.getEmail())
                .fullName(registrationDto.getFullName())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .role(UserRole.STUDENT)
                .build();

        userRepository.save(user);

        response.put("status", "success");
        response.put("message", "Account created successfully. Please log in.");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /home — Server-side redirect logic based on authenticated role.
     * Called by Spring Security's success handler redirect internally.
     */
    @GetMapping("/home")
    public ResponseEntity<?> homeRedirect(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(302).header("Location", "/login").build();
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        String redirectPath = isAdmin ? "/admin/dashboard" : isStudent ? "/student/tests" : "/login";
        return ResponseEntity.status(302).header("Location", redirectPath).build();
    }
}
