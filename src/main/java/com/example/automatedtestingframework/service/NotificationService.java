package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final String senderEmail;
    private final String senderName;
    private final String apiKey;

    public NotificationService(@Value("${brevo.sender.email:no-reply@example.com}") String senderEmail,
                               @Value("${brevo.sender.name:Automation Platform}") String senderName,
                               @Value("${brevo.api-key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.apiKey = apiKey;
    }

    public void notifyRunCompletion(Project project, List<Report> reports, List<User> recipients) {
        if (recipients.isEmpty()) {
            log.debug("Skip notification, no recipients configured");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Brevo API key missing. Notification skipped.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> sender = Map.of("name", senderName, "email", senderEmail);
        payload.put("sender", sender);
        payload.put("to", recipients.stream()
            .map(user -> Map.of("email", user.getEmail(), "name", user.getFullName()))
            .toList());
        payload.put("subject", String.format("%s - Automated test results", project.getName()));
        payload.put("htmlContent", buildHtmlSummary(project, reports));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        try {
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", new HttpEntity<>(payload, headers), String.class);
            log.info("Notification dispatched to {} recipients", recipients.size());
        } catch (Exception ex) {
            log.error("Failed to notify via Brevo", ex);
        }
    }

    private String buildHtmlSummary(Project project, List<Report> reports) {
        long passed = reports.stream().filter(r -> "PASSED".equalsIgnoreCase(r.getStatus())).count();
        long failed = reports.size() - passed;
        StringBuilder builder = new StringBuilder();
        builder.append("<h2>Automation run summary for ").append(project.getName()).append("</h2>");
        builder.append("<p>Passed: ").append(passed).append(" | Failed: ").append(failed).append("</p>");
        builder.append("<table border='1' cellpadding='6' cellspacing='0'>");
        builder.append("<tr><th>Test</th><th>Status</th><th>Type</th><th>Started</th><th>Completed</th><th>Error</th></tr>");
        reports.forEach(report -> builder
            .append("<tr>")
            .append("<td>").append(report.getTestCase() != null ? report.getTestCase().getName() : "Ad-hoc").append("</td>")
            .append("<td>").append(report.getStatus()).append("</td>")
            .append("<td>").append(report.getTestCase() != null ? report.getTestCase().getType() : "-").append("</td>")
            .append("<td>").append(report.getStartedAt()).append("</td>")
            .append("<td>").append(report.getCompletedAt()).append("</td>")
            .append("<td>").append(report.getErrorMessage() != null ? report.getErrorMessage() : "").append("</td>")
            .append("</tr>"));
        builder.append("</table>");
        return builder.toString();
    }
}
