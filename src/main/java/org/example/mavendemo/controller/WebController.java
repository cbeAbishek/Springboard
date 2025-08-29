package org.example.mavendemo.controller;

import org.example.mavendemo.model.TestCase;
import org.example.mavendemo.model.TestResult;
import org.example.mavendemo.repository.TestCaseRepository;
import org.example.mavendemo.repository.TestResultRepository;
import org.example.mavendemo.service.TestCaseService;
import org.example.mavendemo.service.TestExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/ui")
public class WebController {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private TestExecutionService testExecutionService;

    // Dashboard - Main page
    @GetMapping("/")
    public String dashboard(Model model) {
        List<TestCase> testCases = testCaseService.getAllTestCases();
        List<TestResult> recentResults = testResultRepository.findTop10ByOrderByCreatedAtDesc();

        // Dashboard statistics
        long totalTests = testCases.size();
        long activeTests = testCases.stream().filter(tc -> "Active".equals(tc.getStatus())).count();
        long passedTests = recentResults.stream().filter(tr -> "PASSED".equals(tr.getStatus())).count();
        long failedTests = recentResults.stream().filter(tr -> "FAILED".equals(tr.getStatus())).count();

        model.addAttribute("totalTests", totalTests);
        model.addAttribute("activeTests", activeTests);
        model.addAttribute("passedTests", passedTests);
        model.addAttribute("failedTests", failedTests);
        model.addAttribute("testCases", testCases);
        model.addAttribute("recentResults", recentResults);
        model.addAttribute("currentPath", "/ui/");

        return "dashboard-clean";
    }

    // Test Cases Management
    @GetMapping("/testcases")
    public String testCases(Model model) {
        List<TestCase> testCases = testCaseRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        model.addAttribute("testCases", testCases);
        model.addAttribute("currentPath", "/ui/testcases");
        return "testcases-clean";
    }

    @GetMapping("/testcases/new")
    public String newTestCase(Model model) {
        model.addAttribute("testCase", new TestCase());
        model.addAttribute("currentPath", "/ui/testcases");
        return "testcase-form-clean";
    }

    @GetMapping("/testcases/edit/{id}")
    public String editTestCase(@PathVariable Long id, Model model) {
        Optional<TestCase> testCase = testCaseService.getTestCaseById(id);
        if (testCase.isPresent()) {
            model.addAttribute("testCase", testCase.get());
            model.addAttribute("currentPath", "/ui/testcases");
            return "testcase-form-clean";
        }
        return "redirect:/ui/testcases";
    }

    @PostMapping("/testcases/save")
    public String saveTestCase(@ModelAttribute TestCase testCase, RedirectAttributes redirectAttributes) {
        try {
            testCaseService.saveTestCase(testCase);
            redirectAttributes.addFlashAttribute("success", "Test case saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving test case: " + e.getMessage());
        }
        return "redirect:/ui/testcases";
    }

    @GetMapping("/testcases/delete/{id}")
    public String deleteTestCase(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            testCaseService.deleteTestCase(id);
            redirectAttributes.addFlashAttribute("success", "Test case deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting test case: " + e.getMessage());
        }
        return "redirect:/ui/testcases";
    }

    @GetMapping("/testcases/view/{id}")
    public String viewTestCase(@PathVariable Long id, Model model) {
        Optional<TestCase> testCase = testCaseService.getTestCaseById(id);
        if (testCase.isPresent()) {
            List<TestResult> results = testResultRepository.findByTestCaseIdOrderByCreatedAtDesc(id);
            model.addAttribute("testCase", testCase.get());
            model.addAttribute("results", results);
            model.addAttribute("currentPath", "/ui/testcases");
            return "testcase-view-clean";
        }
        return "redirect:/ui/testcases";
    }

    // Test Execution
    @PostMapping("/execute/{id}")
    @ResponseBody
    public String executeTest(@PathVariable Long id) {
        try {
            testExecutionService.executeTest(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // Test Results Management
    @GetMapping("/results")
    public String testResults(Model model) {
        List<TestResult> results = testResultRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("results", results);
        model.addAttribute("currentPath", "/ui/results");
        return "test-results-clean";
    }

    @GetMapping("/results/view/{id}")
    public String viewTestResult(@PathVariable Long id, Model model) {
        Optional<TestResult> result = testResultRepository.findById(id);
        if (result.isPresent()) {
            model.addAttribute("result", result.get());
            model.addAttribute("currentPath", "/ui/results");
            return "result-view-clean";
        }
        return "redirect:/ui/results";
    }

    // Analytics
    @GetMapping("/analytics")
    public String analytics(Model model) {
        List<TestResult> allResults = testResultRepository.findAll();

        // Calculate analytics
        long totalExecutions = allResults.size();
        long passedExecutions = allResults.stream().filter(tr -> "PASSED".equals(tr.getStatus())).count();
        long failedExecutions = allResults.stream().filter(tr -> "FAILED".equals(tr.getStatus())).count();
        double passRate = totalExecutions > 0 ? (double) passedExecutions / totalExecutions * 100 : 0;

        model.addAttribute("totalExecutions", totalExecutions);
        model.addAttribute("passedExecutions", passedExecutions);
        model.addAttribute("failedExecutions", failedExecutions);
        model.addAttribute("passRate", String.format("%.1f", passRate));
        model.addAttribute("allResults", allResults);
        model.addAttribute("currentPath", "/ui/analytics");

        return "analytics-clean";
    }
}
