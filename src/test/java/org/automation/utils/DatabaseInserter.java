package org.automation.utils;

import java.sql.*;

public class DatabaseInserter {

    // Using H2 embedded database (no MySQL needed!)
    private static final String DB_URL = "jdbc:h2:file:./data/automation_framework";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    // ---------- Insert UI Test Result ----------
    public static void insertUiTestResult(String usId, String testCaseId, String name,
                                          String status, long durationMs, String artifact) {
        String sql = "INSERT INTO ui_tests (us_id, test_case_id, name, status, execution_time, duration_ms, artifact) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usId);
            stmt.setString(2, testCaseId);
            stmt.setString(3, name);
            stmt.setString(4, status);
            stmt.setLong(5, durationMs);
            stmt.setString(6, artifact);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Insert API Test Result ----------
    public static void insertApiTestResult(String usId, String testCaseId, String name,
                                           String status, long durationMs, String request, String response, String artifact) {
        String sql = "INSERT INTO api_responses (us_id, test_case_id, name, status, execution_time, duration_ms, request, response, artifact) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usId);
            stmt.setString(2, testCaseId);
            stmt.setString(3, name);
            stmt.setString(4, status);
            stmt.setLong(5, durationMs);
            stmt.setString(6, request);
            stmt.setString(7, response);
            stmt.setString(8, artifact);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Insert Execution Log ----------
    public static void insertExecutionLog(String testType, String usId, String testCaseId,
                                          String message, String level, String screenshotPath,
                                          Timestamp startTime, Timestamp endTime, long duration) {
        String sql = "INSERT INTO execution_logs " +
                "(test_type, us_id, test_case_id, message, level, log_time, tc_id, screenshot_path, start_time, end_time, duration) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, testType);
            stmt.setString(2, usId);
            stmt.setString(3, testCaseId);
            stmt.setString(4, message);
            stmt.setString(5, level);
            stmt.setString(6, testCaseId); // tc_id
            stmt.setString(7, screenshotPath);
            stmt.setTimestamp(8, startTime);
            stmt.setTimestamp(9, endTime);
            stmt.setLong(10, duration);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Insert General Test Result (used by TestListener) ----------
    public static void insertTestResult(String className, String testName, String status,
                                      String timestamp, long duration, String errorMessage, String screenshotPath) {
        String sql = "INSERT INTO test_results (class_name, test_name, status, timestamp, duration_ms, error_message, screenshot_path) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, className);
            stmt.setString(2, testName);
            stmt.setString(3, status);
            stmt.setString(4, timestamp);
            stmt.setLong(5, duration);
            stmt.setString(6, errorMessage);
            stmt.setString(7, screenshotPath);
            stmt.executeUpdate();

        } catch (SQLException e) {
            // If database connection fails, log to console instead
            System.err.println("Failed to insert test result to database: " + e.getMessage());
            System.out.println("Test Result - Class: " + className + ", Test: " + testName +
                             ", Status: " + status + ", Duration: " + duration + "ms");
        }
    }
}
