# Automation Framework for Regression Testing

## Overview
This is a comprehensive automation framework for managing regression testing of web and API applications using core Java technologies. The framework leverages Spring Boot, Selenium WebDriver, REST-Assured, and multithreading for efficient test execution and management.

## Features
- **Web UI Testing** with Selenium WebDriver
- **API Testing** with REST-Assured
- **Parallel Test Execution** with configurable thread pools
- **Test Scheduling** with Quartz Scheduler
- **Comprehensive Reporting** (HTML, CSV, JUnit formats)
- **Screenshot Capture** on test failures
- **Analytics and Trend Analysis**
- **RESTful APIs** for web-based management
- **Database Storage** for test results and metrics

## Architecture - Four Core Modules

### 1. Test Integration Engine
- **WebDriverManager**: Manages WebDriver instances with support for Chrome, Firefox, and Edge
- **WebUITestExecutor**: Executes web UI tests with screenshot capture on failures
- **ApiTestExecutor**: Executes API tests with comprehensive validation

### 2. Scheduling and Execution System
- **TestScheduler**: Manages test scheduling with cron expressions
- **TestExecutionService**: Handles batch processing and parallel execution
- **Quartz Integration**: For reliable job scheduling and execution

### 3. Reporting and Log Collection Hub
- **ReportGenerator**: Creates HTML, CSV, and JUnit reports
- **ScreenshotUtils**: Captures and manages failure screenshots
- **ReportingService**: Asynchronous report generation

### 4. Result Analytics Tracker
- **AnalyticsService**: Provides trend analysis and regression metrics
- **Database Storage**: Persistent storage of test results and analytics

## Technology Stack
- **Java 21** with Spring Boot 3.1.5
- **Selenium WebDriver 4.15.0** for web automation
- **REST-Assured 5.3.2** for API testing
- **Quartz Scheduler** for test scheduling
- **H2/MySQL Database** for data persistence
- **ExtentReports** for comprehensive reporting
- **TestNG** for test execution
- **Maven** for build management

## Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- Chrome/Firefox/Edge browser

### Installation
1. Clone the repository
2. Navigate to project directory
3. Run: `mvn clean install`
4. Run: `mvn spring-boot:run`

### Configuration
Configure the framework in `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

# Framework Settings
automation.framework.webDriver.defaultBrowser=chrome
automation.framework.execution.maxParallelThreads=5
automation.framework.reporting.outputPath=test-reports/
```

## REST API Endpoints

### Test Execution
- `POST /api/execution/batch` - Execute test batch
- `POST /api/execution/single/{testCaseId}` - Execute single test
- `GET /api/execution/batch/{batchId}` - Get batch status

### Test Case Management
- `GET /api/testcases` - Get all test cases
- `POST /api/testcases` - Create new test case
- `PUT /api/testcases/{id}` - Update test case
- `DELETE /api/testcases/{id}` - Delete test case

### Scheduling
- `GET /api/schedules` - Get all schedules
- `POST /api/schedules` - Create new schedule
- `POST /api/schedules/{id}/activate` - Activate schedule

### Analytics
- `GET /api/analytics/trends` - Get trend analysis
- `GET /api/analytics/regression/{environment}` - Get regression metrics

### Reporting
- `POST /api/reports/generate/{batchId}` - Generate all reports
- `POST /api/reports/html/{batchId}` - Generate HTML report

## Usage Examples

### Creating a Test Case
```json
POST /api/testcases
{
  "name": "Login Test",
  "description": "Test user login functionality",
  "testType": "WEB_UI",
  "testData": "{\"url\":\"https://example.com/login\"}",
  "expectedResult": "Dashboard",
  "priority": "HIGH",
  "testSuite": "smoke",
  "environment": "staging"
}
```

### Executing a Test Batch
```json
POST /api/execution/batch
{
  "testSuite": "smoke",
  "environment": "staging",
  "parallelThreads": 3
}
```

### Creating a Schedule
```json
POST /api/schedules
{
  "scheduleName": "Daily Smoke Tests",
  "cronExpression": "0 0 2 * * ?",
  "testSuite": "smoke",
  "environment": "staging",
  "parallelThreads": 2
}
```

## Database Schema
The framework uses the following main entities:
- **TestCase**: Test case definitions
- **TestExecution**: Individual test execution results
- **TestBatch**: Batch execution tracking
- **TestSchedule**: Scheduled test configurations

## Reports and Analytics
- **HTML Reports**: Comprehensive visual reports with charts
- **CSV Reports**: Data export for external analysis
- **JUnit Reports**: CI/CD integration support
- **Trend Analysis**: Pass/fail rates over time
- **Regression Metrics**: Stability and detection rates

## Monitoring and Management
- **H2 Console**: Database management at `/h2-console`
- **Actuator Endpoints**: Health and metrics monitoring
- **Logging**: Comprehensive logging with configurable levels

## CI/CD Integration
The framework supports CI/CD pipelines through:
- Maven build integration
- JUnit XML reports
- REST API for external triggering
- Configurable environments

## Contributing
1. Fork the repository
2. Create feature branch
3. Commit changes
4. Create pull request

