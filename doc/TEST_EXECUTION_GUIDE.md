# Springboard Test Automation Framework - User Guide

## Overview

This updated test automation framework now supports:
- ✅ Running specific test files and methods
- ✅ Running entire test suites (UI, API, or both)
- ✅ Automatic report generation for all test executions
- ✅ Cumulative test analysis dashboard with matrix visualization
- ✅ Seamless execution in GitHub Actions and locally
- ✅ Export test results to CSV/Excel formats

---

## Table of Contents

1. [Running Tests Locally](#running-tests-locally)
2. [Running Tests in GitHub Actions](#running-tests-in-github-actions)
3. [Dashboard Features](#dashboard-features)
4. [File Structure](#file-structure)
5. [Configuration](#configuration)

---

## Running Tests Locally

### Using the Shell Script

The framework includes a powerful script `run-specific-test.sh` for local test execution:

#### Run All Tests
```bash
./run-specific-test.sh -s all
```

#### Run UI Tests Only
```bash
./run-specific-test.sh -s ui -b chrome
./run-specific-test.sh -s ui -b firefox -h  # Headless mode
```

#### Run API Tests Only
```bash
./run-specific-test.sh -s api
```

#### Run Specific Test Class
```bash
./run-specific-test.sh -s specific -c org.automation.ui.BlazeDemoTests
```

#### Run Specific Test Method
```bash
./run-specific-test.sh -s specific -c org.automation.ui.BlazeDemoTests -m testBookFlight -b chrome
```

#### Advanced Options
```bash
./run-specific-test.sh -s ui -b firefox -h -t 10  # Headless, 10 parallel threads
./run-specific-test.sh -s api -r                   # Skip report generation
```

### Using Maven Directly

#### Run Complete Test Suite
```bash
mvn clean test -Dsuite=all
```

#### Run UI Tests
```bash
mvn clean test -Dsuite=ui -Dbrowser=chrome
```

#### Run API Tests
```bash
mvn clean test -Dsuite=api
```

#### Run Specific Test Class
```bash
mvn clean test -Dtest=org.automation.ui.BlazeDemoTests
```

#### Run Specific Test Method
```bash
mvn clean test -Dtest=org.automation.ui.BlazeDemoTests#testBookFlight
```

---

## Running Tests in GitHub Actions

### Workflow Triggers

The workflow runs automatically on:
- Every push to the repository
- Scheduled at 2 AM daily (UTC)
- Manual trigger via workflow_dispatch

### Manual Workflow Execution

1. Navigate to **Actions** tab in GitHub
2. Select **Automated Test Suite** workflow
3. Click **Run workflow**
4. Choose your options:

#### Available Options

| Option | Description | Values |
|--------|-------------|--------|
| **Test Suite** | Type of tests to run | all, ui, api, specific |
| **Test File** | Specific test class | e.g., org.automation.ui.BlazeDemoTests |
| **Test Method** | Specific test method | e.g., testBookFlight |
| **Browser** | Browser for UI tests | chrome, firefox, edge |
| **Generate Reports** | Auto-generate reports | true, false |

#### Examples

**Run All Tests:**
- Test Suite: `all`
- Browser: `chrome`

**Run Specific UI Test:**
- Test Suite: `specific`
- Test File: `org.automation.ui.BlazeDemoTests`
- Test Method: `testBookFlight`
- Browser: `chrome`

**Run API Tests Only:**
- Test Suite: `api`

### Workflow Jobs

The GitHub Actions workflow consists of:

1. **setup** - Determines test configuration
2. **ui-tests** - Runs UI tests (Chrome & Firefox in parallel)
3. **api-tests** - Runs API tests
4. **specific-test** - Runs specific test file/method
5. **analytics-update** - Updates test analytics database
6. **consolidate-reports** - Combines all test reports
7. **deploy-dashboard** - Builds dashboard package
8. **notification** - Sends execution notifications

---

## Dashboard Features

### Accessing the Dashboard

#### Local Development
```bash
mvn spring-boot:run
```
Then navigate to: `http://localhost:8080/dashboard`

#### Dashboard Sections

### 1. Main Dashboard (`/dashboard`)
- Real-time test execution statistics
- Pass/Fail rate visualization
- Recent test executions
- Quick actions for running tests

### 2. Analytics Dashboard (`/dashboard/analytics`)
**NEW FEATURE**: Cumulative Test Analysis Matrix

Features:
- **Test Execution Matrix**: View all test executions with pass rates
- **Failure Heatmap**: Visual representation of test failures over time
- **Trend Analysis**: Identify improving/declining tests
- **Filters**: Date range, test suite, status, search
- **Export**: Download results as CSV or Excel

#### Matrix Columns:
- Test Name
- Suite (UI/API)
- Status (Pass/Fail/Skip)
- Execution Time
- Duration
- Browser/Environment
- Pass Rate (with visual bar)
- Last Run (relative time)
- Trend (Improving ↗ / Stable → / Declining ↘)

#### Heatmap Features:
- Daily failure counts for last 30 days
- Color-coded intensity (darker = more failures)
- Helps identify flaky tests
- Quick pattern recognition

### 3. Test Manager (`/dashboard/test-manager`)
- Run tests directly from browser
- Upload new test files
- Configure test execution parameters

### 4. Reports (`/dashboard/reports`)
- Browse all generated reports
- Download HTML/XML/JSON reports
- View Allure reports
- Screenshots from failed tests

---

## File Structure

```
Springboard/
├── .github/
│   └── workflows/
│       └── Auto_checkup.yml          # Updated GitHub Actions workflow
├── src/
│   ├── main/
│   │   └── resources/
│   │       └── templates/
│   │           └── dashboard/
│   │               ├── index.html         # Main dashboard
│   │               ├── analytics.html     # NEW: Cumulative analysis
│   │               ├── test-manager.html
│   │               └── reports.html
│   └── test/
│       └── java/
│           └── org/automation/
│               ├── analytics/
│               │   ├── service/
│               │   │   └── AnalyticsService.java  # Updated with matrix APIs
│               │   └── controller/
│               ├── dashboard/
│               │   └── DashboardController.java   # Updated with analytics routes
│               ├── ui/                # UI test classes
│               └── api/               # API test classes
├── artifacts/
│   ├── reports/          # Generated test reports
│   ├── screenshots/      # Failure screenshots
│   └── api/              # API test data
├── target/
│   ├── surefire-reports/ # TestNG/JUnit reports
│   └── site/             # Maven site reports
├── allure-results/       # Allure test results
├── run-specific-test.sh  # NEW: Local test execution script
├── run-tests.sh          # Original test runner
├── testng.xml            # Complete test suite
├── testng-ui.xml         # UI tests only
├── testng-api.xml        # API tests only
└── pom.xml               # Maven configuration
```

---

## Report Generation

### Automatic Report Generation

Reports are **automatically generated** after every test execution:

#### Surefire Reports
- Location: `target/surefire-reports/`
- Format: HTML, XML
- Contains: Test results, execution times, failures

#### Allure Reports
- Location: `allure-results/` (results), `target/allure-report/` (HTML)
- Command: `mvn allure:report` or `allure serve allure-results`
- Features: Timeline, graphs, detailed test steps

#### Custom Reports
- Location: `artifacts/reports/`
- Formats: Markdown summaries, CSV, Excel
- Content: Execution metadata, test statistics

### Viewing Reports

#### Local:
```bash
# Open Surefire HTML report
open target/site/surefire-report.html

# Serve Allure report
mvn allure:serve
# or
allure serve allure-results
```

#### GitHub Actions:
1. Go to **Actions** tab
2. Select workflow run
3. Download artifacts:
   - `ui-test-results-{browser}-{timestamp}`
   - `api-test-results-{timestamp}`
   - `consolidated-test-reports-{timestamp}`
   - `dashboard-package-{timestamp}`

---

## Configuration

### Environment Variables

#### For GitHub Actions (set in repository secrets):
```yaml
ANALYTICS_ENDPOINT_URL     # Dashboard analytics endpoint
ANALYTICS_TOKEN            # Authentication token
DB_DATABASE                # Database name
DB_USER                    # Database user
DB_PASSWORD                # Database password
```

#### For Local Execution:
Set in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/test_automation
spring.datasource.username=testuser
spring.datasource.password=Test@1234
```

### TestNG Configuration

Modify `testng.xml`, `testng-ui.xml`, or `testng-api.xml`:

```xml
<suite name="Suite Name" parallel="methods" thread-count="5">
    <test name="Test Name">
        <classes>
            <class name="org.automation.ui.YourTestClass"/>
        </classes>
    </test>
</suite>
```

---

## API Endpoints

### Dashboard APIs

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/dashboard` | GET | Main dashboard view |
| `/dashboard/analytics` | GET | Cumulative analysis view |
| `/dashboard/test-manager` | GET | Test management interface |
| `/dashboard/reports` | GET | Reports browser |
| `/dashboard/api/stats` | GET | Test execution statistics |
| `/dashboard/api/test-matrix` | GET | Test execution matrix data |
| `/dashboard/api/failure-heatmap` | GET | Failure heatmap data |
| `/dashboard/api/export/csv` | GET | Export results as CSV |
| `/dashboard/api/export/excel` | GET | Export results as Excel |
| `/dashboard/run-tests` | POST | Execute test suite |

### Query Parameters

**Test Matrix API:**
```
GET /dashboard/api/test-matrix?days=30&suite=ui&status=FAIL
```
- `days`: Number of days to analyze (default: 30)
- `suite`: Filter by suite (all/ui/api)
- `status`: Filter by status (all/PASS/FAIL/SKIP)

**Failure Heatmap API:**
```
GET /dashboard/api/failure-heatmap?days=30
```
- `days`: Number of days for heatmap (default: 30)

---

## Troubleshooting

### Common Issues

**1. Script Permission Denied**
```bash
chmod +x run-specific-test.sh
```

**2. Tests Not Found**
- Verify test class name includes full package: `org.automation.ui.BlazeDemoTests`
- Check TestNG XML files contain the test class

**3. Dashboard Not Loading**
```bash
# Rebuild and restart
mvn clean package -DskipTests
mvn spring-boot:run
```

**4. Reports Not Generated**
- Ensure `-Dmaven.test.failure.ignore=true` is set
- Check `target/surefire-reports/` exists
- Run `mvn surefire-report:report-only`

**5. GitHub Actions Failing**
- Check workflow syntax in `.github/workflows/Auto_checkup.yml`
- Verify secrets are configured
- Review workflow logs for specific errors

---

## Best Practices

### 1. Test Organization
- Keep UI tests in `src/test/java/org/automation/ui/`
- Keep API tests in `src/test/java/org/automation/api/`
- Use descriptive test names

### 2. Running Tests
- Use specific tests during development
- Run full suite before commits
- Let CI/CD handle comprehensive testing

### 3. Report Management
- Review reports after each run
- Archive important reports
- Use dashboard for trend analysis

### 4. Dashboard Usage
- Check analytics daily for test health
- Monitor failure heatmap for flaky tests
- Export data for stakeholder reports

---

## Support & Documentation

- **GitHub Issues**: Report bugs or request features
- **Dashboard**: Real-time metrics at `/dashboard`
- **Logs**: Check `logs/` directory for execution logs
- **Test Output**: Review `test-output/` for detailed TestNG output

---

## Version History

### v2.0.0 (Current)
- ✅ Added support for specific test file execution
- ✅ Automatic report generation for all executions
- ✅ Cumulative test analysis matrix dashboard
- ✅ Failure heatmap visualization
- ✅ CSV/Excel export functionality
- ✅ Enhanced GitHub Actions workflow
- ✅ Local execution script with full options

### v1.0.0
- Initial release with UI and API test support
- Basic dashboard functionality
- GitHub Actions integration

---

**Last Updated:** $(date)
**Framework Version:** 2.0.0
**Maintainer:** Springboard Team

