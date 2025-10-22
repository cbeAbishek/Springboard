package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class ProjectSetupController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectSetupController(ProjectRepository projectRepository,
                                  UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/project-setup")
    public String showProjectSetup(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        
        // If user already has projects, redirect to dashboard
        if (user.getProjects() != null && !user.getProjects().isEmpty()) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("user", user);
        return "project-setup";
    }

    @PostMapping("/project-setup")
    public String createFirstProject(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam("name") String name,
                                     @RequestParam(value = "description", required = false) String description,
                                     @RequestParam(value = "githubLink", required = false) String githubLink,
                                     @RequestParam(value = "websiteLink", required = false) String websiteLink,
                                     @RequestParam(value = "appDomain", required = false) String appDomain,
                                     RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        
        // Validate project name
        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Project name is required");
            return "redirect:/project-setup";
        }

        // Check for duplicate project name
        Optional<Project> existing = projectRepository.findByOwnerAndNameIgnoreCase(user, trimmedName);
        if (existing.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "You already have a project with this name");
            return "redirect:/project-setup";
        }

        // Create the project
        Project project = new Project();
        project.setOwner(user);
        project.setName(trimmedName);
        
        if (description != null && !description.isBlank()) {
            project.setDescription(description.trim());
        }
        
        // Set optional fields
        if (githubLink != null && !githubLink.isBlank()) {
            project.setGithubLink(githubLink.trim());
        }
        
        if (websiteLink != null && !websiteLink.isBlank()) {
            project.setWebsiteLink(websiteLink.trim());
        }
        
        if (appDomain != null && !appDomain.isBlank()) {
            project.setAppDomain(appDomain.trim());
        }
        
        Project saved = projectRepository.save(project);
        redirectAttributes.addFlashAttribute("message", "Welcome! Your project '" + trimmedName + "' has been created successfully");
        return "redirect:/dashboard?projectId=" + saved.getId();
    }
}
