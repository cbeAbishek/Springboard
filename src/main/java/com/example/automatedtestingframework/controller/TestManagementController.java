package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.analysis.EndpointAnalysisPayload;
import com.example.automatedtestingframework.model.EndpointAnalysisResult;
import com.example.automatedtestingframework.model.GeneratedActionFile;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.model.TestCaseType;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.GeneratedActionFileRepository;
import com.example.automatedtestingframework.repository.EndpointAnalysisResultRepository;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.TestCaseRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import com.example.automatedtestingframework.service.EndpointAnalysisService;
import com.example.automatedtestingframework.service.SchedulingService;
import com.example.automatedtestingframework.util.JsonParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class TestManagementController {

    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final GeneratedActionFileRepository generatedActionFileRepository;
    private final EndpointAnalysisResultRepository endpointAnalysisResultRepository;
    private final UserRepository userRepository;
    private final JsonParserUtil jsonParserUtil;
    private final SchedulingService schedulingService;
    private final EndpointAnalysisService endpointAnalysisService;

    private static final Logger logger = LoggerFactory.getLogger(TestManagementController.class);

    public TestManagementController(TestCaseRepository testCaseRepository,
                                    ProjectRepository projectRepository,
                                    GeneratedActionFileRepository generatedActionFileRepository,
                                    EndpointAnalysisResultRepository endpointAnalysisResultRepository,
                                    UserRepository userRepository,
                                    JsonParserUtil jsonParserUtil,
                                    SchedulingService schedulingService,
                                    EndpointAnalysisService endpointAnalysisService) {
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.generatedActionFileRepository = generatedActionFileRepository;
        this.endpointAnalysisResultRepository = endpointAnalysisResultRepository;
        this.userRepository = userRepository;
        this.jsonParserUtil = jsonParserUtil;
        this.schedulingService = schedulingService;
        this.endpointAnalysisService = endpointAnalysisService;
    }

    @GetMapping("/test-management")
    public String manageTests(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam(name = "projectId", required = false) Long projectId,
                              Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        List<Project> projects = projectRepository.findByOwner(user);
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(projects.isEmpty() ? null : projects.get(0)) : (projects.isEmpty() ? null : projects.get(0));
        if (project != null) {
            List<TestCase> tests = testCaseRepository.findByProject(project);
            model.addAttribute("tests", tests);

            Optional<TestCase> lastRun = tests.stream()
                .filter(tc -> tc.getLastRunAt() != null)
                .max(Comparator.comparing(TestCase::getLastRunAt));

            model.addAttribute("lastRunAt", lastRun.map(TestCase::getLastRunAt).orElse(null));
            model.addAttribute("recentStatus", lastRun.map(TestCase::getLastRunStatus).orElse(null));
            model.addAttribute("actions", generatedActionFileRepository.findByProject(project));
            model.addAttribute("project", project);

            List<EndpointAnalysisResult> analyses = endpointAnalysisResultRepository.findTop10ByProjectOrderByExecutedAtDesc(project);
            model.addAttribute("analysisResults", analyses.stream()
                .map(this::toView)
                .toList());
        }
        model.addAttribute("projects", projects);
        model.addAttribute("types", TestCaseType.values());
        return "test-management";
    }

    @PostMapping("/test-management/test")
    public String createTest(@AuthenticationPrincipal UserDetails principal,
                             @RequestParam Long projectId,
                             @RequestParam String name,
                             @RequestParam TestCaseType type,
                             @RequestParam String definitionJson,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
            Project project = projectRepository.findById(projectId).orElseThrow();
            validateProjectOwnership(user, project);
            jsonParserUtil.parse(definitionJson); // validation
            TestCase testCase = new TestCase();
            testCase.setProject(project);
            testCase.setName(name);
            testCase.setType(type);
            testCase.setDefinitionJson(definitionJson);
            testCaseRepository.save(testCase);
            redirectAttributes.addFlashAttribute("message", "Test '%s' created".formatted(name));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Failed to create test", ex);
            redirectAttributes.addFlashAttribute("error", "Failed to create test: " + ex.getMessage());
        }
        return projectId != null
            ? "redirect:/test-management?projectId=" + projectId
            : "redirect:/test-management";
    }

    @PostMapping("/test-management/project/{projectId}/endpoint-analysis")
    public String triggerEndpointAnalysis(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long projectId,
                                          @RequestParam("domainUrl") String domainUrl,
                                          RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
            Project project = projectRepository.findById(projectId).orElseThrow();
            validateProjectOwnership(user, project);

            EndpointAnalysisResult result = endpointAnalysisService.performAnalysis(project, domainUrl);
            redirectAttributes.addFlashAttribute("message",
                "Endpoint analysis completed with status %s".formatted(result.getStatus()));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Endpoint analysis failed for project {}", projectId, ex);
            redirectAttributes.addFlashAttribute("error", "Endpoint analysis failed: " + ex.getMessage());
        }
        return "redirect:/test-management?projectId=" + projectId + "#analysis";
    }

    @PostMapping("/test-management/test/{id}/run")
    public String runTest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long projectId = null;
        try {
            TestCase testCase = testCaseRepository.findById(id).orElseThrow();
            projectId = testCase.getProject().getId();
            schedulingService.executeTestCase(testCase);
            redirectAttributes.addFlashAttribute("message", "Execution queued for '%s'".formatted(testCase.getName()));
        } catch (Exception e) {
            logger.error("Error starting test with ID " + id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to start test: " + e.getMessage());
        }
        return "redirect:/test-management?projectId=" + projectId;
    }

    @PostMapping("/test-management/tests/import")
    public String importTests(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam Long projectId,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              @RequestParam(value = "payload", required = false) String payload,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
            Project project = projectRepository.findById(projectId).orElseThrow();
            validateProjectOwnership(user, project);

            String importJson = extractImportPayload(file, payload);
            if (importJson == null || importJson.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Provide a JSON file or paste JSON payload");
                return "redirect:/test-management?projectId=" + projectId;
            }

            JsonNode root = jsonParserUtil.parse(importJson);
            JsonNode testsNode = root.path("tests");
            if (!testsNode.isArray() || !testsNode.elements().hasNext()) {
                redirectAttributes.addFlashAttribute("error", "Import payload must contain a non-empty 'tests' array");
                return "redirect:/test-management?projectId=" + projectId;
            }

            int created = 0;
            int updated = 0;
            Iterator<JsonNode> iterator = testsNode.elements();
            while (iterator.hasNext()) {
                JsonNode testNode = iterator.next();
                String name = testNode.path("name").asText("").trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Each test must include a 'name'");
                }

                String typeValue = testNode.path("type").asText("UI");
                TestCaseType type;
                try {
                    type = TestCaseType.valueOf(typeValue.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Unsupported test type '" + typeValue + "'");
                }

                String definitionJson = resolveDefinition(testNode);
                jsonParserUtil.parse(definitionJson); // validation

                TestCase testCase = testCaseRepository.findByProjectAndNameIgnoreCase(project, name)
                    .orElseGet(() -> {
                        TestCase tc = new TestCase();
                        tc.setProject(project);
                        tc.setName(name);
                        return tc;
                    });
                boolean isNew = testCase.getId() == null;
                testCase.setType(type);
                testCase.setDefinitionJson(definitionJson);
                testCaseRepository.save(testCase);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }

            redirectAttributes.addFlashAttribute("message",
                "Imported %d tests (%d created, %d updated)".formatted(created + updated, created, updated));
        } catch (IllegalArgumentException | IOException ex) {
            logger.error("Bulk import failed", ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/test-management?projectId=" + projectId;
    }

    @PostMapping("/test-management/project/{projectId}/run-suite")
    public String runFullSuite(@AuthenticationPrincipal UserDetails principal,
                               @PathVariable Long projectId,
                               @RequestParam(name = "threadCount", defaultValue = "4") int threadCount,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
            Project project = projectRepository.findById(projectId).orElseThrow();
            validateProjectOwnership(user, project);

            int sanitizedThreads = Math.max(1, Math.min(threadCount, 64));
            schedulingService.executeProjectAsync(project, sanitizedThreads);
            redirectAttributes.addFlashAttribute("message",
                "Triggered project run with %d parallel workers".formatted(sanitizedThreads));
        } catch (Exception ex) {
            logger.error("Failed to trigger project run", ex);
            redirectAttributes.addFlashAttribute("error", "Failed to start project run: " + ex.getMessage());
        }
        return "redirect:/test-management?projectId=" + projectId;
    }

    @PostMapping("/test-management/action")
    public String createActionFile(@AuthenticationPrincipal UserDetails principal,
                                   @RequestParam Long projectId,
                                   @RequestParam String name,
                                   @RequestParam String content,
                                   RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        validateProjectOwnership(user, project);
        GeneratedActionFile actionFile = new GeneratedActionFile();
        actionFile.setProject(project);
        actionFile.setName(name);
        actionFile.setContent(content);
        generatedActionFileRepository.save(actionFile);
        redirectAttributes.addFlashAttribute("message", "Saved action file '" + name + "'");
        return "redirect:/test-management?projectId=" + projectId;
    }

    private void validateProjectOwnership(User user, Project project) {
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
    }

    private String extractImportPayload(MultipartFile file, String payload) throws IOException {
        if (file != null && !file.isEmpty()) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        return payload != null ? payload.trim() : null;
    }

    private String resolveDefinition(JsonNode testNode) {
        JsonNode definitionNode = testNode.path("definition");
        if (definitionNode.isMissingNode() || definitionNode.isNull()) {
            String raw = testNode.path("definitionJson").asText("");
            if (raw.isBlank()) {
                throw new IllegalArgumentException("Test '" + testNode.path("name").asText("?") + "' missing definition");
            }
            return raw.trim();
        }
        return jsonParserUtil.toJson(definitionNode);
    }

    private EndpointAnalysisView toView(EndpointAnalysisResult result) {
        EndpointAnalysisPayload payload;
        try {
            payload = Optional.ofNullable(result.getPayloadJson())
                .filter(json -> !json.isBlank())
                .map(json -> jsonParserUtil.parse(json, EndpointAnalysisPayload.class))
                .orElseGet(() -> new EndpointAnalysisPayload(null, null, null, null));
        } catch (IllegalArgumentException ex) {
            logger.warn("Failed to parse endpoint analysis payload for result {}", result.getId(), ex);
            payload = new EndpointAnalysisPayload(null, null, null, List.of("Payload parsing error"));
        }
        return new EndpointAnalysisView(result, payload);
    }

    private static class EndpointAnalysisView {
        private final EndpointAnalysisResult entity;
        private final EndpointAnalysisPayload payload;

        private EndpointAnalysisView(EndpointAnalysisResult entity, EndpointAnalysisPayload payload) {
            this.entity = entity;
            this.payload = payload;
        }

        public EndpointAnalysisResult getEntity() {
            return entity;
        }

        public EndpointAnalysisPayload getPayload() {
            return payload;
        }
    }
}
