package org.automation.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.automation.ui.DriverManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestSuiteListener implements ITestListener {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/automation_tests";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "rooT@12345"; // updated
    private static final String ARTIFACTS_DIR = "artifacts/";

    private static final int MAX_US_ID_LENGTH = 50;
    private static final int MAX_TC_ID_LENGTH = 255;

    // ---------- Database Helper ----------
    private Connection getConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void insertExecutionLog(String testName, String status, String type,
                                    String usId, String tcId, String artifact, String screenshotPath) {
        usId = truncate(usId, MAX_US_ID_LENGTH);
        tcId = truncate(tcId, MAX_TC_ID_LENGTH);

        String sql = "INSERT INTO execution_log (test_name, status, test_type, us_id, tc_id, artifact, screenshot_path, execution_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testName);
            ps.setString(2, status);
            ps.setString(3, type);
            ps.setString(4, usId);
            ps.setString(5, tcId);
            ps.setString(6, artifact);
            ps.setString(7, screenshotPath);
            ps.setString(8, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column 'test_type'")) {
                System.err.println("[DB] Missing columns detected. Attempting auto-migration...");
                selfHealSchema();
                // retry once
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, testName);
                    ps.setString(2, status);
                    ps.setString(3, type);
                    ps.setString(4, usId);
                    ps.setString(5, tcId);
                    ps.setString(6, artifact);
                    ps.setString(7, screenshotPath);
                    ps.setString(8, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    ps.executeUpdate();
                    System.out.println("[DB] Insert succeeded after schema repair.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }
    }

    private void selfHealSchema() {
        String[] alters = new String[] {
                "ALTER TABLE execution_log ADD COLUMN test_type VARCHAR(20) NULL",
                "ALTER TABLE execution_log ADD COLUMN us_id VARCHAR(50) NULL",
                "ALTER TABLE execution_log ADD COLUMN tc_id VARCHAR(255) NULL",
                "ALTER TABLE execution_log ADD COLUMN artifact VARCHAR(500) NULL",
                "ALTER TABLE execution_log ADD COLUMN screenshot_path VARCHAR(500) NULL",
                "ALTER TABLE execution_log ADD COLUMN execution_time DATETIME DEFAULT CURRENT_TIMESTAMP"
        };
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String alter : alters) {
                try { st.executeUpdate(alter); } catch (SQLException ignore) { }
            }
            System.out.println("[DB] Auto-migration attempted for execution_log.");
        } catch (SQLException e) {
            System.err.println("[DB] Auto-migration failed: " + e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "N/A";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    // ---------- Screenshot Helper ----------
    private String saveScreenshot(String testName) {
        String path = "";
        try {
            Files.createDirectories(Paths.get(ARTIFACTS_DIR + "screenshots/"));
            WebDriver driver = DriverManager.getDriver();
            if (driver != null) {
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                path = ARTIFACTS_DIR + "screenshots/" + testName + "_FAILED_" + timestamp + ".png";
                Files.write(Paths.get(path), screenshotBytes);

                System.out.println("[Artifact] Screenshot saved: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    // ---------- API Artifact Helper ----------
    private String saveAPIArtifact(String testName, String request, String response) {
        String filePath = "";
        try {
            Files.createDirectories(Paths.get(ARTIFACTS_DIR + "api/"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            filePath = ARTIFACTS_DIR + "api/" + testName + "_" + timestamp + ".json";

            String content = "{\n" +
                    "  \"testName\": \"" + testName + "\",\n" +
                    "  \"timestamp\": \"" + timestamp + "\",\n" +
                    "  \"request\": \"" + request.replace("\"", "\\\"") + "\",\n" +
                    "  \"response\": \"" + response.replace("\"", "\\\"") + "\"\n" +
                    "}";

            try (FileWriter writer = new FileWriter(new File(filePath))) {
                writer.write(content);
            }
            System.out.println("[Artifact] API artifact saved: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    // ---------- Save UI Test Result ----------
    private void saveUITestResult(ITestResult result) {
        String testName = result.getName();
        String status = result.isSuccess() ? "PASS" : "FAIL";
        String usId = result.getAttribute("US_ID") != null ? result.getAttribute("US_ID").toString() : "N/A";

        String tcId = result.getAttribute("TC_ID") != null ? result.getAttribute("TC_ID").toString()
                : (result.getMethod().getDescription() != null && !result.getMethod().getDescription().isEmpty())
                ? result.getMethod().getDescription()
                : result.getMethod().getMethodName();

        tcId = truncate(tcId, MAX_TC_ID_LENGTH);
        usId = truncate(usId, MAX_US_ID_LENGTH);

        String screenshotPath = "";
        if (result.getStatus() == ITestResult.FAILURE) {
            screenshotPath = saveScreenshot(testName);
        }

        insertExecutionLog(testName, status, "UI", usId, tcId, "", screenshotPath);
    }

    // ---------- Save API Test Result ----------
    private void saveAPITestResult(ITestResult result) {
        String testName = result.getName();
        String status = result.isSuccess() ? "PASS" : "FAIL";
        String usId = result.getAttribute("US_ID") != null ? result.getAttribute("US_ID").toString() : "N/A";

        String tcId = result.getAttribute("TC_ID") != null ? result.getAttribute("TC_ID").toString()
                : (result.getMethod().getDescription() != null && !result.getMethod().getDescription().isEmpty())
                ? result.getMethod().getDescription()
                : result.getMethod().getMethodName();

        tcId = truncate(tcId, MAX_TC_ID_LENGTH);
        usId = truncate(usId, MAX_US_ID_LENGTH);

        String requestPayload = result.getAttribute("requestPayload") != null ? result.getAttribute("requestPayload").toString() : "{}";
        String responseBody = result.getAttribute("responseBody") != null ? result.getAttribute("responseBody").toString() : "{}";

        // Save API JSON artifact
        String artifactPath = saveAPIArtifact(testName, requestPayload, responseBody);

        insertExecutionLog(testName, status, "API", usId, tcId, artifactPath, null);
    }

    // ---------- TestNG Hooks ----------
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
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("Test Suite Finished: " + context.getName());
    }
}
