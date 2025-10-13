package com.example.automatedtestingframework.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {

    @GetMapping("/demo/login")
    public String demoLogin() {
        return "demo/login";
    }
}
