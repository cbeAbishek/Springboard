package org.example.scheduler;

import org.example.model.TestSchedule;
import org.example.service.TestExecutionService;
import org.example.repository.TestScheduleRepository;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class TestScheduler {

    private static final Logger log = LoggerFactory.getLogger(TestScheduler.class);

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private TestScheduleRepository testScheduleRepository;

    @Autowired
    private TestExecutionService testExecutionService;

    /**
     * Initialize scheduler and load existing schedules
     */
    @PostConstruct
    public void initializeScheduler() {
        try {
            log.info("Initializing test scheduler...");

            // Load and schedule all active test schedules from database
            List<TestSchedule> activeSchedules = testScheduleRepository.findByIsActiveTrue();

            for (TestSchedule schedule : activeSchedules) {
                try {
                    scheduleTest(schedule);
                    log.info("Loaded scheduled test: {}", schedule.getName());
                } catch (Exception e) {
                    log.error("Failed to load scheduled test: {}", schedule.getName(), e);
                }
            }

            log.info("Test scheduler initialized with {} active schedules", activeSchedules.size());

        } catch (Exception e) {
            log.error("Failed to initialize test scheduler", e);
        }
    }

    /**
     * Schedule a test execution
     */
    public void scheduleTest(TestSchedule testSchedule) {
        try {
            // Ensure entity is persisted
            if (testSchedule.getId() == null) {
                testSchedule = testScheduleRepository.save(testSchedule);
            }

            // Validate cron expression
            if (!CronExpression.isValidExpression(testSchedule.getCronExpression())) {
                throw new IllegalArgumentException("Invalid cron expression: " + testSchedule.getCronExpression());
            }

            JobKey jobKey = new JobKey("testJob_" + testSchedule.getId(), "testGroup");

            // Remove existing job if present
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.deleteJob(jobKey);
                log.info("Removed existing scheduled job: {}", jobKey);
            }

            // Create job detail
            JobDetail jobDetail = JobBuilder.newJob(ScheduledTestJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("scheduleId", testSchedule.getId())
                    .build();

            // Create trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("testTrigger_" + testSchedule.getId(), "testGroup")
                    .withSchedule(CronScheduleBuilder.cronSchedule(testSchedule.getCronExpression()))
                    .build();

            // Schedule the job
            quartzScheduler.scheduleJob(jobDetail, trigger);

            // Update next execution time
            Date nextFireTime = trigger.getNextFireTime();
            if (nextFireTime != null) {
                testSchedule.setNextExecution(nextFireTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                testScheduleRepository.save(testSchedule);
            }

            log.info("Successfully scheduled test: {} with cron: {}", testSchedule.getName(), testSchedule.getCronExpression());

        } catch (SchedulerException e) {
            log.error("Failed to schedule test: {}", testSchedule.getName(), e);
            throw new RuntimeException("Failed to schedule test", e);
        }
    }

    /**
     * Unschedule a test
     */
    public void unscheduleTest(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");

            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.deleteJob(jobKey);
                log.info("Unscheduled test job: {}", jobKey);
            }

        } catch (SchedulerException e) {
            log.error("Failed to unschedule test: {}", scheduleId, e);
            throw new RuntimeException("Failed to unschedule test", e);
        }
    }

    /**
     * Update existing schedule
     */
    public void updateSchedule(TestSchedule testSchedule) {
        try {
            // First unschedule the existing job
            unscheduleTest(testSchedule.getId());

            // Then schedule with new parameters
            if (testSchedule.getIsActive()) {
                scheduleTest(testSchedule);
            }

        } catch (Exception e) {
            log.error("Failed to update schedule: {}", testSchedule.getName(), e);
            throw new RuntimeException("Failed to update schedule", e);
        }
    }

    /**
     * Schedule BlazeDemo tests
     */
    public TestSchedule scheduleBlazeDemo(String cronExpression, String environment, boolean parallel) {
        TestSchedule schedule = new TestSchedule();
        schedule.setName("BlazeDemo_Scheduled_" + System.currentTimeMillis());
        schedule.setDescription("Scheduled BlazeDemo UI tests");
        schedule.setCronExpression(cronExpression);
        schedule.setTestType("BlazeDemo");
        schedule.setEnvironment(environment);
        schedule.setParallelExecution(parallel);
        schedule.setMaxRetries(1);
        schedule.setTimeoutMinutes(60);
        schedule.setIsActive(true);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setCreatedBy("system");

        schedule = testScheduleRepository.save(schedule);
        scheduleTest(schedule);

        return schedule;
    }

    /**
     * Schedule ReqRes API tests
     */
    public TestSchedule scheduleReqRes(String cronExpression, String environment, boolean parallel) {
        TestSchedule schedule = new TestSchedule();
        schedule.setName("ReqRes_Scheduled_" + System.currentTimeMillis());
        schedule.setDescription("Scheduled ReqRes API integration tests");
        schedule.setCronExpression(cronExpression);
        schedule.setTestType("ReqRes");
        schedule.setEnvironment(environment);
        schedule.setParallelExecution(parallel);
        schedule.setMaxRetries(2);
        schedule.setTimeoutMinutes(30);
        schedule.setIsActive(true);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setCreatedBy("system");

        schedule = testScheduleRepository.save(schedule);
        scheduleTest(schedule);

        return schedule;
    }

    /**
     * Schedule regression tests
     */
    public TestSchedule scheduleRegression(String cronExpression, String environment) {
        TestSchedule schedule = new TestSchedule();
        schedule.setName("Regression_Scheduled_" + System.currentTimeMillis());
        schedule.setDescription("Scheduled regression test suite");
        schedule.setCronExpression(cronExpression);
        schedule.setTestType("Regression");
        schedule.setEnvironment(environment);
        schedule.setParallelExecution(true);
        schedule.setMaxRetries(1);
        schedule.setTimeoutMinutes(120);
        schedule.setIsActive(true);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setCreatedBy("system");

        schedule = testScheduleRepository.save(schedule);
        scheduleTest(schedule);

        return schedule;
    }

    /**
     * Get all active schedules
     */
    public List<TestSchedule> getActiveSchedules() {
        return testScheduleRepository.findByIsActiveTrue();
    }

    /**
     * Pause a schedule
     */
    public void pauseSchedule(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");
            quartzScheduler.pauseJob(jobKey);
            log.info("Paused scheduled job: {}", jobKey);

        } catch (SchedulerException e) {
            log.error("Failed to pause schedule: {}", scheduleId, e);
            throw new RuntimeException("Failed to pause schedule", e);
        }
    }

    /**
     * Resume a schedule
     */
    public void resumeSchedule(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");
            quartzScheduler.resumeJob(jobKey);
            log.info("Resumed scheduled job: {}", jobKey);

        } catch (SchedulerException e) {
            log.error("Failed to resume schedule: {}", scheduleId, e);
            throw new RuntimeException("Failed to resume schedule", e);
        }
    }

    /**
     * Get schedule status
     */
    public String getScheduleStatus(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");

            if (!quartzScheduler.checkExists(jobKey)) {
                return "NOT_SCHEDULED";
            }

            List<Trigger> triggers = (List<Trigger>) quartzScheduler.getTriggersOfJob(jobKey);
            if (triggers.isEmpty()) {
                return "NO_TRIGGERS";
            }

            Trigger.TriggerState state = quartzScheduler.getTriggerState(triggers.get(0).getKey());
            return state.name();

        } catch (SchedulerException e) {
            log.error("Failed to get schedule status: {}", scheduleId, e);
            return "ERROR";
        }
    }

    /**
     * Execute scheduled test immediately
     */
    public void executeNow(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");
            quartzScheduler.triggerJob(jobKey);
            log.info("Triggered immediate execution of job: {}", jobKey);

        } catch (SchedulerException e) {
            log.error("Failed to execute schedule immediately: {}", scheduleId, e);
            throw new RuntimeException("Failed to execute schedule", e);
        }
    }

    /**
     * Scheduled job implementation
     */
    @DisallowConcurrentExecution
    public static class ScheduledTestJob implements Job {

        private static final Logger jobLog = LoggerFactory.getLogger(ScheduledTestJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Long scheduleId = context.getJobDetail().getJobDataMap().getLong("scheduleId");

                // Get Spring context and services
                ApplicationContext appContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");
                TestScheduleRepository scheduleRepository = appContext.getBean(TestScheduleRepository.class);
                TestExecutionService executionService = appContext.getBean(TestExecutionService.class);

                // Get schedule details
                TestSchedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new JobExecutionException("Schedule not found: " + scheduleId));

                if (!schedule.getIsActive()) {
                    jobLog.info("Schedule {} is inactive, skipping execution", scheduleId);
                    return;
                }

                jobLog.info("Executing scheduled test: {}", schedule.getName());

                // Update last execution time
                schedule.setLastExecution(LocalDateTime.now());
                scheduleRepository.save(schedule);

                // Execute tests based on type
                try {
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
                            jobLog.warn("Unknown test type: {}", schedule.getTestType());
                            break;
                    }

                    jobLog.info("Successfully completed scheduled test: {}", schedule.getName());

                } catch (Exception e) {
                    jobLog.error("Failed to execute scheduled test: {}", schedule.getName(), e);
                    throw new JobExecutionException("Test execution failed", e);
                }

            } catch (Exception e) {
                jobLog.error("Scheduled job execution failed", e);
                throw new JobExecutionException(e);
            }
        }
    }
}
