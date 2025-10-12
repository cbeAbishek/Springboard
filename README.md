# Springboard Test Automation Framework

A comprehensive Spring Boot-based test automation framework that supports UI testing with Selenium WebDriver, API testing, parallel execution, comprehensive reporting, and scheduled test runs with performance monitoring.

## Features

- **Multi-layer Testing**: Support for UI, API, and database testing
- **Parallel Test Execution**: Configurable parallel execution of tests using TestNG
- **Comprehensive Reporting**: HTML, CSV, XML report generation with screenshots
- **Test Scheduling**: Cron-based test scheduling with email notifications
- **Real-time Monitoring**: Dashboard for test execution metrics
- **Data-driven Testing**: Externalized test data in JSON format
- **CI/CD Integration**: Ready configurations for continuous integration

## Technology Stack

- **Java 17+**: Core programming language
- **Spring Boot 3.x**: Application framework with dependency injection
- **Selenium WebDriver**: Browser automation for UI testing
- **TestNG**: Test execution and parallel test orchestration
- **RESTAssured**: API testing library
- **H2/PostgreSQL**: Database storage for test results
- **Logback**: Logging framework
- **Maven**: Build and dependency management

## Getting Started

### Prerequisites

- Java 17+ installed
- Maven 3.6+ installed
- Chrome/Firefox browser installed

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/springboard.git
   cd springboard
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   ./run_springboard.sh
   ```
   
   Or using Maven:
   ```bash
   mvn spring-boot:run
   ```

### Verify WebDriver Installation

```bash
./verify_webdriver.sh
```

## Project Structure

```
springboard/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/
│   │   │       ├── controller/      # REST API controllers
│   │   │       ├── engine/          # Test execution engines
│   │   │       ├── model/           # Data models
│   │   │       ├── repository/      # Database repositories
│   │   │       ├── service/         # Business logic services
│   │   │       └── tests/           # Test implementations
│   │   └── resources/
│   │       ├── static/              # Frontend assets
│   │       ├── db/migration/        # Database migrations
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       ├── java/
│       │   └── org/example/         # Unit tests
│       └── resources/
│           ├── test-data.json       # Test data
│           └── testng.xml           # TestNG configuration
```

## Usage

### Running Tests

Run all tests:
```bash
mvn test
```

Run specific test suite:
```bash
mvn test -Dsuite=APITests
```

Run tests with specific browser:
```bash
mvn test -Dbrowser=firefox
```

### Creating New Tests

1. **API Tests**: Create a new class in `org.example.tests.api` extending `BaseAPITest`
2. **UI Tests**: Create a new class in `org.example.tests.ui` extending `BaseUITest`

Example API Test:
```java
@Test(groups = {"api", "smoke"})
public void testGetUserAPI() {
    given()
        .when()
        .get("/api/users/2")
        .then()
        .statusCode(200)
        .body("data.id", equalTo(2));
}
```

Example UI Test:
```java
@Test(groups = {"ui", "functional"})
public void testLoginForm() {
    driver.get("https://your-application.com/login");
    driver.findElement(By.id("username")).sendKeys("testuser");
    driver.findElement(By.id("password")).sendKeys("password");
    driver.findElement(By.id("loginButton")).click();
    
    String pageTitle = driver.getTitle();
    Assert.assertTrue(pageTitle.contains("Dashboard"));
}
```

### Test Data Management

Update the test data file at `src/test/resources/test-data.json` to add or modify test data.

## Configuration

### Application Properties

Key application settings in `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:file:./testdb
spring.datasource.driverClassName=org.h2.Driver

# Server Configuration
server.port=8080

# Test Framework Configuration
automation.framework.webDriver.defaultBrowser=chrome
automation.framework.webDriver.headless=false
automation.framework.webDriver.implicitWait=10
automation.framework.reporting.outputPath=test-reports/
```

### TestNG Configuration

Customize test execution in `src/test/resources/testng.xml`:
- Configure parallel execution
- Group tests by category
- Set browser parameters

## Reporting

Reports are generated in the `test-reports/` directory in multiple formats:
- HTML reports with screenshots
- CSV reports for data analysis
- XML reports for CI/CD integration

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
