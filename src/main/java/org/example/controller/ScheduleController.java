package org.example.controller;

import org.example.model.TestSchedule;
import org.example.scheduler.TestScheduler;
import org.example.repository.TestScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private TestScheduler testScheduler;

    @Autowired
    private TestScheduleRepository scheduleRepository;

    /**
     * Schedule BlazeDemo tests
     */
    @PostMapping("/blazedemo")
    public ResponseEntity<Map<String, Object>> scheduleBlazeDemo(@RequestBody ScheduleRequest request) {
        try {
            log.info("Scheduling BlazeDemo tests with cron: {} for environment: {}",
                    request.getCronExpression(), request.getEnvironment());

            TestSchedule schedule = testScheduler.scheduleBlazeDemo(
                    request.getCronExpression(),
                    request.getEnvironment(),
                    request.isParallel()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", schedule.getId());
            response.put("scheduleName", schedule.getName());
            response.put("nextExecution", schedule.getNextExecution());
            response.put("message", "BlazeDemo tests scheduled successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to schedule BlazeDemo tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Schedule ReqRes API tests
     */
    @PostMapping("/reqres")
    public ResponseEntity<Map<String, Object>> scheduleReqRes(@RequestBody ScheduleRequest request) {
        try {
            log.info("Scheduling ReqRes API tests with cron: {} for environment: {}",
                    request.getCronExpression(), request.getEnvironment());

            TestSchedule schedule = testScheduler.scheduleReqRes(
                    request.getCronExpression(),
                    request.getEnvironment(),
                    request.isParallel()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", schedule.getId());
            response.put("scheduleName", schedule.getName());
            response.put("nextExecution", schedule.getNextExecution());
            response.put("message", "ReqRes API tests scheduled successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to schedule ReqRes API tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Schedule regression tests
     */
    @PostMapping("/regression")
    public ResponseEntity<Map<String, Object>> scheduleRegression(@RequestBody ScheduleRequest request) {
        try {
            log.info("Scheduling regression tests with cron: {} for environment: {}",
                    request.getCronExpression(), request.getEnvironment());

            TestSchedule schedule = testScheduler.scheduleRegression(
                    request.getCronExpression(),
                    request.getEnvironment()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", schedule.getId());
            response.put("scheduleName", schedule.getName());
            response.put("nextExecution", schedule.getNextExecution());
            response.put("message", "Regression tests scheduled successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to schedule regression tests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all active schedules
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveSchedules() {
        try {
            List<TestSchedule> schedules = testScheduler.getActiveSchedules();
            List<Map<String, Object>> response = schedules.stream()
                    .map(this::convertToMap)
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get active schedules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get schedule by ID
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> getSchedule(@PathVariable Long scheduleId) {
        try {
            return scheduleRepository.findById(scheduleId)
                    .map(schedule -> {
                        Map<String, Object> response = convertToMap(schedule);
                        response.put("status", testScheduler.getScheduleStatus(scheduleId));
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Failed to get schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pause a schedule
     */
    @PostMapping("/{scheduleId}/pause")
    public ResponseEntity<Map<String, Object>> pauseSchedule(@PathVariable Long scheduleId) {
        try {
            testScheduler.pauseSchedule(scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", scheduleId);
            response.put("message", "Schedule paused successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to pause schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Resume a schedule
     */
    @PostMapping("/{scheduleId}/resume")
    public ResponseEntity<Map<String, Object>> resumeSchedule(@PathVariable Long scheduleId) {
        try {
            testScheduler.resumeSchedule(scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", scheduleId);
            response.put("message", "Schedule resumed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to resume schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Execute schedule immediately
     */
    @PostMapping("/{scheduleId}/execute")
    public ResponseEntity<Map<String, Object>> executeNow(@PathVariable Long scheduleId) {
        try {
            testScheduler.executeNow(scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", scheduleId);
            response.put("message", "Schedule executed immediately");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to execute schedule immediately", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Delete a schedule
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Long scheduleId) {
        try {
            // First unschedule the job
            testScheduler.unscheduleTest(scheduleId);

            // Then delete from database
            scheduleRepository.deleteById(scheduleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scheduleId", scheduleId);
            response.put("message", "Schedule deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update a schedule
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody TestSchedule updatedSchedule) {

        try {
            return scheduleRepository.findById(scheduleId)
                    .map(existingSchedule -> {
                        // Update fields
                        existingSchedule.setName(updatedSchedule.getName());
                        existingSchedule.setDescription(updatedSchedule.getDescription());
                        existingSchedule.setCronExpression(updatedSchedule.getCronExpression());
                        existingSchedule.setEnvironment(updatedSchedule.getEnvironment());
                        existingSchedule.setIsActive(updatedSchedule.getIsActive());
                        existingSchedule.setParallelExecution(updatedSchedule.getParallelExecution());
                        existingSchedule.setMaxRetries(updatedSchedule.getMaxRetries());
                        existingSchedule.setTimeoutMinutes(updatedSchedule.getTimeoutMinutes());
                        existingSchedule.setUpdatedAt(LocalDateTime.now());

                        // Save to database
                        TestSchedule savedSchedule = scheduleRepository.save(existingSchedule);

                        // Update scheduler
                        testScheduler.updateSchedule(savedSchedule);

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("scheduleId", savedSchedule.getId());
                        response.put("message", "Schedule updated successfully");

                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Failed to update schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Convert TestSchedule to Map for JSON response
     */
    private Map<String, Object> convertToMap(TestSchedule schedule) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", schedule.getId());
        map.put("name", schedule.getName());
        map.put("description", schedule.getDescription());
        map.put("cronExpression", schedule.getCronExpression());
        map.put("testType", schedule.getTestType());
        map.put("environment", schedule.getEnvironment());
        map.put("isActive", schedule.getIsActive());
        map.put("parallelExecution", schedule.getParallelExecution());
        map.put("maxRetries", schedule.getMaxRetries());
        map.put("timeoutMinutes", schedule.getTimeoutMinutes());
        map.put("createdAt", schedule.getCreatedAt());
        map.put("updatedAt", schedule.getUpdatedAt());
        map.put("lastExecution", schedule.getLastExecution());
        map.put("nextExecution", schedule.getNextExecution());
        map.put("createdBy", schedule.getCreatedBy());
        return map;
    }

    // Request DTO
    public static class ScheduleRequest {
        private String cronExpression;
        private String environment = "dev";
        private boolean parallel = false;

        // Getters and setters
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
    }
}
