package com.example.automatedtestingframework.controller;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.SchedulerFrequency;
import com.example.automatedtestingframework.model.SchedulerJob;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.SchedulerJobRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import com.example.automatedtestingframework.service.SchedulingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping
public class SchedulerController {

    private final SchedulerJobRepository schedulerJobRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SchedulingService schedulingService;

    public SchedulerController(SchedulerJobRepository schedulerJobRepository,
                               ProjectRepository projectRepository,
                               UserRepository userRepository,
                               SchedulingService schedulingService) {
        this.schedulerJobRepository = schedulerJobRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.schedulingService = schedulingService;
    }

    @GetMapping("/scheduler")
    public String schedulerPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        List<Project> projects = projectRepository.findByOwner(user);
        if (!projects.isEmpty()) {
            model.addAttribute("jobs", schedulerJobRepository.findByProject(projects.get(0)));
        }
        model.addAttribute("projects", projects);
        model.addAttribute("frequencies", SchedulerFrequency.values());
        return "scheduler";
    }

    @PostMapping("/scheduler/job")
    public String createJob(@AuthenticationPrincipal UserDetails principal,
                            @RequestParam Long projectId,
                            @RequestParam SchedulerFrequency frequency,
                            @RequestParam String name,
                            @RequestParam(required = false) String cronExpression) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        schedulingService.createJob(projectId, frequency, name, cronExpression);
        return "redirect:/scheduler";
    }

    @PostMapping("/scheduler/job/{id}/run")
    public String runNow(@PathVariable Long id) {
        SchedulerJob job = schedulerJobRepository.findById(id).orElseThrow();
        schedulingService.scheduleAsyncRun(job);
        return "redirect:/scheduler";
    }

    @PostMapping("/api/run/project/{projectId}")
    @ResponseBody
    public ResponseEntity<String> runProject(@PathVariable Long projectId,
                                             @RequestHeader(value = "X-AUTOMATION-TOKEN", required = false) String token) {
        // Future enhancement: validate token. For now ensure project exists.
        Project project = projectRepository.findById(projectId).orElseThrow();
        schedulingService.executeProject(projectId);
        return ResponseEntity.accepted().body("Execution triggered for project " + project.getName());
    }
}
