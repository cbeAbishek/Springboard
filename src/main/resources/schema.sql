-- MySQL Database Schema for Automation Test Reporting System
-- Database: automation_tests

CREATE DATABASE IF NOT EXISTS automation_tests;
USE automation_tests;

-- Table: test_reports
-- Stores master report information with unique IDs
CREATE TABLE IF NOT EXISTS test_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(50) NOT NULL UNIQUE,
    report_name VARCHAR(255),
    execution_date DATETIME,
    suite_type VARCHAR(50),
    browser VARCHAR(50),
    total_tests INT DEFAULT 0,
    passed_tests INT DEFAULT 0,
    failed_tests INT DEFAULT 0,
    skipped_tests INT DEFAULT 0,
    success_rate DOUBLE DEFAULT 0.0,
    duration_ms BIGINT DEFAULT 0,
    report_path VARCHAR(500),
    status VARCHAR(20),
    created_by VARCHAR(20),
    trigger_type VARCHAR(20),
    branch_name VARCHAR(100),
    commit_hash VARCHAR(100),
    environment VARCHAR(50),
    INDEX idx_report_id (report_id),
    INDEX idx_execution_date (execution_date),
    INDEX idx_suite_type (suite_type),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: test_report_details
-- Stores individual test execution details
CREATE TABLE IF NOT EXISTS test_report_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    test_name VARCHAR(255),
    test_class VARCHAR(500),
    test_method VARCHAR(255),
    status VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    duration_ms BIGINT,
    error_message TEXT,
    stack_trace TEXT,
    screenshot_path VARCHAR(500),
    screenshot_name VARCHAR(255),
    test_type VARCHAR(20),
    browser VARCHAR(50),
    api_endpoint VARCHAR(500),
    api_method VARCHAR(20),
    api_response_code INT,
    api_request_body TEXT,
    api_response_body TEXT,
    api_artifact_path VARCHAR(500),
    retry_count INT DEFAULT 0,
    tags VARCHAR(500),
    FOREIGN KEY (report_id) REFERENCES test_reports(id) ON DELETE CASCADE,
    INDEX idx_test_name (test_name),
    INDEX idx_status (status),
    INDEX idx_test_type (test_type),
    INDEX idx_report_id (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: execution_log (existing compatibility)
-- Maintains backward compatibility with existing code
CREATE TABLE IF NOT EXISTS execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255),
    test_class VARCHAR(255),
    status VARCHAR(50),
    start_time DATETIME,
    end_time DATETIME,
    duration_ms BIGINT,
    screenshot_path VARCHAR(500),
    error_message TEXT,
    browser VARCHAR(50),
    suite_id VARCHAR(100),
    test_suite VARCHAR(255),
    timestamp DATETIME,
    duration BIGINT,
    INDEX idx_suite_id (suite_id),
    INDEX idx_status (status),
    INDEX idx_test_name (test_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Legacy tables for backward compatibility
CREATE TABLE IF NOT EXISTS ui_tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    us_id VARCHAR(50),
    test_case_id VARCHAR(50),
    name VARCHAR(255),
    status VARCHAR(50),
    execution_time DATETIME,
    duration_ms BIGINT,
    artifact VARCHAR(500),
    INDEX idx_status (status),
    INDEX idx_us_id (us_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    us_id VARCHAR(50),
    test_case_id VARCHAR(50),
    name VARCHAR(255),
    status VARCHAR(50),
    execution_time DATETIME,
    duration_ms BIGINT,
    request TEXT,
    response TEXT,
    artifact VARCHAR(500),
    INDEX idx_status (status),
    INDEX idx_us_id (us_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_type VARCHAR(20),
    us_id VARCHAR(50),
    test_case_id VARCHAR(50),
    message TEXT,
    level VARCHAR(20),
    screenshot_path VARCHAR(500),
    log_time DATETIME,
    INDEX idx_test_type (test_type),
    INDEX idx_log_time (log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Views for reporting

-- View: report_summary
-- Provides quick summary of all reports
CREATE OR REPLACE VIEW report_summary AS
SELECT
    r.report_id,
    r.report_name,
    r.execution_date,
    r.suite_type,
    r.total_tests,
    r.passed_tests,
    r.failed_tests,
    r.skipped_tests,
    r.success_rate,
    r.status,
    r.created_by,
    COUNT(d.id) as detail_count
FROM test_reports r
LEFT JOIN test_report_details d ON r.id = d.report_id
GROUP BY r.id
ORDER BY r.execution_date DESC;

-- View: failed_tests_with_screenshots
-- Lists all failed tests that have screenshots
CREATE OR REPLACE VIEW failed_tests_with_screenshots AS
SELECT
    r.report_id,
    r.report_name,
    r.execution_date,
    d.test_name,
    d.test_class,
    d.error_message,
    d.screenshot_path,
    d.screenshot_name,
    d.browser
FROM test_report_details d
JOIN test_reports r ON d.report_id = r.id
WHERE d.status IN ('FAIL', 'FAILED')
AND d.screenshot_path IS NOT NULL
ORDER BY r.execution_date DESC;

-- View: test_execution_stats
-- Provides overall statistics
CREATE OR REPLACE VIEW test_execution_stats AS
SELECT
    COUNT(DISTINCT r.id) as total_reports,
    SUM(r.total_tests) as total_tests,
    SUM(r.passed_tests) as total_passed,
    SUM(r.failed_tests) as total_failed,
    SUM(r.skipped_tests) as total_skipped,
    AVG(r.success_rate) as avg_success_rate,
    COUNT(DISTINCT CASE WHEN r.suite_type = 'UI' THEN r.id END) as ui_reports,
    COUNT(DISTINCT CASE WHEN r.suite_type = 'API' THEN r.id END) as api_reports
FROM test_reports r
WHERE r.status = 'COMPLETED';

-- Indexes for performance optimization
CREATE INDEX idx_report_details_composite ON test_report_details(report_id, status, test_type);
CREATE INDEX idx_reports_date_status ON test_reports(execution_date, status);

-- Stored Procedure: Clean old reports
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_old_reports(IN days_to_keep INT)
BEGIN
    DELETE FROM test_reports
    WHERE execution_date < DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
END //
DELIMITER ;

-- Sample data insertion for testing (optional)
-- Uncomment to test the schema
/*
INSERT INTO test_reports (report_id, report_name, execution_date, suite_type, total_tests, passed_tests, failed_tests, success_rate, status, created_by)
VALUES
('RPT_20231001_120000_abc123', 'Sample UI Test Suite', NOW(), 'UI', 10, 8, 2, 80.0, 'COMPLETED', 'CMD'),
('RPT_20231001_130000_def456', 'Sample API Test Suite', NOW(), 'API', 15, 14, 1, 93.33, 'COMPLETED', 'UI');
*/

-- Grant permissions (adjust username as needed)
-- GRANT ALL PRIVILEGES ON automation_tests.* TO 'root'@'localhost';
-- FLUSH PRIVILEGES;

SELECT 'Database schema created successfully!' as message;

