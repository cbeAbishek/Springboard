package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import com.example.automatedtestingframework.service.ActionTemplateService;
import com.example.automatedtestingframework.service.ReportingService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReportingService reportingService;
    private final ActionTemplateService actionTemplateService;

    public DashboardController(ProjectRepository projectRepository,
                               UserRepository userRepository,
                               ReportingService reportingService,
                               ActionTemplateService actionTemplateService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.reportingService = reportingService;
        this.actionTemplateService = actionTemplateService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam(name = "projectId", required = false) Long projectId,
                            Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        List<Project> projects = projectRepository.findByOwner(user);
        model.addAttribute("projects", projects);
        Project selected = resolveSelectedProject(projects, projectId);
        if (selected != null) {
            model.addAttribute("selectedProject", selected);
            model.addAttribute("summary", reportingService.buildSummary(selected));
        }
        return "dashboard";
    }

    @PostMapping("/dashboard/project")
    public String createProject(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam("name") String name,
                                @RequestParam(value = "description", required = false) String description,
                                RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Project name is required");
            return "redirect:/dashboard";
        }

        Optional<Project> existing = projectRepository.findByOwnerAndNameIgnoreCase(user, trimmedName);
        if (existing.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "You already have a project with this name");
            return "redirect:/dashboard";
        }

        Project project = new Project();
        project.setOwner(user);
        project.setName(trimmedName);
        if (description != null && !description.isBlank()) {
            project.setDescription(description.trim());
        }
        Project saved = projectRepository.save(project);
        redirectAttributes.addFlashAttribute("message", "Project '" + trimmedName + "' created");
        return "redirect:/dashboard?projectId=" + saved.getId();
    }

    @GetMapping("/dashboard/project/{projectId}/github-action")
    public ResponseEntity<ByteArrayResource> downloadGithubAction(@AuthenticationPrincipal UserDetails principal,
                                                                  @PathVariable Long projectId) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        String content = actionTemplateService.render(project);
        ByteArrayResource resource = new ByteArrayResource(content.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("automation-workflow-" + project.getId() + ".yml")
            .build());
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType("application/x-yaml"))
            .contentLength(resource.contentLength())
            .body(resource);
    }

    @PostMapping("/dashboard/project/{projectId}/integration")
    public String updateIntegration(@AuthenticationPrincipal UserDetails principal,
                                    @PathVariable Long projectId,
                                    @RequestParam(name = "endpoint", required = false) String endpoint,
                                    @RequestParam(name = "token", required = false) String token,
                                    @RequestParam(name = "targetProjectId", required = false) String targetProjectId,
                                    @RequestParam(name = "branches", required = false) String branches,
                                    RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findByIdAndOwner(projectId, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        project.setAutomationEndpoint(clean(endpoint));
        project.setAutomationToken(clean(token));
        project.setAutomationTargetProjectId(clean(targetProjectId));
        if (project.getAutomationTargetProjectId() == null) {
            project.setAutomationTargetProjectId(String.valueOf(project.getId()));
        }
        project.setAutomationBranches(normalizeBranches(branches));
        projectRepository.save(project);

        redirectAttributes.addFlashAttribute("message", "Integration settings updated for " + project.getName());
        return "redirect:/dashboard?projectId=" + projectId;
    }

    private Project resolveSelectedProject(List<Project> projects, Long requestedId) {
        if (projects.isEmpty()) {
            return null;
        }
        if (requestedId == null) {
            return projects.get(0);
        }
        return projects.stream()
            .filter(project -> project.getId().equals(requestedId))
            .findFirst()
            .orElse(projects.get(0));
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeBranches(String branches) {
        if (!StringUtils.hasText(branches)) {
            return null;
        }
        return Arrays.stream(branches.split("[,\n]"))
            .map(String::trim)
            .filter(segment -> !segment.isEmpty())
            .distinct()
            .collect(Collectors.joining(","));
    }
}
