package org.automation.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.automation.ui.DriverManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.automation.config.ConfigManager;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestSuiteListener implements ITestListener {

    private static final String ARTIFACTS_DIR = "artifacts/";
    private static final int MAX_US_ID_LENGTH = 50;
    private static final int MAX_TC_ID_LENGTH = 255;

    private volatile boolean dbAvailable = false;
    private volatile boolean artifactsEnabled = true;

    private String getDbUrl() { return ConfigManager.getDbUrl(); }
    private String getDbUser() { return ConfigManager.getDbUser(); }
    private String getDbPass() { return ConfigManager.getDbPassword(); }

    private Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(getDbUrl(), getDbUser(), getDbPass());
    }

    private void initDbFlag() {
        if (!ConfigManager.isDbEnabled()) {
            System.out.println("[DB] Disabled via configuration");
            dbAvailable = false;
            return;
        }
        try (Connection ignored = getConnection()) {
            dbAvailable = true;
            System.out.println("[DB] Connection successful, DB logging enabled");
        } catch (Exception e) {
            dbAvailable = false;
            System.out.println("[DB] Connection failed, DB logging disabled: " + e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "N/A";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private void insertExecutionLog(String testName, String status, String type,
                                    String usId, String tcId, String artifact) {
        if (!dbAvailable) return;
        usId = truncate(usId, MAX_US_ID_LENGTH);
        tcId = truncate(tcId, MAX_TC_ID_LENGTH);
        String sql = "INSERT INTO execution_log (test_name, status, test_type, us_id, tc_id, artifact, execution_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testName);
            ps.setString(2, status);
            ps.setString(3, type);
            ps.setString(4, usId);
            ps.setString(5, tcId);
            ps.setString(6, artifact);
            ps.setString(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private String saveScreenshot(String testName) {
        if (!artifactsEnabled) return "";
        String path = "";
        try {
            Files.createDirectories(Paths.get(ARTIFACTS_DIR + "screenshots/"));
            WebDriver driver = DriverManager.getDriver();
            if (driver != null) {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                path = ARTIFACTS_DIR + "screenshots/" + sanitize(testName) + "_" + timestamp + ".png";
                Files.write(Paths.get(path), screenshotBytes);
                System.out.println("[Artifact] Screenshot saved: " + path);
            }
        } catch (Exception ignored) {}
        return path;
    }

    private String sanitize(String name) {
        return name == null ? "unknown" : name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String saveAPIArtifact(String testName, String request, String response) {
        if (!artifactsEnabled) return "";
        String filePath = "";
        try {
            Files.createDirectories(Paths.get(ARTIFACTS_DIR + "api/"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            filePath = ARTIFACTS_DIR + "api/" + sanitize(testName) + "_" + timestamp + ".json";

            String content = "{\n" +
                    "  \"testName\": \"" + escape(testName) + "\",\n" +
                    "  \"timestamp\": \"" + timestamp + "\",\n" +
                    "  \"request\": \"" + escape(request) + "\",\n" +
                    "  \"response\": \"" + escape(response) + "\"\n" +
                    "}";

            try (FileWriter writer = new FileWriter(new File(filePath))) {
                writer.write(content);
            }
            System.out.println("[Artifact] API artifact saved: " + filePath);
        } catch (Exception ignored) {}
        return filePath;
    }

    private String escape(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private void saveUITestResult(ITestResult result) {
        String testName = result.getName();
        String status = result.isSuccess() ? "PASS" : "FAIL";
        String usId = attr(result, "US_ID", "N/A");
        String tcId = deriveTcId(result);

        String artifactPath = saveScreenshot(testName);
        insertExecutionLog(testName, status, "UI", usId, tcId, artifactPath);

        if (dbAvailable) {
            String sql = "INSERT INTO ui_tests (us_id, test_case_id, name, status, execution_time, artifact) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, usId);
                ps.setString(2, tcId);
                ps.setString(3, testName);
                ps.setString(4, status);
                ps.setString(5, now());
                ps.setString(6, artifactPath);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }
    }

    private void saveAPITestResult(ITestResult result) {
        String testName = result.getName();
        String status = result.isSuccess() ? "PASS" : "FAIL";
        String usId = attr(result, "US_ID", "N/A");
        String tcId = deriveTcId(result);
        String requestPayload = attr(result, "requestPayload", "{}");
        String responseBody = attr(result, "responseBody", "{}");

        String artifactPath = saveAPIArtifact(testName, requestPayload, responseBody);
        insertExecutionLog(testName, status, "API", usId, tcId, artifactPath);

        if (dbAvailable) {
            String sql = "INSERT INTO api_responses (us_id, test_case_id, name, status, execution_time, request_payload, response_body, artifact) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, usId);
                ps.setString(2, tcId);
                ps.setString(3, testName);
                ps.setString(4, status);
                ps.setString(5, now());
                ps.setString(6, requestPayload);
                ps.setString(7, responseBody);
                ps.setString(8, artifactPath);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }
    }

    private String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }

    private String attr(ITestResult r, String key, String def) {
        Object v = r.getAttribute(key);
        if (v == null) return def;
        String s = v.toString();
        return s.isBlank() ? def : truncate(s, key.equals("US_ID") ? MAX_US_ID_LENGTH : MAX_TC_ID_LENGTH);
    }

    private String deriveTcId(ITestResult result) {
        if (result.getAttribute("TC_ID") != null) return truncate(result.getAttribute("TC_ID").toString(), MAX_TC_ID_LENGTH);
        String desc = result.getMethod().getDescription();
        if (desc != null && !desc.isBlank()) return truncate(desc, MAX_TC_ID_LENGTH);
        return truncate(result.getMethod().getMethodName(), MAX_TC_ID_LENGTH);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (result.getTestClass().getName().contains(".ui.")) saveUITestResult(result);
        else saveAPITestResult(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (result.getTestClass().getName().contains(".ui.")) saveUITestResult(result);
        else saveAPITestResult(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (result.getTestClass().getName().contains(".ui.")) saveUITestResult(result);
        else saveAPITestResult(result);
    }

    @Override
    public void onStart(ITestContext context) {
        System.out.println("Test Suite Started: " + context.getName());
        initDbFlag();
        artifactsEnabled = ConfigManager.isArtifactsEnabled();
        if (!artifactsEnabled) System.out.println("[Artifacts] Disabled via configuration");
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("Test Suite Finished: " + context.getName());
    }
}
