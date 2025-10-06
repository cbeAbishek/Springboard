# Quick Start Guide - Test Reporting System

## 🚀 Quick Setup (5 Minutes)

### Step 1: Configure MySQL Database

```bash
# Ensure MySQL is running
sudo systemctl start mysql

# Create database and tables
mysql -u root -pCk@709136 < src/main/resources/schema.sql
```

Or let Spring Boot auto-create tables (recommended for first-time setup):
- The application will automatically create tables when started
- Just ensure MySQL is running on port 3306

### Step 2: Verify Configuration

Check `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/automation_tests?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=Ck@709136
```

### Step 3: Run Tests from Command Line

```bash
# Option A: Use the provided script
./run-tests.sh

# Option B: Use Maven directly
mvn clean test -Dsuite=ui -Dbrowser=chrome
```

### Step 4: View Reports in Web Dashboard

```bash
# Start the Spring Boot application
mvn spring-boot:run

# Open browser and navigate to:
http://localhost:8080/reports
```

That's it! 🎉

## 📊 What You Get

### Unique Report IDs
Every test execution gets a unique ID like: `RPT_20231006_143530_a1b2c3d4`

### Organized Storage
```
artifacts/reports/RPT_20231006_143530_a1b2c3d4/
├── summary.html              # Standalone HTML report
├── screenshots/              # Failed test screenshots
│   └── testLogin_FAILED_*.png
├── api-artifacts/            # API test data
└── logs/                     # Execution logs
```

### Database Integration
All test results stored in MySQL for:
- Historical analysis
- Trend reporting
- Advanced filtering
- Aggregated statistics

### Web Dashboard Features
- **Filter reports** by suite type, status, browser, date range
- **View detailed results** for each test
- **See screenshots** for failed UI tests
- **Aggregated report** showing all tests to date
- **Export data** via REST API

## 🎯 Common Use Cases

### Use Case 1: Run UI Tests from Command Line

```bash
mvn clean test -Dsuite=ui -Dbrowser=chrome -Dheadless=false
```

**Result:**
- Report created: `artifacts/reports/RPT_YYYYMMDD_HHMMSS_UUID/`
- Data stored in MySQL
- Screenshots of failures saved
- Summary HTML generated

### Use Case 2: View All Test Reports

```bash
# Start application
mvn spring-boot:run

# Open browser
http://localhost:8080/reports

# Apply filters as needed
# Click on any report ID to see details
```

### Use Case 3: Get Aggregated Statistics

```bash
# Via Web UI
http://localhost:8080/reports/aggregated

# Via REST API
curl http://localhost:8080/api/reports/aggregated
```

### Use Case 4: Find Failed Tests with Screenshots

```bash
# Via Web UI
http://localhost:8080/reports/failures

# Via REST API
curl http://localhost:8080/api/reports/failures/screenshots
```

## 🔧 Running Tests

### From Command Line (CMD Mode)

```bash
# UI Tests
mvn clean test -Dsuite=ui -Dbrowser=chrome

# API Tests
mvn clean test -Dsuite=api

# All Tests
mvn clean test -Dsuite=testng
```

**Features:**
- Execution mode automatically detected as "CMD"
- Unique report ID generated
- All artifacts stored in dedicated folder
- MySQL database updated in real-time

### From UI Dashboard

1. Start application: `mvn spring-boot:run`
2. Navigate to: `http://localhost:8080`
3. Go to test execution page
4. Select suite and trigger tests
5. Watch real-time progress

**Features:**
- Execution mode automatically detected as "UI"
- Live progress updates
- Immediate report availability
- Direct navigation to results

## 📈 Accessing Reports

### Web Dashboard Endpoints

| Endpoint | Description |
|----------|-------------|
| `/reports` | Main reports page with filters |
| `/reports/{reportId}` | Detailed report view |
| `/reports/aggregated` | Aggregated report |
| `/reports/failures` | Failed tests with screenshots |

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/reports` | GET | Get all reports |
| `/api/reports/{reportId}` | GET | Get specific report |
| `/api/reports/{reportId}/details` | GET | Get test details |
| `/api/reports/filter` | GET | Filter reports |
| `/api/reports/aggregated` | GET | Aggregated data |
| `/api/reports/statistics` | GET | Statistics summary |

### Example API Calls

```bash
# Get all reports
curl http://localhost:8080/api/reports

# Get specific report
curl http://localhost:8080/api/reports/RPT_20231006_143530_a1b2c3d4

# Filter reports
curl "http://localhost:8080/api/reports/filter?suiteType=UI&status=COMPLETED"

# Get aggregated report
curl http://localhost:8080/api/reports/aggregated
```

## 🎨 Dashboard Features

### Filtering Options
- **Suite Type**: UI, API, ALL, SPECIFIC
- **Status**: RUNNING, COMPLETED, FAILED
- **Execution Mode**: UI, CMD
- **Browser**: chrome, firefox, edge
- **Date Range**: Custom start and end dates

### Report Information Displayed
- Report ID (unique identifier)
- Report name
- Execution date and time
- Suite type
- Status with color coding
- Test counts (total, passed, failed, skipped)
- Success rate percentage
- Duration
- Execution mode

### Test Details View
- Test name and class
- Status (color-coded)
- Duration
- Error messages
- Stack traces
- Screenshots (clickable thumbnails)
- API endpoint information

## 🗄️ Database Structure

### Main Tables
- `test_reports` - Master report data
- `test_report_details` - Individual test results
- `execution_log` - Legacy compatibility

### Useful Queries

```sql
-- Get all reports
SELECT * FROM test_reports ORDER BY execution_date DESC;

-- Get failed tests with screenshots
SELECT * FROM failed_tests_with_screenshots;

-- Get overall statistics
SELECT * FROM test_execution_stats;

-- Get reports from last 7 days
SELECT * FROM test_reports 
WHERE execution_date >= DATE_SUB(NOW(), INTERVAL 7 DAY);
```

## 🔍 Troubleshooting

### MySQL Connection Issues

```bash
# Check MySQL status
sudo systemctl status mysql

# Start MySQL if not running
sudo systemctl start mysql

# Test connection
mysql -u root -pCk@709136 -e "SHOW DATABASES;"
```

### Reports Not Appearing

1. Check application logs: `logs/spring-boot.log`
2. Verify MySQL connection in `application.properties`
3. Check `artifacts/reports/` directory permissions
4. Restart Spring Boot application

### Screenshots Not Captured

1. Ensure WebDriver is properly initialized
2. Check browser driver is in PATH
3. Verify `DriverManager.getDriver()` returns valid driver
4. Check write permissions on report directory

## 📝 Configuration Options

### application.properties

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/automation_tests
spring.datasource.username=root
spring.datasource.password=Ck@709136

# Report Settings
reports.base.directory=artifacts/reports
reports.retention.days=90
reports.auto.cleanup=false

# Server
server.port=8080
```

## 🎓 Best Practices

1. **Always run tests through TestNG** to ensure listeners are invoked
2. **Check database connection** before running large test suites
3. **Monitor disk space** in `artifacts/reports/` directory
4. **Use filters** in dashboard to find specific reports quickly
5. **Review aggregated report** for trend analysis
6. **Archive old reports** periodically (automated cleanup available)

## 📚 Additional Resources

- Full Documentation: `REPORTING_SYSTEM_README.md`
- Database Schema: `src/main/resources/schema.sql`
- Example Tests: `src/test/java/org/automation/`
- Configuration: `src/main/resources/application.properties`

## ✅ Verification Checklist

- [ ] MySQL is running
- [ ] Database `automation_tests` exists
- [ ] Tables are created
- [ ] `application.properties` is configured
- [ ] Tests run successfully from command line
- [ ] Reports appear in `artifacts/reports/`
- [ ] Spring Boot application starts
- [ ] Dashboard accessible at `http://localhost:8080/reports`
- [ ] Reports visible in dashboard
- [ ] Filters work correctly
- [ ] Screenshots visible for failed tests

## 🆘 Support

For issues:
1. Check application logs
2. Verify MySQL connection
3. Review `REPORTING_SYSTEM_README.md`
4. Check file permissions
5. Ensure all dependencies are installed

Happy Testing! 🚀

