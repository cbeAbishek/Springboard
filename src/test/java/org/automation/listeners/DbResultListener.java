package org.automation.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.sql.*;

public class DbResultListener implements ITestListener {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/automation_tests?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "rooT@12345"; // updated

    private Connection getConn() throws SQLException { return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS); }

    @Override public void onTestSuccess(ITestResult result) { persist(result, "PASS"); }
    @Override public void onTestFailure(ITestResult result) { persist(result, "FAIL"); }
    @Override public void onTestSkipped(ITestResult result) { persist(result, "SKIPPED"); }

    private void persist(ITestResult result, String status) {
        String insert = "INSERT INTO execution_log (test_name, status, test_type, us_id, tc_id, artifact, screenshot_path, execution_time, message, level, log_time, start_time, end_time, duration) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, NOW(), ?, ?, ?)";

        String testType = result.getTestContext().getSuite().getName().contains("UI") ? "UI" : "API";
        String usId = attr(result, "usId");
        String testCaseId = attr(result, "testCaseId");
        String message = result.getThrowable() != null ? result.getThrowable().toString() : "Test " + status;
        String level = status.equals("FAIL") ? "ERROR" : "INFO";
        String testName = result.getMethod().getMethodName();
        String screenshotPath = attr(result, "screenshotPath");
        Timestamp startTime = new Timestamp(result.getStartMillis());
        Timestamp endTime = new Timestamp(result.getEndMillis());
        long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());

        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, testName);
            ps.setString(2, status);
            ps.setString(3, testType);
            ps.setString(4, usId);
            ps.setString(5, testCaseId);
            ps.setString(6, null); // artifact not captured here
            ps.setString(7, screenshotPath);
            ps.setString(8, message);
            ps.setString(9, level);
            ps.setTimestamp(10, startTime);
            ps.setTimestamp(11, endTime);
            ps.setLong(12, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to persist test result to DB: " + e.getMessage());
        }
    }

    private String attr(ITestResult r, String key) { return r.getAttribute(key) != null ? r.getAttribute(key).toString() : null; }

    @Override public void onStart(ITestContext context) { }
    @Override public void onFinish(ITestContext context) { }
    @Override public void onTestStart(ITestResult result) { }
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }
}
