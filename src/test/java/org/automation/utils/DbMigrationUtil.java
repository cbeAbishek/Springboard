package org.automation.utils;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Ensures the database schema required by the automation framework exists.
 * Creates or alters the execution_log table so that UI test listeners can insert rows safely.
 */
public class DbMigrationUtil {

    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/automation_tests");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "rooT@12345");

    private static final String TABLE_NAME = "execution_log"; // unified singular
    private static final String LEGACY_TABLE = "execution_logs"; // legacy plural

    // Required columns (name -> DDL fragment)
    private static final String[][] REQUIRED_COLUMNS = new String[][] {
            {"id", "INT AUTO_INCREMENT PRIMARY KEY"},
            {"test_name", "VARCHAR(255)"},
            {"status", "VARCHAR(20)"},
            {"test_type", "VARCHAR(20)"},
            {"us_id", "VARCHAR(50)"},
            {"tc_id", "VARCHAR(255)"},
            {"artifact", "VARCHAR(500)"},
            {"screenshot_path", "VARCHAR(500)"},
            {"execution_time", "DATETIME DEFAULT CURRENT_TIMESTAMP"},
            {"message", "VARCHAR(1000)"},
            {"level", "VARCHAR(20)"},
            {"log_time", "DATETIME DEFAULT CURRENT_TIMESTAMP"},
            {"start_time", "DATETIME NULL"},
            {"end_time", "DATETIME NULL"},
            {"duration", "BIGINT NULL"}
    };

    public static void migrate() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            handleLegacy(conn);
            if (!tableExists(conn, TABLE_NAME)) {
                createTable(conn);
                System.out.println("[DB-MIGRATION] Created table '" + TABLE_NAME + "'.");
            } else {
                ensureColumns(conn);
            }
        } catch (SQLException e) {
            System.err.println("[DB-MIGRATION] Failed: " + e.getMessage());
        }
    }

    private static void handleLegacy(Connection conn) throws SQLException {
        boolean legacyExists = tableExists(conn, LEGACY_TABLE);
        boolean currentExists = tableExists(conn, TABLE_NAME);
        if (legacyExists && !currentExists) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("RENAME TABLE " + LEGACY_TABLE + " TO " + TABLE_NAME);
                System.out.println("[DB-MIGRATION] Renamed legacy table '" + LEGACY_TABLE + "' to '" + TABLE_NAME + "'.");
            }
        }
    }

    private static boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        }
    }

    private static void createTable(Connection conn) throws SQLException {
        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(TABLE_NAME).append(" (");
        for (int i = 0; i < REQUIRED_COLUMNS.length; i++) {
            if (i > 0) ddl.append(", ");
            ddl.append(REQUIRED_COLUMNS[i][0]).append(" ").append(REQUIRED_COLUMNS[i][1]);
        }
        ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl.toString());
        }
    }

    private static void ensureColumns(Connection conn) throws SQLException {
        Set<String> existing = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            ps.setString(1, TABLE_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) existing.add(rs.getString(1).toLowerCase());
            }
        }
        for (String[] col : REQUIRED_COLUMNS) {
            String name = col[0];
            String def = col[1];
            if (!existing.contains(name.toLowerCase())) {
                String alter = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + name + " " + def + ";";
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(alter);
                    System.out.println("[DB-MIGRATION] Added missing column '" + name + "'.");
                }
            }
        }
    }
}
