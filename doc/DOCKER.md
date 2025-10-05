# Docker Setup for Test Automation Framework

This guide explains how to build and run the Test Automation Framework using Docker.

## Prerequisites

- Docker installed (version 20.10 or higher)
- Docker Compose installed (version 2.0 or higher)
- At least 4GB of available RAM for the container

## Quick Start

### Option 1: Using Docker Compose (Recommended)

This will start both the application and MySQL database:

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Option 2: Build and Run Manually

```bash
# Build the Docker image
./docker-build.sh

# Or manually:
docker build -t test-automation-framework:latest .

# Run with H2 (in-memory database)
docker run -p 8080:8080 test-automation-framework:latest

# Run with MySQL (requires MySQL running separately)
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/test_automation \
  -e SPRING_DATASOURCE_DRIVER=com.mysql.cj.jdbc.Driver \
  -e SPRING_DATASOURCE_USERNAME=testuser \
  -e SPRING_DATASOURCE_PASSWORD=Test@1234 \
  -e SPRING_JPA_DIALECT=org.hibernate.dialect.MySQLDialect \
  test-automation-framework:latest
```

## Running Tests in Docker

### Run API Tests

```bash
docker run --rm \
  -v $(pwd)/artifacts:/app/artifacts \
  -v $(pwd)/allure-results:/app/allure-results \
  test-automation-framework:latest \
  sh -c "mvn test -Dsuite=api"
```

### Run UI Tests (Headless)

```bash
docker run --rm \
  -v $(pwd)/artifacts:/app/artifacts \
  -v $(pwd)/allure-results:/app/allure-results \
  -e HEADLESS=true \
  test-automation-framework:latest \
  sh -c "mvn test -Dsuite=ui -Dbrowser=chrome -Dheadless=true"
```

### Run All Tests

```bash
docker run --rm \
  -v $(pwd)/artifacts:/app/artifacts \
  -v $(pwd)/allure-results:/app/allure-results \
  test-automation-framework:latest \
  sh -c "mvn test"
```

## Accessing the Application

Once the container is running:

- **Dashboard**: http://localhost:8080/dashboard
- **H2 Console** (if using H2): http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health

## Docker Compose Services

### Services Overview

- **mysql**: MySQL 8.0 database server
- **app**: Test Automation Framework application

### Environment Variables

You can customize the following environment variables in `docker-compose.yml`:

```yaml
# Database Configuration
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/test_automation
SPRING_DATASOURCE_USERNAME: testuser
SPRING_DATASOURCE_PASSWORD: Test@1234

# JVM Options
JAVA_OPTS: "-Xmx2048m -XX:+UseG1GC"
```

## Volume Mounts

The following directories are mounted as volumes:

- `./logs` → `/app/logs` - Application logs
- `./artifacts` → `/app/artifacts` - Test reports and screenshots
- `./allure-results` → `/app/allure-results` - Allure test results

## Troubleshooting

### Container fails to start

Check logs:
```bash
docker-compose logs app
```

### MySQL connection issues

Ensure MySQL is healthy:
```bash
docker-compose ps
docker-compose logs mysql
```

### Permission issues with volumes

Fix permissions:
```bash
sudo chown -R $USER:$USER logs artifacts allure-results
```

### Out of memory errors

Increase memory in `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xmx4096m -XX:+UseG1GC"
```

## Advanced Usage

### Custom TestNG Suite

```bash
docker run --rm \
  -v $(pwd)/testng-custom.xml:/app/testng-custom.xml \
  -v $(pwd)/artifacts:/app/artifacts \
  test-automation-framework:latest \
  sh -c "mvn test -DsuiteXmlFile=testng-custom.xml"
```

### Interactive Shell

```bash
docker run -it --rm \
  -v $(pwd)/artifacts:/app/artifacts \
  test-automation-framework:latest \
  /bin/sh
```

### Build with Custom Tag

```bash
docker build -t test-automation-framework:v1.0.0 .
```

### Push to Docker Registry

```bash
# Tag for your registry
docker tag test-automation-framework:latest your-registry/test-automation-framework:latest

# Push to registry
docker push your-registry/test-automation-framework:latest
```

## Image Details

- **Base Image**: eclipse-temurin:21-jre-alpine
- **Browsers**: Chromium, Firefox
- **Display Server**: Xvfb (for headless UI testing)
- **Java Version**: 21
- **User**: appuser (non-root)

## Maintenance

### Clean up Docker resources

```bash
# Remove stopped containers
docker-compose down

# Remove all containers and volumes
docker-compose down -v

# Clean up unused images
docker image prune -a

# Clean up everything
docker system prune -a --volumes
```

### Update the image

```bash
# Rebuild without cache
docker-compose build --no-cache

# Or manually
docker build --no-cache -t test-automation-framework:latest .
```

## Security Considerations

- The application runs as a non-root user (`appuser`)
- Sensitive credentials should be passed via environment variables
- Use Docker secrets for production deployments
- Keep base images updated regularly

## Performance Tips

1. Use volume mounts for large datasets
2. Adjust JVM heap size based on your tests
3. Use Docker BuildKit for faster builds:
   ```bash
   DOCKER_BUILDKIT=1 docker build -t test-automation-framework:latest .
   ```

## Support

For issues related to:
- Docker setup: Check this README
- Application: See main README.md
- Tests: See test documentation in `src/test/README.md`

