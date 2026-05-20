package com.gate.mockexam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaViewController {

    @GetMapping({
        "/student/**",
        "/admin/**"
    })
    public String forwardToSpa() {
        // Forward to the static index.html served directly by Spring Boot
        return "forward:/index.html";
    }
}
