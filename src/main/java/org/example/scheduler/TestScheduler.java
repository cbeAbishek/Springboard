package org.example.scheduler;

import org.example.model.TestSchedule;
import org.example.service.TestExecutionService;
import org.example.repository.TestScheduleRepository;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.quartz.CronExpression;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Component
public class TestScheduler {

    private static final Logger log = LoggerFactory.getLogger(TestScheduler.class);

    @Autowired
    private Scheduler quartzScheduler;

    @Autowired
    private TestScheduleRepository testScheduleRepository;

    @Autowired
    private TestExecutionService testExecutionService;

    public void scheduleTest(TestSchedule testSchedule) {
        try {
            // Ensure entity is persisted to have an ID
            if (testSchedule.getId() == null) {
                testSchedule = testScheduleRepository.save(testSchedule);
            }
            // Validate cron expression
            if (!CronExpression.isValidExpression(testSchedule.getCronExpression())) {
                throw new IllegalArgumentException("Invalid cron expression: " + testSchedule.getCronExpression());
            }
            JobKey jobKey = new JobKey("testJob_" + testSchedule.getId(), "testGroup");
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.deleteJob(jobKey); // replace existing
            }
            JobDetail jobDetail = JobBuilder.newJob(ScheduledTestJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("scheduleId", testSchedule.getId())
                    .usingJobData("testSuite", nullSafe(testSchedule.getTestSuite()))
                    .usingJobData("environment", nullSafe(testSchedule.getEnvironment()))
                    .usingJobData("parallelThreads", defaultInt(testSchedule.getParallelThreads(), 1))
                    .build();
            TriggerKey triggerKey = new TriggerKey("testTrigger_" + testSchedule.getId(), "testGroup");
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(testSchedule.getCronExpression()))
                    .build();
            quartzScheduler.scheduleJob(jobDetail, trigger);
            Date nextFireTime = trigger.getNextFireTime();
            if (nextFireTime != null) {
                testSchedule.setNextExecution(LocalDateTime.ofInstant(nextFireTime.toInstant(), ZoneId.systemDefault()));
            }
            testScheduleRepository.save(testSchedule);
            log.info("Scheduled test: {} (id={}) cron={} nextFire={} ",
                    testSchedule.getScheduleName(), testSchedule.getId(), testSchedule.getCronExpression(), testSchedule.getNextExecution());
        } catch (SchedulerException e) {
            log.error("Failed to schedule test: {}", testSchedule.getScheduleName(), e);
            throw new RuntimeException("Scheduling failed", e);
        }
    }

    public void scheduleAllActiveOnStartup() {
        List<TestSchedule> active = testScheduleRepository.findByIsActiveTrue();
        for (TestSchedule s : active) {
            try {
                scheduleTest(s);
            } catch (Exception ex) {
                log.error("Failed to (re)schedule active schedule id={} name={}", s.getId(), s.getScheduleName(), ex);
            }
        }
        log.info("(Re)scheduled {} active schedules on startup", active.size());
    }

    public List<LocalDateTime> previewNextExecutions(String cronExpression, int count) {
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression");
        }
        try {
            CronExpression cron = new CronExpression(cronExpression);
            List<LocalDateTime> times = new ArrayList<>();
            Date next = new Date();
            for (int i = 0; i < count; i++) {
                next = cron.getNextValidTimeAfter(next);
                if (next == null) break;
                times.add(LocalDateTime.ofInstant(next.toInstant(), ZoneId.systemDefault()));
            }
            return times;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse cron", e);
        }
    }

    public void triggerNow(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");
            if (!quartzScheduler.checkExists(jobKey)) {
                TestSchedule s = testScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new RuntimeException("Schedule not found " + scheduleId));
                scheduleTest(s); // auto create if missing
            }
            quartzScheduler.triggerJob(jobKey);
            log.info("Manually triggered schedule id={}", scheduleId);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to trigger schedule " + scheduleId, e);
        }
    }

    private String nullSafe(String v) { return v == null ? "" : v; }
    private int defaultInt(Integer v, int d) { return v == null ? d : v; }

    public void unscheduleTest(Long scheduleId) {
        try {
            JobKey jobKey = new JobKey("testJob_" + scheduleId, "testGroup");
            TriggerKey triggerKey = new TriggerKey("testTrigger_" + scheduleId, "testGroup");
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.pauseTrigger(triggerKey);
                quartzScheduler.unscheduleJob(triggerKey);
                quartzScheduler.deleteJob(jobKey);
            }
            log.info("Unscheduled test with ID: {}", scheduleId);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule test with ID: {}", scheduleId, e);
        }
    }

    public void rescheduleTest(TestSchedule testSchedule) {
        unscheduleTest(testSchedule.getId());
        scheduleTest(testSchedule);
    }

    public List<TestSchedule> getActiveSchedules() {
        return testScheduleRepository.findByIsActiveTrue();
    }

    @DisallowConcurrentExecution
    public static class ScheduledTestJob implements Job {

        private static final Logger log = LoggerFactory.getLogger(ScheduledTestJob.class);

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();

            Long scheduleId = dataMap.getLong("scheduleId");
            String testSuite = dataMap.getString("testSuite");
            String environment = dataMap.getString("environment");
            Integer parallelThreads = dataMap.getInt("parallelThreads");

            try {
                log.info("Executing scheduled test: {} for environment: {}", testSuite, environment);

                // Get Spring ApplicationContext from Quartz context
                ApplicationContext applicationContext = (ApplicationContext) context.getScheduler().getContext().get("applicationContext");

                if (applicationContext == null) {
                    throw new JobExecutionException("ApplicationContext not found in scheduler context");
                }

                // Get Spring beans
                TestExecutionService testExecutionService = applicationContext.getBean(TestExecutionService.class);
                TestScheduleRepository testScheduleRepository = applicationContext.getBean(TestScheduleRepository.class);

                // Get the TestSchedule object
                TestSchedule schedule = testScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new JobExecutionException("Schedule not found: " + scheduleId));

                // Update last execution time
                schedule.setLastExecution(LocalDateTime.now());
                // Recompute next execution after this run
                try {
                    Trigger trigger = context.getTrigger();
                    Date nf = trigger.getNextFireTime();
                    if (nf != null) {
                        schedule.setNextExecution(LocalDateTime.ofInstant(nf.toInstant(), ZoneId.systemDefault()));
                    }
                } catch (Exception ignore) { }
                testScheduleRepository.save(schedule);

                // Execute the scheduled tests using the proper service method
                CompletableFuture.runAsync(() -> {
                    try {
                        testExecutionService.executeScheduledTests(schedule);
                        log.info("Scheduled test execution completed for schedule ID: {}", scheduleId);
                    } catch (Exception e) {
                        log.error("Scheduled test execution failed for schedule ID: {}", scheduleId, e);
                    }
                });

                log.info("Scheduled test execution initiated successfully for schedule ID: {}", scheduleId);

            } catch (Exception e) {
                log.error("Scheduled test execution failed for schedule ID: {}", scheduleId, e);
                throw new JobExecutionException("Scheduled test execution failed", e);
            }
        }
    }
}
