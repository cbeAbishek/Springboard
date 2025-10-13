package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.SchedulerFrequency;
import com.example.automatedtestingframework.model.SchedulerJob;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.model.User;
import com.example.automatedtestingframework.repository.ProjectRepository;
import com.example.automatedtestingframework.repository.SchedulerJobRepository;
import com.example.automatedtestingframework.repository.TestCaseRepository;
import com.example.automatedtestingframework.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    private final SchedulerJobRepository schedulerJobRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApiTestExecutor apiTestExecutor;
    private final UiTestExecutor uiTestExecutor;
    private final TaskScheduler taskScheduler;
    private final Executor testExecutor;

    private final List<Long> runningJobs = new CopyOnWriteArrayList<>();

    public SchedulingService(SchedulerJobRepository schedulerJobRepository,
                             TestCaseRepository testCaseRepository,
                             ProjectRepository projectRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             ApiTestExecutor apiTestExecutor,
                             UiTestExecutor uiTestExecutor,
                             TaskScheduler taskScheduler,
                             @Qualifier("testExecutor") Executor testExecutor) {
        this.schedulerJobRepository = schedulerJobRepository;
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.apiTestExecutor = apiTestExecutor;
        this.uiTestExecutor = uiTestExecutor;
        this.taskScheduler = taskScheduler;
        this.testExecutor = testExecutor;
    }

    public SchedulerJob createJob(Long projectId, SchedulerFrequency frequency, String name, String cronExpression) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        SchedulerJob job = new SchedulerJob();
        job.setProject(project);
        job.setFrequency(frequency);
        job.setName(name);
        job.setCronExpression(cronExpression);
        job.setNextRunAt(computeNextRun(frequency, cronExpression));
        return schedulerJobRepository.save(job);
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void evaluateSchedules() {
        List<SchedulerJob> jobs = schedulerJobRepository.findAll();
        OffsetDateTime now = OffsetDateTime.now();
        for (SchedulerJob job : jobs) {
            if (!job.isActive()) {
                continue;
            }
            if (job.getNextRunAt() != null && job.getNextRunAt().isBefore(now.plusSeconds(1))) {
                scheduleAsyncRun(job);
                job.setNextRunAt(computeNextRun(job.getFrequency(), job.getCronExpression()));
                schedulerJobRepository.save(job);
            }
        }
    }

    public void scheduleAsyncRun(SchedulerJob job) {
        if (runningJobs.contains(job.getId())) {
            log.info("Job {} already running", job.getId());
            return;
        }
        runningJobs.add(job.getId());
        taskScheduler.schedule(() -> {
            try {
                executeProjectAsync(job.getProject().getId(), 0);
            } finally {
                runningJobs.remove(job.getId());
            }
        }, OffsetDateTime.now().toInstant());
    }

    public void executeProject(Project project) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("Project must have an identifier");
        }
        executeProject(project.getId());
    }

    public void executeProject(Long projectId) {
        executeProjectAsync(projectId, 0);
    }

    public void executeProjectAsync(Project project, int requestedThreads) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("Project must have an identifier");
        }
        executeProjectAsync(project.getId(), requestedThreads);
    }

    public void executeProjectAsync(Long projectId, int requestedThreads) {
        testExecutor.execute(() -> executeProjectInternal(projectId, requestedThreads));
    }

    public void executeTestCase(TestCase testCase) {
        log.info("Starting test case execution: {} (ID: {})", testCase.getName(), testCase.getId());
        testExecutor.execute(() -> {
            try {
                Report report = executeCase(testCase);
                log.info("Test case {} completed with status: {}", testCase.getName(), report.getStatus());
            } catch (Exception e) {
                log.error("Failed to execute test case: {} (ID: {})", testCase.getName(), testCase.getId(), e);
                // Update test case with error information
                try {
                    testCase.setLastRunAt(OffsetDateTime.now());
                    testCase.setLastRunStatus("FAILED");
                    testCase.setLastErrorMessage("Execution error: " + e.getMessage());
                    testCaseRepository.save(testCase);
                } catch (Exception saveEx) {
                    log.error("Failed to save error state for test case {}", testCase.getId(), saveEx);
                }
            }
        });
    }

    public OffsetDateTime computeNextRun(SchedulerFrequency frequency, String cronExpression) {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (frequency) {
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.plusWeeks(1);
            case MONTHLY -> now.plusMonths(1);
            case CUSTOM -> now.plus(Duration.ofHours(1));
        };
    }

    private void executeProjectInternal(Long projectId, int requestedThreads) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        log.info("Executing tests for project {} with requested thread count {}", project.getName(), requestedThreads);
        List<TestCase> cases = testCaseRepository.findByProject(project);
        if (cases.isEmpty()) {
            log.info("Project {} has no test cases to execute", project.getName());
            return;
        }

        int effectiveThreads = determineThreadCount(requestedThreads, cases.size());
        log.debug("Effective thread count for project {} execution: {}", project.getName(), effectiveThreads);

        List<Report> reports = Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = effectiveThreads > 1
            ? Executors.newFixedThreadPool(effectiveThreads, projectThreadFactory(project.getId()))
            : null;

        try {
            Stream<TestCase> stream = cases.stream();
            if (pool != null) {
                List<CompletableFuture<Report>> futures = stream
                    .map(testCase -> CompletableFuture.supplyAsync(() -> executeCase(testCase), pool))
                    .collect(Collectors.toList());

                futures.forEach(future -> {
                    try {
                        Report report = future.join();
                        if (report != null) {
                            reports.add(report);
                        }
                    } catch (CompletionException completionException) {
                        log.error("Test case run failed", completionException.getCause());
                    }
                });
            } else {
                stream.forEach(testCase -> {
                    Report report = executeCase(testCase);
                    reports.add(report);
                });
            }
        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }

        Project persisted = projectRepository.findById(project.getId()).orElse(project);
        List<User> recipients = userRepository.findAll();
        notificationService.notifyRunCompletion(persisted, new ArrayList<>(reports), recipients);
    }

    private int determineThreadCount(int requestedThreads, int totalCases) {
        int defaultThreads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 16));
        int threads = requestedThreads > 0 ? requestedThreads : defaultThreads;
        threads = Math.min(threads, totalCases);
        return Math.max(1, Math.min(threads, 64));
    }

    private ThreadFactory projectThreadFactory(Long projectId) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("project-" + projectId + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private Report executeCase(TestCase testCase) {
        return switch (testCase.getType()) {
            case API -> apiTestExecutor.execute(testCase);
            case UI -> uiTestExecutor.execute(testCase);
        };
    }
}
