package org.example.service;

import org.example.dto.TestExecutionResultDTO;
import org.example.model.TestSchedule;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendSuccessNotification(TestSchedule schedule, String executionSummary) {
        try {
            String subject = String.format("✅ Test Execution Completed Successfully - %s", schedule.getName());
            String message = buildSuccessMessage(schedule, executionSummary);

            // In a real implementation, you would integrate with email service, Slack, etc.
            log.info("SUCCESS NOTIFICATION:\nSubject: {}\nMessage: {}", subject, message);

            // Since notificationEmails field doesn't exist in corrected TestSchedule model,
            // we'll use a default email or skip email sending
            log.info("Sending success notification for schedule: {}", schedule.getName());

        } catch (Exception e) {
            log.error("Error sending success notification", e);
        }
    }

    public void sendFailureNotification(TestSchedule schedule, String errorMessage) {
        try {
            String subject = String.format("❌ Test Execution Failed - %s", schedule.getName());
            String message = buildFailureMessage(schedule, errorMessage);

            log.error("FAILURE NOTIFICATION:\nSubject: {}\nMessage: {}", subject, message);

            // Since notificationEmails field doesn't exist in corrected TestSchedule model,
            // we'll use a default email or skip email sending
            log.error("Sending failure notification for schedule: {}", schedule.getName());

        } catch (Exception e) {
            log.error("Error sending failure notification", e);
        }
    }

    private String buildSuccessMessage(TestSchedule schedule, String executionSummary) {
        return String.format("""
            🎉 Test execution completed successfully!
            
            Schedule: %s
            Environment: %s
            
            📊 Execution Summary:
            %s
            
            ⏱️ Next scheduled execution: %s
            
            Great job! All tests passed successfully.
            """,
            schedule.getName(),
            schedule.getEnvironment(),
            executionSummary,
            schedule.getNextExecution() != null ? schedule.getNextExecution().format(formatter) : "Not scheduled"
        );
    }

    private String buildFailureMessage(TestSchedule schedule, String errorMessage) {
        return String.format("""
            ⚠️ Test execution failed!
            
            Schedule: %s
            Environment: %s
            
            🔍 Error Details:
            %s
            
            🔄 Next scheduled execution: %s
            
            Please check the logs for more details.
            """,
            schedule.getName(),
            schedule.getEnvironment(),
            errorMessage,
            schedule.getNextExecution() != null ? schedule.getNextExecution().format(formatter) : "Not scheduled"
        );
    }

    private double calculateSuccessRate(String executionSummary) {
        // Since we're now receiving a String summary, return a default success rate
        return 100.0; // Assuming success if this method is called
    }
}
