#!/bin/bash

echo "=============================================="
echo "Final Compilation and Verification Script"
echo "=============================================="
echo ""

cd /home/abishek/IdeaProjects/Springboard

# Step 1: Clean
echo "Step 1: Cleaning project..."
mvn clean -q
echo "✓ Clean complete"
echo ""

# Step 2: Compile
echo "Step 2: Compiling project..."
mvn compile -DskipTests 2>&1 | tee /tmp/final_compile.log

# Check result
if grep -q "BUILD SUCCESS" /tmp/final_compile.log; then
    echo ""
    echo "=============================================="
    echo "✓✓✓ BUILD SUCCESS ✓✓✓"
    echo "=============================================="
    echo ""
    echo "Compiled classes:"
    find target/classes -name "*.class" 2>/dev/null | wc -l
    echo ""
    echo "Report system files compiled:"
    find target/classes/org/automation/reports -name "*.class" 2>/dev/null | wc -l
    echo ""
    echo "Next steps:"
    echo "1. Setup MySQL database: mysql -u root -pCk@709136 < src/main/resources/schema.sql"
    echo "2. Run tests: mvn clean test -Dsuite=ui"
    echo "3. Start web app: mvn spring-boot:run"
    echo "4. View reports: http://localhost:8080/reports"
    echo ""
elif grep -q "BUILD FAILURE" /tmp/final_compile.log; then
    echo ""
    echo "=============================================="
    echo "✗✗✗ BUILD FAILED ✗✗✗"
    echo "=============================================="
    echo ""
    echo "Errors found:"
    grep -A 5 "error:" /tmp/final_compile.log | head -50
    echo ""
    echo "Full log saved to: /tmp/final_compile.log"
    echo "Please send the error messages above for further assistance."
else
    echo "Unable to determine build status. Check /tmp/final_compile.log"
fi

echo ""
echo "=============================================="

