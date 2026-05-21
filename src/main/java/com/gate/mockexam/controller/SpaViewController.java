package com.gate.mockexam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaViewController {

    @GetMapping({
        "/student/**",
        "/admin/**",
        "/login",
        "/register"
    })
    public String forwardToSpa() {
        // Forward all SPA routes to the static index.html served by Spring Boot
        return "forward:/index.html";
    }
}
