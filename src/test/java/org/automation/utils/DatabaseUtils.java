package org.automation.utils;

import java.sql.*;

public class DatabaseUtils {

    // Fetch DB details from environment variables (fallback to defaults)
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/automation_tests");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "rooT@12345"); // updated

    // ---------- Get Connection ----------
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ---------- Insert UI Test ----------
    public static void insertUiTest(String usId, String testCaseId, String name,
                                    String status, long durationMs, String artifact) {
        String sql = "INSERT INTO ui_tests (us_id, test_case_id, name, status, execution_time, duration_ms, artifact) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (Connection conn = getConnection();
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

    // ---------- Insert API Test ----------
    public static void insertApiTest(String usId, String testCaseId, String name,
                                     String status, long durationMs, String request, String response, String artifact) {
        String sql = "INSERT INTO api_responses (us_id, test_case_id, name, status, execution_time, duration_ms, request, response, artifact) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)";
        try (Connection conn = getConnection();
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
                                          String testName, String status, String artifactPath, String screenshotPath) {
        String sql = "INSERT INTO execution_log (test_name, status, test_type, us_id, tc_id, artifact, screenshot_path, execution_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, testName);
            stmt.setString(2, status);
            stmt.setString(3, testType);
            stmt.setString(4, usId);
            stmt.setString(5, testCaseId);
            stmt.setString(6, artifactPath);
            stmt.setString(7, screenshotPath);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Update Screenshot Path ----------
    public static void updateScreenshotPath(String testName, String screenshotPath) {
        String sql = "UPDATE execution_log SET screenshot_path = ? WHERE test_name = ? ORDER BY execution_time DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, screenshotPath);
            stmt.setString(2, testName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Clear All Tables ----------
    public static void clearAllTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("TRUNCATE TABLE ui_tests");
            stmt.executeUpdate("TRUNCATE TABLE api_responses");
            stmt.executeUpdate("TRUNCATE TABLE execution_log");

            System.out.println("✅ All tables cleared successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Check if Screenshot Exists ----------
    public static boolean screenshotExists(String usId, String testCaseId) {
        String sql = "SELECT screenshot_path FROM execution_log WHERE us_id = ? AND tc_id = ? AND screenshot_path IS NOT NULL";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usId);
            stmt.setString(2, testCaseId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
