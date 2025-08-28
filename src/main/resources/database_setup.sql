-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS testframework_db;

-- Use the database
USE testframework_db;

-- Create test_cases table
CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    description TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create test_results table
CREATE TABLE IF NOT EXISTS test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_case_id BIGINT NOT NULL,
    status VARCHAR(50),
    executed_at TIMESTAMP,
    notes TEXT,
    duration BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_case_id) REFERENCES test_cases(id) ON DELETE CASCADE
);

-- Insert sample test cases
INSERT INTO test_cases (name, type, description, status) VALUES
('Login Test', 'Functional', 'Test user login functionality with valid credentials', 'Active'),
('Registration Test', 'Functional', 'Test user registration with valid data', 'Active'),
('Password Reset Test', 'Functional', 'Test password reset functionality', 'Active'),
('API Response Time Test', 'Performance', 'Test API response time under normal load', 'Active'),
('Database Connection Test', 'Integration', 'Test database connectivity and queries', 'Active');

-- Insert sample test results
INSERT INTO test_results (test_case_id, status, executed_at, notes, duration) VALUES
(1, 'PASSED', NOW(), 'Login test completed successfully', 1200),
(2, 'PASSED', NOW(), 'Registration test completed successfully', 1800),
(3, 'FAILED', NOW(), 'Password reset email not sent', 2500),
(4, 'PASSED', NOW(), 'API response time within acceptable limits', 800),
(5, 'PASSED', NOW(), 'Database connection established successfully', 500);

-- Show the created data
SELECT 'Test Cases:' as Info;
SELECT * FROM test_cases;

SELECT 'Test Results:' as Info;
SELECT * FROM test_results;
