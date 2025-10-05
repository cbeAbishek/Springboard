package org.automation.reports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the reporting system
 */
@Configuration
@ConfigurationProperties(prefix = "reports")
public class ReportConfig {

    private String baseDirectory = "artifacts/reports";
    private int retentionDays = 90;
    private boolean autoCleanup = false;
    private int maxScreenshotSize = 2048; // KB
    private String screenshotFormat = "png";

    // Getters and Setters
    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public boolean isAutoCleanup() {
        return autoCleanup;
    }

    public void setAutoCleanup(boolean autoCleanup) {
        this.autoCleanup = autoCleanup;
    }

    public int getMaxScreenshotSize() {
        return maxScreenshotSize;
    }

    public void setMaxScreenshotSize(int maxScreenshotSize) {
        this.maxScreenshotSize = maxScreenshotSize;
    }

    public String getScreenshotFormat() {
        return screenshotFormat;
    }

    public void setScreenshotFormat(String screenshotFormat) {
        this.screenshotFormat = screenshotFormat;
    }
}

