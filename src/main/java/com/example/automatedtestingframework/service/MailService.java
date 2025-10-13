package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.GeneratedReport;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${app.mail.enabled:true}") boolean enabled,
                       @Value("${app.mail.from:no-reply@automation.local}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.enabled = enabled && this.mailSender != null;
        this.fromAddress = fromAddress;
    }

    public void sendGeneratedReport(User user, Project project, GeneratedReport generatedReport, String filterSummary) {
        if (!enabled || mailSender == null) {
            log.info("Mail sending disabled; skipping email for generated report {}", generatedReport.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            if (StringUtils.hasText(fromAddress)) {
                message.setFrom(fromAddress);
            }
            message.setSubject("Automation report ready for " + project.getName());

            StringBuilder body = new StringBuilder();
            body.append("Hi ").append(user.getFullName()).append(",\n\n");
            body.append("Your filtered report for project '").append(project.getName()).append("' is ready.\n");
            body.append(filterSummary).append("\n\n");
            body.append("Download link: ").append(generatedReport.getFileUrl()).append("\n");
            body.append("Records included: ").append(generatedReport.getTotalRecords()).append("\n");
            body.append("Generated at: ").append(generatedReport.getCreatedAt()).append("\n\n");
            body.append("Thanks,\nAutomation Platform");

            message.setText(body.toString());
            mailSender.send(message);
            log.info("Sent generated report email to {}", user.getEmail());
        } catch (MailException ex) {
            log.error("Failed to send generated report email to {}", user.getEmail(), ex);
        }
    }
}
