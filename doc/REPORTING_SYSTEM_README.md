# Test Reporting System Documentation

## Overview

This comprehensive reporting system provides unique IDs for each test execution, MySQL database integration, and support for both UI and CMD test execution with advanced filtering capabilities.

## Features

### ✅ Core Features
- **Unique Report IDs**: Each test execution generates a unique ID in format `RPT_YYYYMMDD_HHMMSS_UUID`
- **MySQL Database Integration**: All test results stored in MySQL for persistence and analytics
- **Dual Execution Mode Support**: Works seamlessly from both UI dashboard and command line (Maven)
- **Organized Report Storage**: All reports stored under `artifacts/reports/` with individual folders
- **Screenshot Management**: Failed UI tests automatically capture and store screenshots
- **API Artifact Storage**: API test requests/responses saved as artifacts
- **Aggregated Reporting**: Common report showing all tests executed to date
- **Advanced Filtering**: Filter reports by suite type, status, execution mode, browser, and date range

### 📊 Report Structure

Each report folder contains:
```
artifacts/reports/RPT_20231001_120000_abc123/
├── summary.html              # HTML summary report
├── screenshots/              # Failed UI test screenshots
│   ├── testLogin_FAILED_*.png
│   └── testCheckout_FAILED_*.png
├── api-artifacts/            # API test artifacts
│   ├── createUser_request.json
│   └── createUser_response.json
└── logs/                     # Execution logs
```

## Database Schema

### MySQL Tables

1. **test_reports** - Master report information
   - Unique report IDs
   - Execution metadata
   - Summary statistics

2. **test_report_details** - Individual test results
   - Test name, class, method
   - Pass/Fail/Skip status
   - Screenshots and error messages
   - API endpoint details

3. **execution_log** - Legacy compatibility
4. **ui_tests**, **api_responses**, **execution_logs** - Legacy tables

## Setup Instructions

### 1. MySQL Database Setup

```bash
# Create database and tables
mysql -u root -p < src/main/resources/schema.sql

# Or let Spring Boot auto-create tables
# Set spring.jpa.hibernate.ddl-auto=update in application.properties
```

### 2. Configuration

Update `src/main/resources/application.properties`:

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/automation_tests?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=Ck@709136

# Report Configuration
reports.base.directory=artifacts/reports
reports.retention.days=90
```

### 3. Running Tests

#### From Command Line (CMD Mode)

```bash
# Make script executable
chmod +x run-tests.sh

# Run tests
./run-tests.sh

# Or use Maven directly
mvn clean test -Dsuite=ui -Dbrowser=chrome
mvn clean test -Dsuite=api
mvn clean test -Dsuite=testng  # All tests
```

#### From UI Dashboard

```bash
# Start Spring Boot application
mvn spring-boot:run

# Access dashboard
http://localhost:8080

# Navigate to test execution page and trigger tests
```

## Accessing Reports

### 1. Web Dashboard

```bash
# Start application
mvn spring-boot:run

# Open browser
http://localhost:8080/reports
```

**Available Pages:**
- `/reports` - Main reports page with filters
- `/reports/{reportId}` - Detailed report view with test results
- `/reports/aggregated` - Aggregated report of all executions
- `/reports/failures` - Failed tests with screenshots

### 2. REST API Endpoints

```bash
# Get all reports
GET http://localhost:8080/api/reports

# Get specific report
GET http://localhost:8080/api/reports/RPT_20231001_120000_abc123

# Get test details
GET http://localhost:8080/api/reports/RPT_20231001_120000_abc123/details

# Filter reports
GET http://localhost:8080/api/reports/filter?suiteType=UI&status=COMPLETED&createdBy=CMD

# Get aggregated report
GET http://localhost:8080/api/reports/aggregated

# Get failed tests with screenshots
GET http://localhost:8080/api/reports/failures/screenshots

# Get statistics
GET http://localhost:8080/api/reports/statistics
```

### 3. File System

Reports are stored in: `artifacts/reports/RPT_YYYYMMDD_HHMMSS_UUID/`

Each folder contains:
- `summary.html` - Standalone HTML report
- `screenshots/` - PNG images of failed tests
- `api-artifacts/` - JSON files of API requests/responses
- `logs/` - Execution logs

## Filtering Capabilities

The web dashboard supports filtering by:

1. **Suite Type**: UI, API, ALL, SPECIFIC
2. **Status**: RUNNING, COMPLETED, FAILED
3. **Execution Mode**: UI, CMD
4. **Browser**: chrome, firefox, edge
5. **Date Range**: Start date to end date

## Report Information

Each report captures:

### Basic Information
- Unique Report ID
- Report Name
- Execution Date & Time
- Suite Type
- Browser (for UI tests)
- Execution Mode (UI/CMD)

### Statistics
- Total Tests
- Passed Tests
- Failed Tests
- Skipped Tests
- Success Rate (%)
- Duration (ms)

### Test Details
- Test Name & Class
- Test Status
- Start/End Time
- Duration
- Error Message & Stack Trace
- Screenshot (for failed UI tests)
- API Endpoint, Method, Response Code (for API tests)

## Integration with TestNG

The reporting system integrates with TestNG through listeners:

```xml
<!-- testng.xml -->
<listeners>
    <listener class-name="org.automation.listeners.SuiteExecutionListener"/>
    <listener class-name="org.automation.listeners.TestListener"/>
</listeners>
```

**SuiteExecutionListener**: Manages report lifecycle (initialize/finalize)
**TestListener**: Captures individual test results

## Usage Examples

### 1. Running UI Tests from Command Line

```bash
mvn clean test -Dsuite=ui -Dbrowser=chrome

# Report will be created at:
# artifacts/reports/RPT_20231006_143022_a1b2c3d4/
```

### 2. Running API Tests from Command Line

```bash
mvn clean test -Dsuite=api

# Report will be created at:
# artifacts/reports/RPT_20231006_143530_e5f6g7h8/
```

### 3. Viewing Reports in Dashboard

```bash
# Start application
mvn spring-boot:run

# Navigate to: http://localhost:8080/reports
# Apply filters as needed
# Click on report ID to view details
```

### 4. Accessing via REST API

```bash
# Get all reports
curl http://localhost:8080/api/reports

# Get specific report details
curl http://localhost:8080/api/reports/RPT_20231006_143022_a1b2c3d4/details

# Get aggregated report
curl http://localhost:8080/api/reports/aggregated
```

## Screenshot Management

For failed UI tests:

1. Screenshots automatically captured
2. Stored in report folder: `screenshots/`
3. Linked in database
4. Displayed in web dashboard
5. Clickable thumbnails for full view

## Aggregated Reporting

The aggregated report provides:

- Overall statistics across all reports
- Success rate trends
- Reports by suite type
- Reports by status
- Recent report history
- Visual charts and graphs

Access at: `http://localhost:8080/reports/aggregated`

## Database Queries

### Get all reports

```sql
SELECT * FROM test_reports ORDER BY execution_date DESC;
```

### Get failed tests with screenshots

```sql
SELECT * FROM failed_tests_with_screenshots;
```

### Get overall statistics

```sql
SELECT * FROM test_execution_stats;
```

### Cleanup old reports (older than 90 days)

```sql
CALL cleanup_old_reports(90);
```

## Troubleshooting

### Issue: Reports not appearing in dashboard

**Solution**: 
1. Check MySQL connection in `application.properties`
2. Verify database tables exist
3. Check logs for errors

### Issue: Screenshots not captured

**Solution**:
1. Ensure WebDriver is properly initialized
2. Check `DriverManager.getDriver()` returns valid driver
3. Verify screenshot directory has write permissions

### Issue: Database connection failed

**Solution**:
1. Verify MySQL is running: `systemctl status mysql`
2. Check credentials in `application.properties`
3. Create database manually: `CREATE DATABASE automation_tests;`

## Architecture

```
ReportManager (Core)
├── Generates unique report IDs
├── Manages report lifecycle
├── Creates directory structure
└── Coordinates with database

TestListener (TestNG)
├── Captures test results
├── Takes screenshots on failure
├── Stores test details
└── Updates report statistics

SuiteExecutionListener (TestNG)
├── Initializes report on suite start
├── Finalizes report on suite finish
└── Generates HTML summary

ReportService (Spring)
├── Provides report data
├── Implements filtering
├── Generates aggregated views
└── Manages database operations

ReportController (REST API)
└── Exposes report data via REST

ReportViewController (Web UI)
└── Renders web dashboard
```

## File Locations

- Report Manager: `src/main/java/org/automation/reports/ReportManager.java`
- Models: `src/main/java/org/automation/reports/model/`
- Repositories: `src/main/java/org/automation/reports/repository/`
- Services: `src/main/java/org/automation/reports/service/`
- Controllers: `src/main/java/org/automation/reports/controller/`
- Views: `src/main/resources/templates/`
- Database Schema: `src/main/resources/schema.sql`
- Configuration: `src/main/resources/application.properties`

## Next Steps

1. Start MySQL database
2. Run the schema creation script
3. Execute tests using `run-tests.sh` or Maven
4. Start Spring Boot application
5. View reports at `http://localhost:8080/reports`

## Support

For issues or questions, check:
- Application logs: `logs/`
- Database logs: MySQL error logs
- Test execution logs: `test-output/`
#!/bin/bash

# Test Execution Script with Reporting System
# This script demonstrates running tests from CMD with proper report generation

echo "=================================================="
echo "Test Execution with Reporting System"
echo "=================================================="

# Set environment variables
export MAVEN_HOME=${MAVEN_HOME:-/usr/share/maven}
export DB_URL=${DB_URL:-jdbc:mysql://localhost:3306/automation_tests}
export DB_USER=${DB_USER:-root}
export DB_PASS=${DB_PASS:-Ck@709136}

# Create artifacts directory if not exists
mkdir -p artifacts/reports

echo ""
echo "Step 1: Setting up MySQL database..."
echo "Executing schema creation..."

# Check if MySQL is running
if command -v mysql &> /dev/null; then
    mysql -u${DB_USER} -p${DB_PASS} < src/main/resources/schema.sql 2>/dev/null || echo "Database already exists or schema already created"
else
    echo "MySQL not found. Please ensure MySQL is installed and running."
    echo "You can also run the Spring Boot application which will auto-create tables."
fi

echo ""
echo "Step 2: Running tests..."
echo "Choose test suite to run:"
echo "1) UI Tests"
echo "2) API Tests"
echo "3) All Tests"
read -p "Enter choice (1-3): " choice

case $choice in
    1)
        SUITE="ui"
        echo "Running UI Test Suite..."
        ;;
    2)
        SUITE="api"
        echo "Running API Test Suite..."
        ;;
    3)
        SUITE="testng"
        echo "Running All Tests..."
        ;;
    *)
        SUITE="testng"
        echo "Invalid choice. Running all tests..."
        ;;
esac

# Run Maven tests
mvn clean test -Dsuite=${SUITE} -Dbrowser=chrome -Dheadless=false

echo ""
echo "=================================================="
echo "Test Execution Completed!"
echo "=================================================="
echo ""
echo "Generated Reports:"
echo "- Unique Report ID: Check artifacts/reports/ directory"
echo "- Latest report folder contains:"
echo "  * summary.html - HTML summary report"
echo "  * screenshots/ - Failed test screenshots"
echo "  * api-artifacts/ - API test artifacts"
echo "  * logs/ - Execution logs"
echo ""
echo "To view reports in the dashboard:"
echo "1. Start the Spring Boot application: mvn spring-boot:run"
echo "2. Open browser: http://localhost:8080/reports"
echo ""
echo "Database: All results are stored in MySQL database 'automation_tests'"
echo "=================================================="

