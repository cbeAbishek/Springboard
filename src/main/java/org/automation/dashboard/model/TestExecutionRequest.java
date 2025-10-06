package org.automation.dashboard.model;

/**
 * Model class for test execution requests
 */
public class TestExecutionRequest {

    private String suite;
    private String browser;
    private String environment;
    private int threadCount;
    private boolean headless;
    private boolean captureScreenshots;
    private boolean generateReports;
    private String testParameters;
    private String testClass;

    // Constructors
    public TestExecutionRequest() {
        this.browser = "chrome";
        this.environment = "test";
        this.threadCount = 3;
        this.headless = false;
        this.captureScreenshots = true;
        this.generateReports = true;
    }

    // Getters
    public String getSuite() { return suite; }
    public String getBrowser() { return browser; }
    public String getEnvironment() { return environment; }
    public int getThreadCount() { return threadCount; }
    public boolean isHeadless() { return headless; }
    public boolean isCaptureScreenshots() { return captureScreenshots; }
    public boolean isGenerateReports() { return generateReports; }
    public String getTestParameters() { return testParameters; }
    public String getTestClass() { return testClass; }

    // Setters
    public void setSuite(String suite) { this.suite = suite; }
    public void setBrowser(String browser) { this.browser = browser; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public void setCaptureScreenshots(boolean captureScreenshots) { this.captureScreenshots = captureScreenshots; }
    public void setGenerateReports(boolean generateReports) { this.generateReports = generateReports; }
    public void setTestParameters(String testParameters) { this.testParameters = testParameters; }
    public void setTestClass(String testClass) { this.testClass = testClass; }

    @Override
    public String toString() {
        return "TestExecutionRequest{" +
                "suite='" + suite + '\'' +
                ", browser='" + browser + '\'' +
                ", environment='" + environment + '\'' +
                ", threadCount=" + threadCount +
                ", headless=" + headless +
                ", captureScreenshots=" + captureScreenshots +
                ", generateReports=" + generateReports +
                '}';
    }
}

