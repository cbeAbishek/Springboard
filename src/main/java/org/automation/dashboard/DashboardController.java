package org.automation.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String redirectToDashboard() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/index";
    }

    @GetMapping("/dashboard/test-manager")
    public String testManager() {
        return "dashboard/test-manager";
    }

    @GetMapping("/dashboard/reports")
    public String reports() {
        return "dashboard/reports";
    }

    @GetMapping("/dashboard/execution-report")
    public String executionReport() {
        return "dashboard/execution-report";
    }
}

