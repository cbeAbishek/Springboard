package org.example.service;

import org.example.model.TestSchedule;
import org.example.repository.TestScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTestExecutionService.class);

    @Autowired
    private TestScheduleRepository scheduleRepository;

    @Autowired
    private TestExecutionService executionService;

    /**
     * Check for due schedules every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void checkDueSchedules() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TestSchedule> dueSchedules = scheduleRepository.findDueSchedules(now);

            log.info("Found {} schedules due for execution", dueSchedules.size());

            for (TestSchedule schedule : dueSchedules) {
                try {
                    executeScheduledTest(schedule);
                } catch (Exception e) {
                    log.error("Failed to execute scheduled test: {}", schedule.getName(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error checking due schedules", e);
        }
    }

    /**
     * Execute a scheduled test
     */
    private void executeScheduledTest(TestSchedule schedule) {
        log.info("Executing scheduled test: {}", schedule.getName());

        try {
            // Update last execution time
            schedule.setLastExecution(LocalDateTime.now());
            scheduleRepository.save(schedule);

            // Execute based on test type
            switch (schedule.getTestType()) {
                case "BlazeDemo":
                    executionService.executeBlazeDemo(schedule.getEnvironment(), schedule.getParallelExecution());
                    break;
                case "ReqRes":
                    executionService.executeReqRes(schedule.getEnvironment(), schedule.getParallelExecution());
                    break;
                case "Regression":
                    executionService.executeRegressionTests(schedule.getEnvironment(), schedule.getParallelExecution());
                    break;
                default:
                    log.warn("Unknown test type: {}", schedule.getTestType());
                    break;
            }

            log.info("Successfully executed scheduled test: {}", schedule.getName());

        } catch (Exception e) {
            log.error("Failed to execute scheduled test: {}", schedule.getName(), e);
        }
    }

    /**
     * Generate weekly summary - scheduled to run every Sunday at midnight
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void generateWeeklySummary() {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusWeeks(1);

            log.info("Generating weekly summary report from {} to {}", startDate, endDate);

            // Get execution statistics for the week
            java.util.Map<String, Object> stats = executionService.getExecutionStatistics();

            log.info("Weekly summary - Total executions: {}, Pass rate: {}%",
                    stats.get("totalExecutions"), stats.get("passRate"));

        } catch (Exception e) {
            log.error("Failed to generate weekly summary", e);
        }
    }

    /**
     * Cleanup old executions - scheduled to run daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldExecutions() {
        try {
            log.info("Starting cleanup of old executions");

            // Keep executions for 30 days
            executionService.cleanupOldExecutions(30);

            log.info("Completed cleanup of old executions");

        } catch (Exception e) {
            log.error("Failed to cleanup old executions", e);
        }
    }
}
