# Quick Reference Guide - Springboard Test Automation

## 🚀 Quick Start Commands

### Local Test Execution

```bash
# Make script executable (first time only)
chmod +x run-specific-test.sh

# Run all tests
./run-specific-test.sh -s all

# Run UI tests (Chrome)
./run-specific-test.sh -s ui -b chrome

# Run API tests
./run-specific-test.sh -s api

# Run specific test class
./run-specific-test.sh -s specific -c org.automation.ui.BlazeDemoTests

# Run specific test method
./run-specific-test.sh -s specific -c org.automation.ui.BlazeDemoTests -m testBookFlight
```

### Dashboard Access

```bash
# Start dashboard
mvn spring-boot:run

# Access URLs:
# Main Dashboard:     http://localhost:8080/dashboard
# Analytics Matrix:   http://localhost:8080/dashboard/analytics
# Test Manager:       http://localhost:8080/dashboard/test-manager
# Reports Browser:    http://localhost:8080/dashboard/reports
```

## 📊 GitHub Actions Workflow

### Automatic Triggers
- ✅ Every push to repository
- ✅ Daily at 2 AM UTC (scheduled)
- ✅ Manual trigger (workflow_dispatch)

### Manual Execution Steps
1. Go to GitHub → **Actions** tab
2. Select **"Automated Test Suite"** workflow
3. Click **"Run workflow"** button
4. Configure options:
   - **Test Suite**: all / ui / api / specific
   - **Test File**: org.automation.ui.BlazeDemoTests (for specific)
   - **Test Method**: testBookFlight (optional)
   - **Browser**: chrome / firefox / edge
   - **Generate Reports**: true / false

### Workflow Jobs (Automatic)
```
setup → ui-tests (Chrome & Firefox) → consolidate-reports
     → api-tests                    → analytics-update
     → specific-test                → deploy-dashboard
                                    → notification
```

## 📁 Key File Paths

### Test Files
- **UI Tests**: `src/test/java/org/automation/ui/`
- **API Tests**: `src/test/java/org/automation/api/`

### Configuration
- **Complete Suite**: `testng.xml`
- **UI Only**: `testng-ui.xml`
- **API Only**: `testng-api.xml`
- **Maven Config**: `pom.xml`

### Reports (Auto-Generated)
- **Surefire**: `target/surefire-reports/`
- **Allure**: `allure-results/`, `target/allure-report/`
- **Custom**: `artifacts/reports/`
- **Screenshots**: `artifacts/screenshots/`

### Scripts
- **Specific Tests**: `./run-specific-test.sh`
- **General Runner**: `./run-tests.sh`
- **Allure Report**: `./generate-allure-report.sh`

## 🎯 Dashboard Features

### Main Dashboard (`/dashboard`)
- Real-time test statistics
- Pass/Fail rates with charts
- Recent test executions table
- Quick action buttons

### Analytics Matrix (`/dashboard/analytics`)
**NEW CUMULATIVE ANALYSIS:**
- ✅ Test execution matrix with all runs
- ✅ Failure heatmap (30-day visualization)
- ✅ Pass rate trends for each test
- ✅ Filter by: date, suite, status
- ✅ Export to CSV/Excel
- ✅ Search functionality

**Matrix Columns:**
| Column | Description |
|--------|-------------|
| Test Name | Full test method name |
| Suite | UI / API / Integration |
| Status | PASS / FAIL / SKIP |
| Execution Time | When test was run |
| Duration | Test execution time (ms) |
| Environment | Browser or platform |
| Pass Rate | % with visual bar |
| Last Run | Relative time (e.g., "2h ago") |
| Trend | ↗ Improving / → Stable / ↘ Declining |

**Heatmap:**
- Shows last 30 days of test failures
- Color intensity = failure count
- Darker cells = more failures
- Helps identify flaky tests

## 🔧 Common Commands

### Maven Commands
```bash
# Run all tests
mvn clean test -Dsuite=all

# Run with specific browser
mvn clean test -Dsuite=ui -Dbrowser=firefox

# Run headless
mvn clean test -Dsuite=ui -Dbrowser=chrome -Dheadless=true

# Run specific test
mvn test -Dtest=org.automation.ui.BlazeDemoTests

# Run specific method
mvn test -Dtest=org.automation.ui.BlazeDemoTests#testBookFlight

# Generate reports only
mvn surefire-report:report-only
mvn allure:report

# Build dashboard
mvn clean package -DskipTests
```

### View Reports
```bash
# Open Surefire report
open target/site/surefire-report.html

# Serve Allure report
allure serve allure-results

# Start dashboard
mvn spring-boot:run
```

## 📤 API Endpoints

### Dashboard APIs
```
GET  /dashboard                    # Main dashboard page
GET  /dashboard/analytics          # Analytics matrix page
GET  /dashboard/test-manager       # Test manager page
GET  /dashboard/reports            # Reports browser

GET  /dashboard/api/stats          # Test statistics JSON
GET  /dashboard/api/test-matrix    # Matrix data JSON
     ?days=30&suite=ui&status=FAIL # Optional filters
GET  /dashboard/api/failure-heatmap # Heatmap data JSON
     ?days=30

GET  /dashboard/api/export/csv     # Download CSV
GET  /dashboard/api/export/excel   # Download Excel

POST /dashboard/run-tests          # Execute tests
     {suite: "ui", browser: "chrome", headless: false}
```

## 🐛 Troubleshooting

### Script Issues
```bash
# Permission denied
chmod +x run-specific-test.sh

# Not found
./run-specific-test.sh --help
```

### Test Issues
```bash
# Test not found - use full class name
-c org.automation.ui.BlazeDemoTests  # ✅ Correct
-c BlazeDemoTests                    # ❌ Wrong

# Clear cache and rebuild
mvn clean install -DskipTests
```

### Dashboard Issues
```bash
# Dashboard won't start
mvn clean package -DskipTests
mvn spring-boot:run

# Database connection error
# Check src/main/resources/application.properties
```

### Reports Not Generated
```bash
# Ensure test failures don't stop build
mvn test -Dmaven.test.failure.ignore=true

# Force report generation
mvn surefire-report:report-only
mvn site -DgenerateReports=false
```

## 📊 GitHub Actions Artifacts

After workflow completion, download artifacts:
- `ui-test-results-{browser}-{timestamp}` - UI test results
- `api-test-results-{timestamp}` - API test results  
- `specific-test-results-{timestamp}` - Specific test results
- `consolidated-test-reports-{timestamp}` - All reports combined
- `dashboard-package-{timestamp}` - Dashboard JAR + reports

**Retention:** 
- Test results: 30 days
- Consolidated reports: 90 days
- Screenshots: 7 days

## ✨ Best Practices

### Development
1. Run specific tests during development
2. Run full suite before commit
3. Review test reports locally
4. Check dashboard analytics daily

### CI/CD
1. Let GitHub Actions run full suite
2. Review consolidated reports
3. Monitor failure heatmap
4. Export data for stakeholders

### Reporting
1. Reports auto-generate for ALL executions
2. Check artifacts after GitHub Actions runs
3. Use dashboard for real-time metrics
4. Export CSV/Excel for meetings

## 📞 Support

- **Documentation**: `TEST_EXECUTION_GUIDE.md`
- **Quick Ref**: This file
- **Dashboard**: http://localhost:8080/dashboard
- **Logs**: `logs/` directory
- **Test Output**: `test-output/` directory

---

**Last Updated:** 2024
**Version:** 2.0.0

