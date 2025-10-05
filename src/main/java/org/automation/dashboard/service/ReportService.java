package org.automation.dashboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing test reports
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private static final String REPORTS_DIR = "artifacts/reports";
    private static final String ALLURE_DIR = "allure-results";
    private static final String SCREENSHOTS_DIR = "artifacts/screenshots";

    /**
     * Get all available reports
     */
    public List<Map<String, Object>> getAllReports() {
        logger.info("Fetching all available reports");
        List<Map<String, Object>> reports = new ArrayList<>();

        try {
            Path reportsPath = Paths.get(REPORTS_DIR);
            if (Files.exists(reportsPath)) {
                Files.walk(reportsPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            reports.add(createReportInfo(path));
                        } catch (IOException e) {
                            logger.error("Error reading report file: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            logger.error("Error reading reports directory: {}", e.getMessage(), e);
        }

        // Sort by creation time (newest first)
        reports.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("createdTime");
            LocalDateTime timeB = (LocalDateTime) b.get("createdTime");
            return timeB.compareTo(timeA);
        });

        logger.info("Found {} reports", reports.size());
        return reports;
    }

    /**
     * Get reports by type
     */
    public List<Map<String, Object>> getReportsByType(String type) {
        logger.info("Fetching reports of type: {}", type);
        return getAllReports().stream()
            .filter(report -> type.equals(report.get("type")))
            .collect(Collectors.toList());
    }

    /**
     * Get report statistics
     */
    public Map<String, Object> getReportStatistics() {
        logger.info("Calculating report statistics");

        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> allReports = getAllReports();

        stats.put("totalReports", allReports.size());
        stats.put("htmlReports", countByType(allReports, "html"));
        stats.put("xmlReports", countByType(allReports, "xml"));
        stats.put("jsonReports", countByType(allReports, "json"));
        stats.put("pdfReports", countByType(allReports, "pdf"));

        // Calculate total size
        long totalSize = allReports.stream()
            .mapToLong(report -> (Long) report.getOrDefault("size", 0L))
            .sum();
        stats.put("totalSize", totalSize);
        stats.put("totalSizeFormatted", formatFileSize(totalSize));

        // Recent reports (last 7 days)
        long recentCount = allReports.stream()
            .filter(report -> {
                LocalDateTime created = (LocalDateTime) report.get("createdTime");
                return created.isAfter(LocalDateTime.now().minusDays(7));
            })
            .count();
        stats.put("recentReports", recentCount);

        logger.info("Report statistics calculated: {} total reports, {} MB total size",
                   allReports.size(), totalSize / (1024 * 1024));

        return stats;
    }

    /**
     * Get screenshots for a specific test
     */
    public List<Map<String, Object>> getScreenshotsForTest(String testName) {
        logger.info("Fetching screenshots for test: {}", testName);

        List<Map<String, Object>> screenshots = new ArrayList<>();
        Path screenshotsPath = Paths.get(SCREENSHOTS_DIR);

        try {
            if (Files.exists(screenshotsPath)) {
                Files.walk(screenshotsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(testName) ||
                                   path.toString().contains(testName))
                    .forEach(path -> {
                        try {
                            Map<String, Object> screenshot = new HashMap<>();
                            String fileName = path.getFileName().toString();
                            screenshot.put("name", fileName);
                            screenshot.put("path", path.toString());
                            screenshot.put("relativePath", screenshotsPath.relativize(path).toString());
                            // Generate correct URL for web access
                            screenshot.put("url", "/screenshots/" + fileName);

                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            screenshot.put("size", attrs.size());
                            screenshot.put("sizeFormatted", formatFileSize(attrs.size()));
                            screenshot.put("createdTime", LocalDateTime.ofInstant(
                                attrs.creationTime().toInstant(), ZoneId.systemDefault()));

                            screenshots.add(screenshot);
                            logger.debug("Added screenshot: {} with URL: /screenshots/{}", fileName, fileName);
                        } catch (IOException e) {
                            logger.error("Error reading screenshot file: {}", path, e);
                        }
                    });
            } else {
                logger.warn("Screenshots directory does not exist: {}", screenshotsPath);
            }
        } catch (IOException e) {
            logger.error("Error reading screenshots directory: {}", e.getMessage(), e);
        }

        logger.info("Found {} screenshots for test {}", screenshots.size(), testName);
        return screenshots;
    }

    /**
     * Get latest screenshot for a test
     */
    public String getLatestScreenshot(String testName) {
        logger.debug("Fetching latest screenshot for test: {}", testName);

        List<Map<String, Object>> screenshots = getScreenshotsForTest(testName);
        if (!screenshots.isEmpty()) {
            // Sort by creation time and get the latest
            screenshots.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("createdTime");
                LocalDateTime timeB = (LocalDateTime) b.get("createdTime");
                return timeB.compareTo(timeA);
            });
            return (String) screenshots.get(0).get("url");
        }

        return null;
    }

    /**
     * Delete old reports (cleanup)
     */
    public int cleanupOldReports(int daysToKeep) {
        logger.info("Cleaning up reports older than {} days", daysToKeep);

        int deletedCount = 0;
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        try {
            Path reportsPath = Paths.get(REPORTS_DIR);
            if (Files.exists(reportsPath)) {
                List<Path> filesToDelete = Files.walk(reportsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            LocalDateTime created = LocalDateTime.ofInstant(
                                attrs.creationTime().toInstant(), ZoneId.systemDefault());
                            return created.isBefore(cutoffDate);
                        } catch (IOException e) {
                            logger.error("Error checking file date: {}", path, e);
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

                for (Path file : filesToDelete) {
                    try {
                        Files.delete(file);
                        deletedCount++;
                        logger.debug("Deleted old report: {}", file);
                    } catch (IOException e) {
                        logger.error("Error deleting report: {}", file, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }

        logger.info("Cleanup completed: {} reports deleted", deletedCount);
        return deletedCount;
    }

    /**
     * Create report information map
     */
    private Map<String, Object> createReportInfo(Path path) throws IOException {
        Map<String, Object> info = new HashMap<>();

        String fileName = path.getFileName().toString();
        info.put("name", fileName);
        info.put("path", path.toString());
        info.put("type", getFileType(fileName));

        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        info.put("size", attrs.size());
        info.put("sizeFormatted", formatFileSize(attrs.size()));
        info.put("createdTime", LocalDateTime.ofInstant(
            attrs.creationTime().toInstant(), ZoneId.systemDefault()));
        info.put("modifiedTime", LocalDateTime.ofInstant(
            attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()));

        // Extract report metadata from filename if possible
        info.put("suite", extractSuiteFromFilename(fileName));
        info.put("status", extractStatusFromFilename(fileName));

        return info;
    }

    /**
     * Get file type from extension
     */
    private String getFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * Extract suite name from filename
     */
    private String extractSuiteFromFilename(String fileName) {
        // Example: UI_Test_Report_2024-01-01.html -> UI
        if (fileName.contains("UI") || fileName.contains("ui")) {
            return "ui";
        } else if (fileName.contains("API") || fileName.contains("api")) {
            return "api";
        } else if (fileName.contains("Integration") || fileName.contains("integration")) {
            return "integration";
        }
        return "unknown";
    }

    /**
     * Extract status from filename
     */
    private String extractStatusFromFilename(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("success") || lower.contains("pass")) {
            return "success";
        } else if (lower.contains("fail") || lower.contains("error")) {
            return "failed";
        }
        return "unknown";
    }

    /**
     * Count reports by type
     */
    private long countByType(List<Map<String, Object>> reports, String type) {
        return reports.stream()
            .filter(report -> type.equals(report.get("type")))
            .count();
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Ensure reports directory exists
     */
    public void ensureReportsDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));
            Files.createDirectories(Paths.get(SCREENSHOTS_DIR));
            Files.createDirectories(Paths.get(ALLURE_DIR));
            logger.info("Reports directories ensured");
        } catch (IOException e) {
            logger.error("Error creating reports directories: {}", e.getMessage(), e);
        }
    }
}
