package com.gate.mockexam.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * v2.1 SPA Routing Controller.
 * Forwards all frontend UI routes to index.html so React Router (client-side)
 * can take over and render the appropriate page. Without this, direct navigation
 * or browser refresh on URLs like /admin/dashboard result in a 404 Whitelabel error.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/register",
        "/admin/**",
        "/student/**",
        "/exam/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
