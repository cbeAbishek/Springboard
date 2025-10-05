# Enhanced Reporting System - Implementation Guide

## Overview

The reporting system has been completely revamped with the following features:

### ✅ Key Features Implemented

1. **Unique Report IDs** - Each report gets a unique ID format: `RPT_YYYYMMDD_HHMMSS_UUID`
2. **Organized Directory Structure** - Reports stored in `artifacts/reports/{REPORT_ID}/`
3. **Screenshot Integration** - Failed UI tests automatically include screenshots
4. **Comprehensive Filtering** - Filter reports by suite type, status, browser, date range
5. **Aggregated Reporting** - View cumulative test results across all executions
6. **Works in UI & CMD** - Seamless operation from both dashboard and command line

---

## Directory Structure

```
artifacts/
└── reports/
    ├── RPT_20251006_143025_a1b2c3d4/
    │   ├── report.html                    # Individual report
    │   ├── screenshots/                   # UI test screenshots
    │   │   ├── testBookFlight_FAILED.png
    │   │   └── testLogin_FAILED.png
    │   ├── artifacts/                     # API test artifacts
    │   │   └── api_response.json
    │   └── logs/                          # Test execution logs
    ├── RPT_20251006_150530_e5f6g7h8/
    │   └── ... (same structure)
    └── aggregated_report.html             # Cumulative report
```

---

## How It Works

### 1. Test Execution Flow

```
Test Suite Starts
    ↓
UnifiedReportListener.onStart()
    ↓
ReportManager.initializeReport()
    ↓
Generate unique Report ID (RPT_YYYYMMDD_HHMMSS_UUID)
    ↓
Create directory: artifacts/reports/{REPORT_ID}/
    ↓
Store in database: test_reports table
    ↓
Tests Execute
    ↓
On Test Completion → UnifiedReportListener captures details
    ↓
Screenshots taken for failed UI tests (ScreenshotListener)
    ↓
TestReportDetail saved with screenshot paths
    ↓
Suite Finishes
    ↓
ReportManager.finalizeReport()
    ↓
Generate HTML report in report directory
    ↓
Update aggregated report with all historical data
```

### 2. Report Generation Triggers

**Automatic Generation:**
- After every test suite execution (UI, API, or ALL)
- Triggered by TestNG listener (UnifiedReportListener)
- Works for both UI-triggered and CMD-triggered executions

**Manual Generation:**
- Via Dashboard: Navigate to Reports page
- Via API: Call `/dashboard/api/reports`

---

## Running Tests

### From Command Line

```bash
# Run UI tests - creates report with ID
mvn clean test -Dsuite=ui -Dbrowser=chrome

# Run API tests - creates report with ID
mvn clean test -Dsuite=api

# Run specific test - creates report with ID
mvn test -Dtest=org.automation.ui.BlazeDemoTests

# Set report metadata
mvn test -Dsuite=ui \
  -Dreport.created.by=CMD \
  -Dreport.trigger.type=MANUAL \
  -Dbrowser=chrome
```

### From Dashboard UI

1. Navigate to: `http://localhost:8080/dashboard/test-manager`
2. Select test suite (UI/API/All)
3. Click "Run Tests"
4. System automatically:
   - Generates unique report ID
   - Sets `report.created.by=UI`
   - Creates report directory
   - Captures screenshots on failures
   - Stores all data in database

---

## Viewing Reports

### Dashboard Navigation

```
http://localhost:8080/dashboard
    ├── /reports                          # All reports with filters
    ├── /reports/{reportId}               # Individual report view
    ├── /reports/aggregated               # Cumulative report
    └── /analytics                        # Test analytics matrix
```

### Reports Page Features

**Filters Available:**
- Suite Type: UI, API, ALL, SPECIFIC
- Status: COMPLETED, FAILED, RUNNING
- Browser: chrome, firefox, edge
- Date Range: Start date to End date
- Search: By Report ID or test name

**Actions:**
- View Report: Opens detailed report with screenshots
- Download Report: Downloads HTML file
- View Screenshots: For failed UI tests
- Export Data: CSV/Excel export

---

## Database Schema

### Table: test_reports

```sql
CREATE TABLE test_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id VARCHAR(255) UNIQUE NOT NULL,
    report_name VARCHAR(500),
    execution_date DATETIME,
    suite_type VARCHAR(50),      -- UI, API, ALL, SPECIFIC
    browser VARCHAR(50),
    total_tests INT,
    passed_tests INT,
    failed_tests INT,
    skipped_tests INT,
    success_rate DOUBLE,
    duration_ms BIGINT,
    report_path VARCHAR(1000),
    status VARCHAR(50),           -- RUNNING, COMPLETED, FAILED
    created_by VARCHAR(50),       -- UI, CMD
    trigger_type VARCHAR(50),     -- MANUAL, SCHEDULED, CI/CD
    branch_name VARCHAR(255),
    commit_hash VARCHAR(255)
);
```

### Table: test_report_details

```sql
CREATE TABLE test_report_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT,
    test_name VARCHAR(500),
    test_class VARCHAR(500),
    test_method VARCHAR(255),
    status VARCHAR(50),           -- PASS, FAIL, SKIP
    start_time DATETIME,
    end_time DATETIME,
    duration_ms BIGINT,
    error_message TEXT,
    stack_trace TEXT,
    screenshot_path VARCHAR(1000),
    screenshot_name VARCHAR(255),
    test_type VARCHAR(50),        -- UI, API
    browser VARCHAR(50),
    api_endpoint VARCHAR(1000),
    api_method VARCHAR(50),
    api_response_code INT,
    api_artifact_path VARCHAR(1000),
    FOREIGN KEY (report_id) REFERENCES test_reports(id)
);
```

---

## API Endpoints

### Reports API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/dashboard/reports` | GET | View all reports with filters |
| `/dashboard/reports/{reportId}` | GET | View specific report details |
| `/dashboard/reports/aggregated` | GET | View cumulative report |
| `/dashboard/api/reports` | GET | Get reports as JSON |
| `/dashboard/api/reports/{reportId}/details` | GET | Get report details as JSON |
| `/dashboard/api/reports/{reportId}/download` | GET | Download report HTML |

### Query Parameters for Filtering

```
GET /dashboard/reports?suiteType=UI&status=COMPLETED&browser=chrome&startDate=2025-10-01&endDate=2025-10-06

Parameters:
- suiteType: UI, API, ALL, SPECIFIC (optional)
- status: COMPLETED, FAILED, RUNNING (optional)
- browser: chrome, firefox, edge (optional)
- startDate: ISO datetime (optional)
- endDate: ISO datetime (optional)
```

---

## Screenshot Handling

### Automatic Screenshot Capture

**When:**
- On UI test failure (automatically)
- Captured by ScreenshotListener

**Where Stored:**
1. Original: `artifacts/screenshots/{testName}_FAILED.png`
2. Report Copy: `artifacts/reports/{REPORT_ID}/screenshots/{testName}_FAILED.png`

**In Report:**
- Failed tests section shows screenshot thumbnails
- Click to view full size in modal
- Download option available

### Example Usage in Report

```html
<div class='failed-test-item'>
    <h3>testBookFlight - FAILED</h3>
    <div class='error-message'>
        Expected: "Thank you"
        Actual: "Error occurred"
    </div>
    <div class='screenshot-gallery'>
        <img src='screenshots/testBookFlight_FAILED.png' 
             onclick='openModal(this.src)'>
    </div>
</div>
```

---

## Report Types

### 1. Individual Report
- **Location:** `artifacts/reports/{REPORT_ID}/report.html`
- **Contains:**
  - Summary statistics (passed/failed/skipped)
  - Test results table with filtering
  - Failed tests with screenshots
  - Duration and timing info
  - Interactive charts

### 2. Aggregated Report
- **Location:** `artifacts/reports/aggregated_report.html`
- **Contains:**
  - All test executions to date
  - Overall statistics
  - Trend charts
  - Filtering by suite/status/browser
  - Search functionality

---

## Configuration

### System Properties

```bash
# Set via command line
-Dreport.created.by=CMD          # Or UI
-Dreport.trigger.type=MANUAL     # Or SCHEDULED, CI/CD
-Dbrowser=chrome                 # Browser for UI tests
```

### Spring Application Properties

```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/test_automation
spring.datasource.username=root
spring.datasource.password=yourpassword

# JPA settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

---

## Integration with Existing Listeners

The system integrates with existing listeners:

1. **UnifiedReportListener** - Main coordinator
   - Initializes reports on suite start
   - Captures test details
   - Finalizes reports on suite end

2. **ScreenshotListener** - Screenshot capture
   - Takes screenshots on UI test failures
   - Stores path in test result attributes
   - UnifiedReportListener picks up the path

3. **TestListener** - Existing test logging
   - Continues to work alongside new system

4. **ReportListener** - Legacy report generation
   - Can coexist with new system

---

## Troubleshooting

### Reports Not Generated

**Check:**
1. Database connection is configured
2. `artifacts/reports` directory exists and is writable
3. UnifiedReportListener is in testng.xml
4. Spring beans are properly autowired

**Fix:**
```bash
# Create directory
mkdir -p artifacts/reports

# Check permissions
chmod 755 artifacts/reports

# Verify listener in testng.xml
<listener class-name="org.automation.listeners.UnifiedReportListener"/>
```

### Screenshots Not Showing

**Check:**
1. ScreenshotListener is enabled in testng.xml
2. Screenshot path is being stored in test attributes
3. Screenshots are being copied to report directory

**Debug:**
```java
// In test result
String screenshotPath = (String) result.getAttribute("screenshotPath");
System.out.println("Screenshot path: " + screenshotPath);
```

### Database Errors

**Check:**
1. Tables are created (auto-created if `ddl-auto=update`)
2. Connection string is correct
3. User has proper permissions

**Manual table creation:**
```sql
-- Run the CREATE TABLE statements from Database Schema section above
```

---

## Example Workflows

### Workflow 1: Run UI Tests from CMD

```bash
# Execute tests
mvn clean test -Dsuite=ui -Dbrowser=chrome -Dreport.created.by=CMD

# Output:
# ✅ Report initialized with ID: RPT_20251006_143025_a1b2c3d4
# ✅ Created report directory: artifacts/reports/RPT_20251006_143025_a1b2c3d4/
# ✅ Running tests...
# ✅ Test failed: testBookFlight - Screenshot captured
# ✅ Reports finalized successfully
# ✅ HTML report: artifacts/reports/RPT_20251006_143025_a1b2c3d4/report.html
# ✅ Aggregated report updated
```

### Workflow 2: Run Tests from Dashboard

```
1. Open: http://localhost:8080/dashboard/test-manager
2. Select: Suite Type = UI, Browser = Chrome
3. Click: Run Tests
4. System automatically:
   - Creates RPT_20251006_150530_e5f6g7h8
   - Stores metadata (created_by=UI, trigger_type=MANUAL)
   - Executes tests
   - Captures screenshots
   - Generates reports
5. Navigate to: /dashboard/reports
6. See new report in list
7. Click "View Report" to see details
```

### Workflow 3: View Failed Tests with Screenshots

```
1. Navigate: http://localhost:8080/dashboard/reports
2. Filter: Status = COMPLETED, Suite Type = UI
3. Click: View Report on any report
4. Scroll to: "Failed Tests Details" section
5. See: Each failed test with screenshot thumbnail
6. Click: Screenshot to view full size
7. Download: Report or individual screenshots
```

---

## Benefits

### ✅ Unique IDs
- Every report is uniquely identifiable
- Easy to reference in discussions
- No naming conflicts

### ✅ Organized Storage
- Each report in its own directory
- Screenshots and artifacts grouped with report
- Easy to archive or share

### ✅ Filtering & Search
- Find specific reports quickly
- Filter by multiple criteria
- Date range analysis

### ✅ Screenshot Integration
- Automatic capture on UI failures
- Displayed in report
- Organized in report directory

### ✅ Aggregated View
- Historical analysis
- Trend identification
- Overall success rate tracking

### ✅ Works Everywhere
- Command line execution
- Dashboard UI execution
- CI/CD integration
- No configuration differences

---

## Next Steps

1. **Run a test suite** to generate first report
2. **Check database** to verify test_reports table populated
3. **Navigate to dashboard** to view reports
4. **Explore filtering** options
5. **Review aggregated report** for historical data

---

**Last Updated:** October 6, 2025
**Version:** 2.0.0
**Status:** Production Ready ✅

