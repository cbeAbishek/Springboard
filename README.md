# Testing Framework using Java, Maven, and Spring Boot

## Overview

This project demonstrates how to create a **Testing Framework** using **Java**, **Maven**, and **Spring Boot**. It was developed as part of an Infosys internship project to ensure modular, scalable, and maintainable automated tests for Spring Boot applications.

The framework now includes **Test Execution Integration** with both **UI (Selenium)** and **API (REST-Assured)** test capabilities, allowing you to execute tests through REST APIs and save results to the database.

---

## Features

* **Spring Boot Integration** for application testing.
* **JUnit** for unit and integration testing.
* **Maven** for dependency management and build automation.
* **Test Suites** for organized test execution.
* **Environment Configuration** using `application.properties` and profiles.
* **Extensible Structure** for adding new test cases easily.
* **🆕 UI Test Execution** using Selenium WebDriver (Google Search Test).
* **🆕 API Test Execution** using REST-Assured (GitHub API Test).
* **🆕 Test Execution Service** to run tests programmatically.
* **🆕 REST APIs** for triggering test execution and retrieving results.
* **🆕 Database Integration** for storing test results with timestamps.

---

## Prerequisites

Ensure the following are installed:

* **Java 17** or above
* **Apache Maven** (latest version)
* **Spring Boot** (via Maven dependencies)
* **IDE** like IntelliJ IDEA or Eclipse

---

## Project Structure

```
project-root
│   pom.xml               # Maven configuration file
│
├── src
│   ├── main
│   │   ├── java          # Application source code
│   │   └── resources     # Application resources
│   │
│   └── test
│       ├── java          # Test classes (unit and integration)
│       └── resources     # Test configuration files
│
└── README.md             # Documentation
```

---

## Steps to Create the Framework

### 1. **Initialize Maven Project**

* Use Maven archetype to create a Spring Boot project.
* Configure `pom.xml` for dependencies:

    * Spring Boot Starter
    * Spring Boot Starter Test
    * JUnit 5

### 2. **Configure Application Properties**

* Set test-specific properties in `src/test/resources/application-test.properties`.
* Use Spring Profiles to separate dev, test, and prod configurations.

### 3. **Write Test Classes**

* Create unit tests for individual components (services, controllers).
* Implement integration tests for end-to-end flow using Spring Boot Test.
* Use annotations like `@SpringBootTest`, `@Test`, `@BeforeAll`, and `@AfterAll`.

### 4. **Organize Test Suites**

* Group related tests into suites for efficient execution.
* Define suite classes to run multiple tests together.

### 5. **Run Tests**

* Execute tests via Maven:

  ```
  mvn test
  ```
* View test results in the console or generated reports.

---

## Test Execution Integration

### Sample Tests Available

#### 1. Google Search Test (UI)
- **Type**: UI (Selenium WebDriver)
- **Description**: Opens Google homepage, searches for "Spring Boot Testing", validates page title contains "Spring Boot"
- **Test Class**: `GoogleSearchTest`
- **Returns**: PASS/FAIL based on title validation

#### 2. GitHub API Test (API)
- **Type**: API (REST-Assured)
- **Description**: Calls GET https://api.github.com and validates response status code is 200
- **Test Class**: `GitHubApiTest`
- **Returns**: PASS/FAIL based on status code validation

### Test Execution APIs

#### Execute Test Case
```http
POST /api/tests/run/{id}
```

**Description**: Triggers execution of a test case by ID. Supports both UI and API test types.

**Path Parameters**:
- `id` (Long): ID of the test case to execute

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/tests/run/1
```

**Example Response** (Success):
```json
{
  "success": true,
  "message": "Test case executed successfully",
  "testResult": {
    "id": 1,
    "testCaseId": 1,
    "status": "PASSED",
    "executedAt": "2024-08-29T10:30:00",
    "duration": 3500,
    "notes": "UI test completed successfully | Google Search Test - Searches for 'Spring Boot Testing' and validates page title contains 'Spring Boot'",
    "createdAt": "2024-08-29T10:30:03"
  }
}
```

**Example Response** (Test Not Found):
```json
{
  "success": false,
  "message": "Test case with ID 999 not found or not executable",
  "supportedTypes": ["UI", "API"]
}
```

#### Get Latest Test Result
```http
GET /api/tests/result/{id}
```

**Description**: Retrieves the latest test execution result for a specific test case ID.

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/tests/result/1
```

#### Service Health Check
```http
GET /api/tests/health
```

**Description**: Health check endpoint for test execution service.

**Example Response**:
```json
{
  "service": "Test Execution Service",
  "status": "UP",
  "timestamp": "2024-08-29T10:30:00",
  "supportedTestTypes": ["UI", "API"]
}
```

### Testing the Complete Flow

#### Step 1: Create a Test Case (if not exists)
```bash
# Create UI Test Case
curl -X POST http://localhost:8080/api/tests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Google Search UI Test",
    "type": "UI",
    "description": "Selenium test for Google search functionality",
    "status": "Active"
  }'

# Create API Test Case
curl -X POST http://localhost:8080/api/tests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GitHub API Test",
    "type": "API", 
    "description": "REST-Assured test for GitHub API",
    "status": "Active"
  }'
```

#### Step 2: Execute the Test
```bash
# Execute test case with ID 1
curl -X POST http://localhost:8080/api/tests/run/1
```

#### Step 3: Check Results
```bash
# Get latest result for test case 1
curl -X GET http://localhost:8080/api/tests/result/1

# Or check all test results
curl -X GET http://localhost:8080/api/test-results
```

### Prerequisites for Test Execution

#### For UI Tests (Selenium):
1. **ChromeDriver**: Ensure ChromeDriver is installed and available in PATH, or use WebDriverManager
2. **Chrome Browser**: Required for Selenium tests (runs in headless mode by default)
3. **Internet Connection**: Required for accessing Google

#### For API Tests (REST-Assured):
1. **Internet Connection**: Required for accessing GitHub API
2. **No additional setup** required

### Database Schema

Test results are automatically saved to the `test_results` table with the following structure:

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| test_case_id | BIGINT | Foreign key to test_cases table |
| status | VARCHAR(50) | PASSED/FAILED |
| executed_at | DATETIME | When the test was executed |
| duration | BIGINT | Execution duration in milliseconds |
| notes | TEXT | Test details and error messages |
| created_at | DATETIME | When the record was created |

### Supported Test Types

- **UI**: Runs Selenium-based UI tests (GoogleSearchTest)
- **API**: Runs REST-Assured-based API tests (GitHubApiTest)

### Error Handling

The framework includes comprehensive error handling for:
- Test case not found
- Unsupported test types
- Test execution failures
- Database connection issues
- Network connectivity problems

---

## Best Practices

* Follow **naming conventions** for test classes and methods.
* Maintain **separate configuration files** for different environments.
* Use **mocking frameworks** like Mockito for dependency isolation.
* Ensure **high code coverage** for critical components.

---

## Deliverables

* Complete Maven-based Spring Boot testing framework.
* Comprehensive test cases for the application.
* Documentation of test execution steps and reports.

---

## Future Enhancements

* Add support for **TestNG** for advanced testing features.
* Integrate **SonarQube** for code quality analysis.
* Include **Continuous Integration (CI)** using Jenkins or GitHub Actions.

---

## Author

**Infosys Internship Project Team**

---

## License

This project is intended for educational purposes as part of an internship and follows standard open-source licensing policies.
