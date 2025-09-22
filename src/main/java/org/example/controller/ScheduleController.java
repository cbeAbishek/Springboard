package org.example.controller;

import org.example.model.TestSchedule;
import org.example.repository.TestScheduleRepository;
import org.example.scheduler.TestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import org.quartz.CronExpression;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private TestScheduleRepository scheduleRepository;

    @Autowired
    private TestScheduler testScheduler;

    @GetMapping
    public ResponseEntity<List<ScheduleDTO>> getAllSchedules() {
        List<ScheduleDTO> schedules = scheduleRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ScheduleDTO>> getActiveSchedules() {
        List<ScheduleDTO> schedules = scheduleRepository.findByIsActiveTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(schedules);
    }

    @PostMapping
    public ResponseEntity<Map<String,Object>> createSchedule(@RequestBody ScheduleRequest request) {
        // Manual validation to allow flexible request class reuse
        if (isBlank(request.getScheduleName()) || isBlank(request.getCronExpression())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "scheduleName and cronExpression are required"
            ));
        }
        if (!CronExpression.isValidExpression(request.getCronExpression())) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid cron expression: " + request.getCronExpression()
            ));
        }
        TestSchedule schedule = new TestSchedule();
        schedule.setScheduleName(request.getScheduleName());
        schedule.setCronExpression(request.getCronExpression());
        schedule.setTestSuite(request.getTestSuite());
        schedule.setEnvironment(request.getEnvironment());
        schedule.setParallelThreads(request.getParallelThreads() != null ? request.getParallelThreads() : 1);
        schedule.setIsActive(request.getEnabled() == null ? true : request.getEnabled());
        TestSchedule savedSchedule = scheduleRepository.save(schedule);
        testScheduler.scheduleTest(savedSchedule);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "schedule", toDTO(savedSchedule)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String,Object>> updateSchedule(@PathVariable Long id, @RequestBody ScheduleRequest request) {
        Optional<TestSchedule> opt = scheduleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Schedule not found"
            ));
        }
        TestSchedule schedule = opt.get();

        boolean hasName = !isBlank(request.getScheduleName());
        boolean hasCron = !isBlank(request.getCronExpression());
        boolean hasAnyConfigField = hasName || hasCron || request.getTestSuite() != null || request.getEnvironment() != null || request.getParallelThreads() != null;
        boolean enabledPresent = request.getEnabled() != null;

        // If legacy toggle call (only enabled passed) allow partial update without validation
        if (enabledPresent && !hasAnyConfigField) {
            schedule.setIsActive(request.getEnabled());
            TestSchedule saved = scheduleRepository.save(schedule);
            if (request.getEnabled()) {
                testScheduler.scheduleTest(saved);
            } else {
                testScheduler.unscheduleTest(saved.getId());
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "schedule", toDTO(saved),
                "message", "Schedule " + (request.getEnabled() ? "enabled" : "disabled")
            ));
        }

        // If any of name/cron provided, validate both must be non-blank
        if (hasName ^ hasCron) { // XOR -> only one provided
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Both scheduleName and cronExpression must be provided together when updating schedule definition"
            ));
        }
        if (hasName && hasCron) {
            if (!CronExpression.isValidExpression(request.getCronExpression())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid cron expression: " + request.getCronExpression()
                ));
            }
            schedule.setScheduleName(request.getScheduleName());
            schedule.setCronExpression(request.getCronExpression());
        }
        if (request.getTestSuite() != null) schedule.setTestSuite(request.getTestSuite());
        if (request.getEnvironment() != null) schedule.setEnvironment(request.getEnvironment());
        if (request.getParallelThreads() != null) schedule.setParallelThreads(request.getParallelThreads());
        if (enabledPresent) schedule.setIsActive(request.getEnabled());

        TestSchedule updated = scheduleRepository.save(schedule);
        if (enabledPresent && !request.getEnabled()) {
            testScheduler.unscheduleTest(updated.getId());
        } else {
            // Reschedule if definition changed (name/cron) or still active
            if (hasName && hasCron || Boolean.TRUE.equals(updated.getIsActive())) {
                try { testScheduler.rescheduleTest(updated); } catch (Exception ignore) { /* fallback if not active */ }
            }
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "schedule", toDTO(updated)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        Optional<TestSchedule> schedule = scheduleRepository.findById(id);
        if (schedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Unschedule the job
        testScheduler.unscheduleTest(id);

        TestSchedule s = schedule.get();
        s.setIsActive(false);
        scheduleRepository.save(s);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<TestSchedule> activateSchedule(@PathVariable Long id) {
        Optional<TestSchedule> optionalSchedule = scheduleRepository.findById(id);
        if (optionalSchedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TestSchedule schedule = optionalSchedule.get();
        schedule.setIsActive(true);
        TestSchedule updatedSchedule = scheduleRepository.save(schedule);

        testScheduler.scheduleTest(updatedSchedule);

        return ResponseEntity.ok(updatedSchedule);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<TestSchedule> deactivateSchedule(@PathVariable Long id) {
        Optional<TestSchedule> optionalSchedule = scheduleRepository.findById(id);
        if (optionalSchedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TestSchedule schedule = optionalSchedule.get();
        schedule.setIsActive(false);
        TestSchedule updatedSchedule = scheduleRepository.save(schedule);

        testScheduler.unscheduleTest(id);

        return ResponseEntity.ok(updatedSchedule);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<?> previewNextExecutions(@PathVariable Long id, @RequestParam(defaultValue = "5") int count) {
        return scheduleRepository.findById(id)
            .map(s -> ResponseEntity.ok(testScheduler.previewNextExecutions(s.getCronExpression(), Math.min(Math.max(count,1),20))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<?> triggerNow(@PathVariable Long id) {
        if (scheduleRepository.existsById(id)) {
            testScheduler.triggerNow(id);
            return ResponseEntity.ok(Map.of("status","TRIGGERED","scheduleId", id));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/next")
    public ResponseEntity<?> listUpcoming() {
        List<Map<String,Object>> data = scheduleRepository.findByIsActiveTrue().stream()
            .map(s -> Map.<String,Object>of(
                "id", s.getId(),
                "name", s.getScheduleName(),
                "cron", s.getCronExpression(),
                "nextExecution", s.getNextExecution(),
                "lastExecution", s.getLastExecution(),
                "testSuite", s.getTestSuite(),
                "environment", s.getEnvironment()
            )).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateScheduleStatus(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        return scheduleRepository.findById(id).map(schedule -> {
            Object enabledObj = body.get("enabled");
            if (enabledObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing 'enabled' field"
                ));
            }
            boolean enable = Boolean.parseBoolean(enabledObj.toString());
            schedule.setIsActive(enable);
            TestSchedule saved = scheduleRepository.save(schedule);
            if (enable) {
                testScheduler.scheduleTest(saved);
            } else {
                testScheduler.unscheduleTest(saved.getId());
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "schedule", toDTO(saved)
            ));
        }).orElse(ResponseEntity.status(404).body(Map.of(
            "success", false,
            "message", "Schedule not found"
        )));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeNow(@PathVariable Long id) {
        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Schedule not found"
            ));
        }
        testScheduler.triggerNow(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Schedule triggered",
            "scheduleId", id
        ));
    }

    private ScheduleDTO toDTO(TestSchedule s) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(s.getId());
        dto.setScheduleName(s.getScheduleName());
        dto.setCronExpression(s.getCronExpression());
        dto.setTestSuite(s.getTestSuite());
        dto.setEnvironment(s.getEnvironment());
        dto.setEnabled(Boolean.TRUE.equals(s.getIsActive()));
        dto.setParallelThreads(s.getParallelThreads());
        dto.setNextExecutionTime(s.getNextExecution());
        dto.setLastExecutionTime(s.getLastExecution());
        return dto;
    }

    @Data
    public static class ScheduleDTO {
        private Long id;
        private String scheduleName;
        private String cronExpression;
        private String testSuite;
        private String environment;
        private boolean enabled;
        private Integer parallelThreads;
        private LocalDateTime nextExecutionTime;
        private LocalDateTime lastExecutionTime;
    }

    private boolean isBlank(String s) { return s == null || s.isEmpty() || s.isBlank(); }

    @Data
    public static class ScheduleRequest {
        private String scheduleName;
        private String cronExpression;
        private String testSuite;
        private String environment;
        private Integer parallelThreads;
        private Boolean enabled;

        // Getters and setters
        public String getScheduleName() { return scheduleName; }
        public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }

        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

        public String getTestSuite() { return testSuite; }
        public void setTestSuite(String testSuite) { this.testSuite = testSuite; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public Integer getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(Integer parallelThreads) { this.parallelThreads = parallelThreads; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}
