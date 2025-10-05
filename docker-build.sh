#!/bin/bash

# Build and run the Test Automation Framework using Docker

set -e

echo "========================================="
echo "Building Test Automation Framework Docker Image"
echo "========================================="

# Build the Docker image
docker build -t cbeabishek/test-automation-framework:latest .

echo ""
echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo ""
echo "To run the application with MySQL, use:"
echo "  docker-compose up -d"
echo ""
echo "To run only the application (with H2):"
echo "  docker run -p 8080:8080 cbeabishek/test-automation-framework:latest"
echo ""
echo "To run tests inside the container:"
echo "  docker run --rm -v \$(pwd)/artifacts:/app/artifacts cbeabishek/test-automation-framework:latest \\"
echo "    sh -c 'mvn test -Dsuite=api'"
echo ""

