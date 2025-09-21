#!/bin/bash
# WebDriver Ubuntu Setup Script for Springboard Framework

echo "Starting WebDriver environment setup for Ubuntu..."

# Set JAVA_HOME to JDK 21 LTS for IntelliJ IDEA compatibility
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Disable Maven color output to prevent ANSI parsing errors
export MAVEN_OPTS="-Djansi.force=false -Dorg.slf4j.simpleLogger.showColors=false"

echo "Using JDK 21 LTS: $(java -version 2>&1 | head -n1)"

# Kill any existing Xvfb processes
pkill Xvfb 2>/dev/null || true

# Start virtual display for headless browser operation
export DISPLAY=:99
Xvfb :99 -screen 0 1920x1080x24 > /dev/null 2>&1 &

# Wait for Xvfb to start
sleep 2

echo "Virtual display :99 started (1920x1080x24)"

# Verify Chrome installation
if command -v google-chrome &> /dev/null; then
    echo "Chrome version: $(google-chrome --version)"
else
    echo "ERROR: Google Chrome not found!"
    exit 1
fi

# Set environment variables for WebDriver
export CHROME_BIN=/usr/bin/google-chrome
export CHROME_OPTS="--no-sandbox --disable-dev-shm-usage --headless=new"

echo "WebDriver environment ready!"
echo "DISPLAY=$DISPLAY"
echo "CHROME_BIN=$CHROME_BIN"
echo "JAVA_HOME=$JAVA_HOME"
echo "MAVEN_OPTS=$MAVEN_OPTS"

# Clean and run the Springboard application
echo "Starting Springboard application with JDK 21..."
cd /home/abishek/IdeaProjects/Springboard

# Force clean to avoid version conflicts
rm -rf target/

# Run Spring Boot with proper Maven settings
mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.awt.headless=true"
