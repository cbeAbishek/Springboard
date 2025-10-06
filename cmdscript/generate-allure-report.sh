#!/bin/bash

# Script to generate Allure report from test results

echo "Generating Allure report..."

# Check if allure-results directory exists
if [ ! -d "allure-results" ]; then
    echo "Error: allure-results directory not found"
    exit 1
fi

# Check if allure-results has files
if [ -z "$(ls -A allure-results)" ]; then
    echo "Error: allure-results directory is empty. Run tests first."
    exit 1
fi

# Generate report using local Allure installation
if [ -f "$HOME/allure/bin/allure" ]; then
    echo "Using Allure from ~/allure/bin/allure"
    $HOME/allure/bin/allure generate allure-results -o allure-report --clean
elif command -v allure &> /dev/null; then
    echo "Using system Allure"
    allure generate allure-results -o allure-report --clean
else
    echo "Error: Allure CLI not found. Please install Allure."
    exit 1
fi

if [ $? -eq 0 ]; then
    echo "✅ Allure report generated successfully in allure-report/"
    echo "📊 Access it at: http://localhost:8080/allure-report"
else
    echo "❌ Failed to generate Allure report"
    exit 1
fi

