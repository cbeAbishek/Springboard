#!/bin/bash
# Simple compilation test script

echo "Starting compilation test..."
cd /home/abishek/IdeaProjects/Springboard

# Run Maven compile
mvn clean compile -q -DskipTests > /tmp/maven_build.log 2>&1

# Check exit code
if [ $? -eq 0 ]; then
    echo "✓ BUILD SUCCESS"
    echo "Compiled classes:"
    find target/classes -name "*.class" 2>/dev/null | wc -l
    exit 0
else
    echo "✗ BUILD FAILED"
    echo "Errors:"
    cat /tmp/maven_build.log | grep -A 3 "error:" | head -50
    exit 1
fi

