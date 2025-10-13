package com.example.automatedtestingframework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * User controller for authentication flows.
 * Now using Clerk for authentication - traditional login/register removed.
 */
@Controller
public class UserController {

    /**
     * Landing page - shows Clerk sign-in component
     */
    @GetMapping("/")
    public String home(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "landing";
    }

    /**
     * Redirect legacy login page to landing
     */
    @GetMapping("/login")
    public String login() {
        return "redirect:/signin";
    }

    /**
     * Dedicated Clerk sign-in page
     */
    @GetMapping({"/signin", "/sign-in"})
    public String signIn() {
        return "signin";
    }

    /**
     * Register page - shows Clerk sign-up component
     */
    @GetMapping("/register")
    public String register(Model model) {
        return "register";
    }

    /**
     * Documentation page
     */
    @GetMapping({"/docs", "/documentation"})
    public String documentation() {
        return "documentation";
    }
}
