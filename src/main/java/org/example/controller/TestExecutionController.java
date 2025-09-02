package org.example.controller;

import org.example.model.TestBatch;
import org.example.model.TestExecution;
import org.example.service.TestExecutionService;
import org.example.repository.TestBatchRepository;
import org.example.repository.TestExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/execution")
@CrossOrigin(origins = "*")
public class TestExecutionController {

    @Autowired
    private TestExecutionService testExecutionService;

    @Autowired
    private TestBatchRepository testBatchRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @PostMapping("/batch")
    public ResponseEntity<BatchResponse> executeBatch(@RequestBody BatchRequest request) {
        try {
            // Create and save the batch first to get a persistent ID
            TestBatch batch = new TestBatch();
            batch.setBatchId(java.util.UUID.randomUUID().toString());
            batch.setBatchName("Batch_" + request.getTestSuite() + "_" + java.time.LocalDateTime.now());
            batch.setStatus(TestBatch.BatchStatus.SCHEDULED);
            batch.setEnvironment(request.getEnvironment());
            batch.setParallelThreads(request.getParallelThreads());
            batch = testBatchRepository.save(batch);

            // Asynchronously execute the batch
            testExecutionService.executeBatch(
                    batch,
                    request.getTestSuite(),
                    request.getEnvironment(),
                    request.getParallelThreads()
            );

            // Respond with the actual batch ID
            BatchResponse response = new BatchResponse();
            response.setBatchId(batch.getBatchId());
            response.setStatus("STARTED");
            response.setMessage("Batch execution started successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BatchResponse errorResponse = new BatchResponse();
            errorResponse.setMessage("Failed to start batch execution: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/single/{testCaseId}")
    public ResponseEntity<TestExecution> executeSingleTest(
            @PathVariable Long testCaseId,
            @RequestParam String environment) {
        try {
            TestExecution execution = testExecutionService.executeSingleTest(testCaseId, environment);
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<TestBatch> getBatchStatus(@PathVariable String batchId) {
        return testBatchRepository.findByBatchId(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batch/{batchId}/executions")
    public ResponseEntity<List<TestExecution>> getBatchExecutions(@PathVariable String batchId) {
        return testBatchRepository.findByBatchId(batchId)
                .map(batch -> ResponseEntity.ok(batch.getTestExecutions()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/batches")
    public ResponseEntity<List<TestBatch>> getAllBatches() {
        List<TestBatch> batches = testBatchRepository.findAllOrderByCreatedAtDesc();
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/batches/recent")
    public ResponseEntity<List<TestBatch>> getRecentBatches(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<TestBatch> recentBatches;

            if (limit > 0 && limit <= 100) {
                recentBatches = testBatchRepository.findTopRecentBatches(limit);
            } else {
                recentBatches = testBatchRepository.findRecentBatches(since);
            }

            return ResponseEntity.ok(recentBatches);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/batches/status/{status}")
    public ResponseEntity<List<TestBatch>> getBatchesByStatus(@PathVariable String status) {
        try {
            TestBatch.BatchStatus batchStatus = TestBatch.BatchStatus.valueOf(status.toUpperCase());
            List<TestBatch> batches = testBatchRepository.findByStatusOrderByCreatedAtDesc(batchStatus);
            return ResponseEntity.ok(batches);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/batches/active")
    public ResponseEntity<List<TestBatch>> getActiveBatches() {
        List<TestBatch> activeBatches = testBatchRepository.findActiveBatches();
        return ResponseEntity.ok(activeBatches);
    }

    @GetMapping("/executions")
    public ResponseEntity<List<TestExecution>> getAllExecutions() {
        List<TestExecution> executions = testExecutionRepository.findAll();
        return ResponseEntity.ok(executions);
    }

    @Data
    public static class BatchRequest {
        private String testSuite;
        private String environment;
        private int parallelThreads = 1;

        public String getTestSuite() { return testSuite; }
        public void setTestSuite(String testSuite) { this.testSuite = testSuite; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public int getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }
    }

    @Data
    public static class BatchResponse {
        private String batchId;
        private String status;
        private String message;

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
