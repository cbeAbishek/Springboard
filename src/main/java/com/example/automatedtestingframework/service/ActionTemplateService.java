package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Project;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Service
public class ActionTemplateService {

    private static final String TEMPLATE_PATH = "github-actions/automation-trigger.yml";

    private final Resource templateResource = new ClassPathResource(TEMPLATE_PATH);
    private volatile String cachedTemplate;

    public String render(Project project) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Automation workflow for project '")
            .append(project.getName())
            .append("' (ID: ")
            .append(project.getId())
            .append(")\n");
        builder.append("# Generated from Automation Platform on ")
            .append(OffsetDateTime.now())
            .append("\n\n");
        builder.append(applyProjectContext(project, loadTemplate()));
        return builder.toString();
    }

    private String loadTemplate() {
        String template = cachedTemplate;
        if (template != null) {
            return template;
        }
        synchronized (this) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }
            try (InputStream inputStream = templateResource.getInputStream()) {
                cachedTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return cachedTemplate;
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to load GitHub Actions template from classpath:" + TEMPLATE_PATH, ex);
            }
        }
    }

    private String applyProjectContext(Project project, String template) {
        String branchesBlock = buildBranchesBlock(project);
        String endpoint = coalesce(project.getAutomationEndpoint(), "https://automation.example.com");
        String targetProjectId = coalesce(project.getAutomationTargetProjectId(), String.valueOf(project.getId()));
        String token = coalesce(project.getAutomationToken(), "<insert-automation-token>");

        return template
            .replace("{{PROJECT_NAME}}", project.getName())
            .replace("{{AUTOMATION_ENDPOINT}}", endpoint)
            .replace("{{AUTOMATION_PROJECT_ID}}", targetProjectId)
            .replace("{{AUTOMATION_TOKEN}}", token)
            .replace("{{BRANCHES_BLOCK}}", branchesBlock);
    }

    private String buildBranchesBlock(Project project) {
        String value = project.getAutomationBranches();
        if (value == null || value.isBlank()) {
            return "      - main";
        }
        String[] branches = value.split(",");
        String block = java.util.Arrays.stream(branches)
            .map(String::trim)
            .filter(segment -> !segment.isEmpty())
            .distinct()
            .map(segment -> "      - " + segment)
            .collect(java.util.stream.Collectors.joining("\n"));
        return block.isEmpty() ? "      - main" : block;
    }

    private String coalesce(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
