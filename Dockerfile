# Multi-stage Dockerfile for Spring Boot Test Automation Framework

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY testng.xml testng-api.xml testng-ui.xml ./

# Build the application (skip tests during build)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

# Install Chrome and dependencies for UI tests
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    firefox \
    xvfb \
    dbus \
    ttf-freefont \
    fontconfig \
    curl \
    wget

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy test configuration files
COPY testng.xml testng-api.xml testng-ui.xml ./

# Create necessary directories
RUN mkdir -p /app/logs /app/artifacts/screenshots /app/artifacts/reports /app/allure-results \
    && chown -R appuser:appgroup /app

# Set environment variables
ENV JAVA_OPTS="-Xmx2048m -XX:+UseG1GC" \
    DISPLAY=:99 \
    CHROME_BIN=/usr/bin/chromium-browser \
    CHROMEDRIVER_BIN=/usr/bin/chromedriver \
    FIREFOX_BIN=/usr/bin/firefox

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Entry point script to handle both web server and test execution
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Default command (can be overridden)
CMD []
