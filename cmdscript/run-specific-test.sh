#!/bin/bash

################################################################################
# Script: run-specific-test.sh
# Description: Run specific test files or entire test suites with automatic
#              report generation. Works both locally and in CI/CD.
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEST_SUITE="all"
TEST_CLASS=""
TEST_METHOD=""
BROWSER="chrome"
HEADLESS="false"
GENERATE_REPORTS="true"
PARALLEL_THREADS=5

# Function to display usage
usage() {
    echo -e "${BLUE}Usage: $0 [OPTIONS]${NC}"
    echo ""
    echo "Options:"
    echo "  -s, --suite <suite>        Test suite: all, ui, api, specific (default: all)"
    echo "  -c, --class <class>        Specific test class (e.g., org.automation.ui.BlazeDemoTests)"
    echo "  -m, --method <method>      Specific test method (e.g., testBookFlight)"
    echo "  -b, --browser <browser>    Browser: chrome, firefox, edge (default: chrome)"
    echo "  -h, --headless             Run in headless mode"
    echo "  -r, --no-reports           Skip report generation"
    echo "  -t, --threads <num>        Number of parallel threads (default: 5)"
    echo "  --help                     Display this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -s ui -b chrome                    # Run all UI tests with Chrome"
    echo "  $0 -s api                              # Run all API tests"
    echo "  $0 -s specific -c org.automation.ui.BlazeDemoTests"
    echo "  $0 -s specific -c org.automation.ui.BlazeDemoTests -m testBookFlight"
    exit 0
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--suite)
            TEST_SUITE="$2"
            shift 2
            ;;
        -c|--class)
            TEST_CLASS="$2"
            shift 2
            ;;
        -m|--method)
            TEST_METHOD="$2"
            shift 2
            ;;
        -b|--browser)
            BROWSER="$2"
            shift 2
            ;;
        -h|--headless)
            HEADLESS="true"
            shift
            ;;
        -r|--no-reports)
            GENERATE_REPORTS="false"
            shift
            ;;
        -t|--threads)
            PARALLEL_THREADS="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# Function to create required directories
create_directories() {
    echo -e "${BLUE}Creating required directories...${NC}"
    mkdir -p artifacts/screenshots
    mkdir -p artifacts/reports
    mkdir -p artifacts/api
    mkdir -p target/surefire-reports
    mkdir -p allure-results
    mkdir -p logs
}

# Function to run tests
run_tests() {
    local timestamp=$(date +'%Y%m%d_%H%M%S')

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Starting Test Execution${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "Suite: ${GREEN}$TEST_SUITE${NC}"
    echo -e "Browser: ${GREEN}$BROWSER${NC}"
    echo -e "Headless: ${GREEN}$HEADLESS${NC}"
    echo -e "Timestamp: ${GREEN}$timestamp${NC}"

    if [ "$TEST_SUITE" == "specific" ]; then
        if [ -z "$TEST_CLASS" ]; then
            echo -e "${RED}Error: Test class must be specified for specific test suite${NC}"
            exit 1
        fi

        echo -e "Test Class: ${GREEN}$TEST_CLASS${NC}"

        if [ -n "$TEST_METHOD" ]; then
            echo -e "Test Method: ${GREEN}$TEST_METHOD${NC}"
            mvn clean test \
                -Dtest="$TEST_CLASS#$TEST_METHOD" \
                -Dbrowser="$BROWSER" \
                -Dheadless="$HEADLESS" \
                -Dtestng.dtd.http=true \
                -Dmaven.test.failure.ignore=true
        else
            mvn clean test \
                -Dtest="$TEST_CLASS" \
                -Dbrowser="$BROWSER" \
                -Dheadless="$HEADLESS" \
                -Dtestng.dtd.http=true \
                -Dmaven.test.failure.ignore=true
        fi
    else
        mvn clean test \
            -Dsuite="$TEST_SUITE" \
            -Dbrowser="$BROWSER" \
            -Dheadless="$HEADLESS" \
            -Dparallel=methods \
            -DthreadCount="$PARALLEL_THREADS" \
            -Dtestng.dtd.http=true \
            -Dmaven.test.failure.ignore=true
    fi

    local exit_code=$?
    return $exit_code
}

# Function to generate reports
generate_reports() {
    if [ "$GENERATE_REPORTS" == "false" ]; then
        echo -e "${YELLOW}Skipping report generation${NC}"
        return 0
    fi

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Generating Test Reports${NC}"
    echo -e "${BLUE}========================================${NC}"

    # Generate Surefire reports
    echo -e "${BLUE}Generating Surefire reports...${NC}"
    mvn surefire-report:report-only
    mvn site -DgenerateReports=false

    # Generate Allure reports
    if [ -d "allure-results" ] && [ "$(ls -A allure-results)" ]; then
        echo -e "${BLUE}Generating Allure reports...${NC}"
        mvn allure:report || echo -e "${YELLOW}Allure report generation skipped${NC}"
    fi

    # Create summary report
    local timestamp=$(date +'%Y%m%d_%H%M%S')
    local summary_file="artifacts/reports/test-summary-$timestamp.md"

    mkdir -p artifacts/reports

    cat > "$summary_file" << EOF
# Test Execution Summary - $timestamp

## Execution Details
- **Suite:** $TEST_SUITE
- **Browser:** $BROWSER
- **Headless:** $HEADLESS
- **Execution Time:** $(date)
- **Branch:** $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "N/A")
- **Commit:** $(git rev-parse --short HEAD 2>/dev/null || echo "N/A")

## Test Configuration
EOF

    if [ "$TEST_SUITE" == "specific" ]; then
        echo "- **Test Class:** $TEST_CLASS" >> "$summary_file"
        [ -n "$TEST_METHOD" ] && echo "- **Test Method:** $TEST_METHOD" >> "$summary_file"
    fi

    echo "" >> "$summary_file"

    if [ -f "target/surefire-reports/testng-results.xml" ]; then
        echo "## Test Results" >> "$summary_file"
        echo "\`\`\`" >> "$summary_file"
        grep -E "tests=|passed=|failed=|skipped=" target/surefire-reports/testng-results.xml || echo "See detailed reports" >> "$summary_file"
        echo "\`\`\`" >> "$summary_file"
    fi

    echo -e "${GREEN}Summary report created: $summary_file${NC}"

    # Display report locations
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Reports Generated Successfully${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "Surefire Reports: ${BLUE}target/surefire-reports/${NC}"
    echo -e "HTML Reports: ${BLUE}target/site/${NC}"
    echo -e "Allure Results: ${BLUE}allure-results/${NC}"
    echo -e "Summary: ${BLUE}$summary_file${NC}"

    if [ -d "target/allure-report" ]; then
        echo -e "Allure Report: ${BLUE}target/allure-report/index.html${NC}"
    fi
}

# Function to display test results
display_results() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Test Execution Complete${NC}"
    echo -e "${BLUE}========================================${NC}"

    if [ -f "target/surefire-reports/testng-results.xml" ]; then
        local total=$(grep -oP 'total="\K[0-9]+' target/surefire-reports/testng-results.xml | head -1 || echo "0")
        local passed=$(grep -oP 'passed="\K[0-9]+' target/surefire-reports/testng-results.xml | head -1 || echo "0")
        local failed=$(grep -oP 'failed="\K[0-9]+' target/surefire-reports/testng-results.xml | head -1 || echo "0")
        local skipped=$(grep -oP 'skipped="\K[0-9]+' target/surefire-reports/testng-results.xml | head -1 || echo "0")

        echo -e "Total Tests: ${BLUE}$total${NC}"
        echo -e "Passed: ${GREEN}$passed${NC}"
        echo -e "Failed: ${RED}$failed${NC}"
        echo -e "Skipped: ${YELLOW}$skipped${NC}"

        if [ "$failed" -gt 0 ]; then
            echo ""
            echo -e "${RED}⚠️  Some tests failed. Check the reports for details.${NC}"
            return 1
        else
            echo ""
            echo -e "${GREEN}✅ All tests passed successfully!${NC}"
            return 0
        fi
    else
        echo -e "${YELLOW}Test results file not found${NC}"
        return 1
    fi
}

# Main execution
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Test Automation Framework${NC}"
    echo -e "${BLUE}========================================${NC}"

    create_directories

    if run_tests; then
        echo -e "${GREEN}Test execution completed${NC}"
    else
        echo -e "${YELLOW}Test execution completed with some failures${NC}"
    fi

    generate_reports

    if display_results; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main

