#!/bin/bash

# Validation Script for Test Reporting System
# This script validates that all components are properly set up

echo "=========================================="
echo "Test Reporting System - Validation"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

passed=0
failed=0

# Function to check
check() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $1"
        ((passed++))
    else
        echo -e "${RED}✗${NC} $1"
        ((failed++))
    fi
}

# 1. Check Java
echo "Checking Prerequisites..."
java -version > /dev/null 2>&1
check "Java is installed"

# 2. Check Maven
mvn -version > /dev/null 2>&1
check "Maven is installed"

# 3. Check MySQL
if command -v mysql &> /dev/null; then
    echo -e "${GREEN}✓${NC} MySQL command is available"
    ((passed++))
else
    echo -e "${YELLOW}⚠${NC} MySQL command not found (may still work if service is running)"
fi

# 4. Check project structure
echo ""
echo "Checking Project Structure..."

[ -f "pom.xml" ]
check "pom.xml exists"

[ -d "src/main/java/org/automation/reports" ]
check "Reports package exists"

[ -f "src/main/java/org/automation/reports/ReportManager.java" ]
check "ReportManager.java exists"

[ -f "src/main/java/org/automation/reports/model/TestReport.java" ]
check "TestReport model exists"

[ -f "src/main/java/org/automation/reports/model/TestReportDetail.java" ]
check "TestReportDetail model exists"

[ -f "src/main/java/org/automation/reports/repository/TestReportRepository.java" ]
check "TestReportRepository exists"

[ -f "src/main/java/org/automation/reports/service/ReportService.java" ]
check "ReportService exists"

[ -f "src/main/java/org/automation/reports/controller/ReportController.java" ]
check "ReportController (REST API) exists"

[ -f "src/main/java/org/automation/reports/controller/ReportViewController.java" ]
check "ReportViewController exists"

# 5. Check templates
echo ""
echo "Checking Web Templates..."

[ -f "src/main/resources/templates/reports.html" ]
check "reports.html template exists"

[ -f "src/main/resources/templates/report-details.html" ]
check "report-details.html template exists"

[ -f "src/main/resources/templates/aggregated-report.html" ]
check "aggregated-report.html template exists"

# 6. Check configuration
echo ""
echo "Checking Configuration..."

[ -f "src/main/resources/application.properties" ]
check "application.properties exists"

[ -f "src/main/resources/schema.sql" ]
check "schema.sql exists"

# 7. Check listeners
echo ""
echo "Checking Test Listeners..."

[ -f "src/test/java/org/automation/listeners/TestListener.java" ]
check "TestListener exists"

[ -f "src/test/java/org/automation/listeners/SuiteExecutionListener.java" ]
check "SuiteExecutionListener exists"

# 8. Check utilities
echo ""
echo "Checking Utilities..."

[ -f "src/test/java/org/automation/utils/ScreenshotUtils.java" ]
check "ScreenshotUtils exists"

# 9. Check directories
echo ""
echo "Checking Directories..."

if [ ! -d "artifacts/reports" ]; then
    mkdir -p artifacts/reports
    echo -e "${GREEN}✓${NC} Created artifacts/reports directory"
    ((passed++))
else
    echo -e "${GREEN}✓${NC} artifacts/reports directory exists"
    ((passed++))
fi

# 10. Check documentation
echo ""
echo "Checking Documentation..."

[ -f "REPORTING_SYSTEM_README.md" ]
check "REPORTING_SYSTEM_README.md exists"

[ -f "QUICK_START.md" ]
check "QUICK_START.md exists"

[ -f "IMPLEMENTATION_SUMMARY.md" ]
check "IMPLEMENTATION_SUMMARY.md exists"

[ -f "run-tests.sh" ]
check "run-tests.sh script exists"

# 11. Check MySQL configuration
echo ""
echo "Checking MySQL Configuration..."

if grep -q "spring.datasource.url=jdbc:mysql" src/main/resources/application.properties; then
    echo -e "${GREEN}✓${NC} MySQL datasource configured"
    ((passed++))
else
    echo -e "${RED}✗${NC} MySQL datasource not configured"
    ((failed++))
fi

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo -e "${GREEN}Passed: $passed${NC}"
if [ $failed -gt 0 ]; then
    echo -e "${RED}Failed: $failed${NC}"
else
    echo -e "${GREEN}Failed: $failed${NC}"
fi
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Your reporting system is ready to use.${NC}"
    echo ""
    echo "Next Steps:"
    echo "1. Ensure MySQL is running: sudo systemctl start mysql"
    echo "2. Create database: mysql -u root -prooT@12345 < src/main/resources/schema.sql"
    echo "3. Run tests: ./run-tests.sh"
    echo "4. View reports: mvn spring-boot:run → http://localhost:8080/reports"
else
    echo -e "${RED}✗ Some checks failed. Please review the issues above.${NC}"
fi

echo ""

