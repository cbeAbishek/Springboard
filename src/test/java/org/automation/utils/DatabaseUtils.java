package org.automation.utils;

import org.automation.config.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseUtils {

    public static void insertTestResult(String testId, String testName, String status, String artifactPath) {
        String query = "INSERT INTO execution_logs (test_id, test_name, status, artifact) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, testId);
            ps.setString(2, testName);
            ps.setString(3, status);
            ps.setString(4, artifactPath);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Swallow exception quietly to avoid breaking test execution when DB is unavailable
            e.printStackTrace();
        }
    }
}
