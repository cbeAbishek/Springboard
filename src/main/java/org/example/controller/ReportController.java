package org.example.controller;

import org.example.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportingService reportingService;

    private static final String REPORTS_DIRECTORY = "test-reports";

    @PostMapping("/generate/{batchId}")
    public ResponseEntity<String> generateAllReports(@PathVariable String batchId) {
        try {
            reportingService.generateAllReports(batchId);

            return ResponseEntity.ok("Report generation started for batch: " + batchId);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failed to start report generation: " + e.getMessage());
        }
    }

    @PostMapping("/html/{batchId}")
    public ResponseEntity<ReportingService.ReportResult> generateHtmlReport(@PathVariable String batchId) {
        try {
            ReportingService.ReportResult result = reportingService.generateHtmlReport(batchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReportInfo>> listReports() {
        try {
            List<ReportInfo> reports = new ArrayList<>();
            File reportsDir = new File(REPORTS_DIRECTORY);

            if (reportsDir.exists() && reportsDir.isDirectory()) {
                File[] files = reportsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            ReportInfo reportInfo = createReportInfo(file);
                            reports.add(reportInfo);
                        }
                    }
                }
            }

            // Sort by creation time (newest first)
            reports.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(REPORTS_DIRECTORY, filename);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                if (filename.endsWith(".html")) {
                    contentType = "text/html";
                } else if (filename.endsWith(".csv")) {
                    contentType = "text/csv";
                } else if (filename.endsWith(".xml")) {
                    contentType = "application/xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> viewReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(REPORTS_DIRECTORY, filename);
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                if (filename.endsWith(".html")) {
                    contentType = "text/html";
                } else if (filename.endsWith(".csv")) {
                    contentType = "text/csv";
                } else if (filename.endsWith(".xml")) {
                    contentType = "application/xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteReport(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(REPORTS_DIRECTORY, filename);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            if (file.delete()) {
                return ResponseEntity.ok("Report deleted successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to delete report");
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting report: " + e.getMessage());
        }
    }

    private ReportInfo createReportInfo(File file) {
        ReportInfo info = new ReportInfo();
        info.setFilename(file.getName());
        info.setSize(file.length());
        info.setCreatedAt(LocalDateTime.ofEpochSecond(file.lastModified() / 1000, 0, java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now())));

        // Extract batch ID from filename (assuming format: TestReport_{batchId}_{timestamp}.{ext})
        String filename = file.getName();
        if (filename.startsWith("TestReport_") && filename.contains("_")) {
            String[] parts = filename.split("_");
            if (parts.length >= 2) {
                info.setBatchId(parts[1]);
            }
        }

        // Determine report type
        if (filename.endsWith(".html")) {
            info.setType("HTML");
        } else if (filename.endsWith(".csv")) {
            info.setType("CSV");
        } else if (filename.endsWith(".xml")) {
            info.setType("XML");
        } else {
            info.setType("Unknown");
        }

        return info;
    }

    public static class ReportInfo {
        private String filename;
        private String batchId;
        private String type;
        private long size;
        private LocalDateTime createdAt;

        // Getters and setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            else return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }

        public String getFormattedCreatedAt() {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
