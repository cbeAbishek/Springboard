// Test Manager JavaScript

let isExecuting = false;
let executionInterval = null;
let currentExecutionData = {
    passed: 0,
    failed: 0,
    skipped: 0,
    duration: 0,
    total: 0
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initTestManager();
    loadTestSuites();
    loadRecentExecutions();
    setupEventListeners();
});

// Initialize test manager
function initTestManager() {
    console.log('Test Manager initialized');

    // Set active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') === '/dashboard/test-manager') {
            link.classList.add('active');
        }
    });
}

// Setup event listeners
function setupEventListeners() {
    const form = document.getElementById('testExecutionForm');
    const parallelCheckbox = document.getElementById('parallelExecution');
    const threadCountGroup = document.getElementById('threadCountGroup');

    // Toggle thread count visibility
    if (parallelCheckbox) {
        parallelCheckbox.addEventListener('change', function(e) {
            threadCountGroup.style.display = e.target.checked ? 'block' : 'none';
        });
    }

    // Handle form submission
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            startTestExecution();
        });
    }

    // Stop tests button
    const stopBtn = document.getElementById('stopTestsBtn');
    if (stopBtn) {
        stopBtn.addEventListener('click', stopTestExecution);
    }
}

// Load available test suites
async function loadTestSuites() {
    try {
        const suites = await fetchTestSuites();
        renderTestSuites(suites);
    } catch (error) {
        console.error('Error loading test suites:', error);
        showNotification('Failed to load test suites', 'danger');
    }
}

// Fetch test suites
async function fetchTestSuites() {
    // Simulate API call
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                {
                    name: 'API Test Suite',
                    type: 'API',
                    count: 45,
                    lastRun: '2 hours ago',
                    status: 'passed',
                    successRate: 95.6
                },
                {
                    name: 'UI Test Suite',
                    type: 'UI',
                    count: 32,
                    lastRun: '3 hours ago',
                    status: 'failed',
                    successRate: 87.5
                },
                {
                    name: 'Integration Tests',
                    type: 'Integration',
                    count: 28,
                    lastRun: '5 hours ago',
                    status: 'passed',
                    successRate: 100
                },
                {
                    name: 'Regression Suite',
                    type: 'Regression',
                    count: 67,
                    lastRun: '1 day ago',
                    status: 'passed',
                    successRate: 97.0
                },
                {
                    name: 'Smoke Test Suite',
                    type: 'Smoke',
                    count: 15,
                    lastRun: '1 day ago',
                    status: 'passed',
                    successRate: 100
                }
            ]);
        }, 500);
    });
}

// Render test suites table
function renderTestSuites(suites) {
    const tbody = document.getElementById('testSuitesTable');
    if (!tbody) return;

    tbody.innerHTML = suites.map((suite, index) => `
        <tr style="animation: fadeIn 0.5s ease ${index * 0.1}s both;">
            <td><strong>${suite.name}</strong></td>
            <td><span class="badge badge-secondary">${suite.type}</span></td>
            <td>${suite.count} tests</td>
            <td>${suite.lastRun}</td>
            <td><span class="badge badge-${suite.status === 'passed' ? 'success' : 'danger'}">${suite.status}</span></td>
            <td>
                <div class="d-flex align-items-center gap-1">
                    <span>${suite.successRate}%</span>
                    <div class="progress" style="width: 60px; height: 6px;">
                        <div class="progress-bar ${suite.successRate >= 90 ? 'success' : suite.successRate >= 70 ? 'warning' : 'danger'}" 
                             style="width: ${suite.successRate}%"></div>
                    </div>
                </div>
            </td>
            <td>
                <button class="btn btn-primary btn-sm" onclick="quickRunSuite('${suite.name.toLowerCase().replace(/\s+/g, '-')}')">
                    Quick Run
                </button>
            </td>
        </tr>
    `).join('');
}

// Start test execution
async function startTestExecution() {
    if (isExecuting) {
        showNotification('Test execution is already running', 'warning');
        return;
    }

    const suite = document.getElementById('testSuite').value;
    if (!suite) {
        showNotification('Please select a test suite', 'warning');
        return;
    }

    isExecuting = true;
    updateExecutionUI(true);

    const environment = document.getElementById('environment').value;
    const browser = document.getElementById('browser').value;
    const parallel = document.getElementById('parallelExecution').checked;
    const threads = parallel ? document.getElementById('threadCount').value : 1;

    showNotification(`Starting ${suite} test execution on ${environment}...`, 'info');

    // Reset execution data
    currentExecutionData = {
        passed: 0,
        failed: 0,
        skipped: 0,
        duration: 0,
        total: getTestCount(suite)
    };

    // Show execution progress
    document.getElementById('executionStatus').style.display = 'none';
    document.getElementById('executionProgress').style.display = 'block';
    document.getElementById('statusBadge').textContent = 'Running';
    document.getElementById('statusBadge').className = 'badge badge-warning';

    // Simulate test execution
    simulateTestExecution();
}

// Get test count for suite
function getTestCount(suite) {
    const counts = {
        'all': 247,
        'api': 45,
        'ui': 32,
        'integration': 28,
        'regression': 67,
        'smoke': 15
    };
    return counts[suite] || 50;
}

// Simulate test execution
function simulateTestExecution() {
    let progress = 0;
    const totalTests = currentExecutionData.total;
    let currentTest = 0;

    executionInterval = setInterval(() => {
        if (!isExecuting) {
            clearInterval(executionInterval);
            return;
        }

        progress += Math.random() * 5;
        currentTest = Math.floor((progress / 100) * totalTests);

        if (progress >= 100) {
            progress = 100;
            currentTest = totalTests;
            completeTestExecution();
            clearInterval(executionInterval);
            return;
        }

        // Update progress
        updateExecutionProgress(progress, currentTest, totalTests);

        // Simulate test results
        if (Math.random() > 0.2) {
            currentExecutionData.passed++;
        } else {
            if (Math.random() > 0.5) {
                currentExecutionData.failed++;
            } else {
                currentExecutionData.skipped++;
            }
        }

        currentExecutionData.duration += 0.5;

        // Update UI
        document.getElementById('runningPassed').textContent = currentExecutionData.passed;
        document.getElementById('runningFailed').textContent = currentExecutionData.failed;
        document.getElementById('runningSkipped').textContent = currentExecutionData.skipped;
        document.getElementById('runningDuration').textContent = currentExecutionData.duration.toFixed(1) + 's';

    }, 500);
}

// Update execution progress
function updateExecutionProgress(progress, current, total) {
    const progressBar = document.getElementById('overallProgress');
    const progressText = document.getElementById('progressPercentage');
    const currentTestName = document.getElementById('currentTestName');

    progressBar.style.width = progress + '%';
    progressText.textContent = Math.floor(progress) + '%';

    const testNames = [
        'LoginTest.testValidCredentials',
        'UserManagementTest.testCreateUser',
        'APITest.testGetEndpoint',
        'SearchTest.testSearchFunctionality',
        'CheckoutTest.testPaymentProcess',
        'NavigationTest.testMenuNavigation',
        'FormTest.testFormValidation',
        'DataTest.testDataPersistence'
    ];

    const randomTest = testNames[Math.floor(Math.random() * testNames.length)];
    currentTestName.textContent = `Running: ${randomTest} (${current}/${total})`;
}

// Complete test execution
function completeTestExecution() {
    isExecuting = false;
    updateExecutionUI(false);

    document.getElementById('statusBadge').textContent =
        currentExecutionData.failed === 0 ? 'Success' : 'Completed';
    document.getElementById('statusBadge').className =
        currentExecutionData.failed === 0 ? 'badge badge-success' : 'badge badge-warning';

    // Show completion modal
    showExecutionCompleteModal();

    // Reload test suites and executions
    setTimeout(() => {
        loadTestSuites();
        loadRecentExecutions();
    }, 1000);
}

// Stop test execution
function stopTestExecution() {
    if (!isExecuting) return;

    if (confirm('Are you sure you want to stop the test execution?')) {
        isExecuting = false;
        clearInterval(executionInterval);
        updateExecutionUI(false);

        document.getElementById('statusBadge').textContent = 'Stopped';
        document.getElementById('statusBadge').className = 'badge badge-danger';

        showNotification('Test execution stopped', 'warning');
    }
}

// Update execution UI state
function updateExecutionUI(executing) {
    const runBtn = document.getElementById('runTestsBtn');
    const stopBtn = document.getElementById('stopTestsBtn');

    if (executing) {
        runBtn.style.display = 'none';
        stopBtn.style.display = 'inline-flex';
        document.getElementById('testExecutionForm').querySelectorAll('input, select').forEach(el => {
            el.disabled = true;
        });
    } else {
        runBtn.style.display = 'inline-flex';
        stopBtn.style.display = 'none';
        document.getElementById('testExecutionForm').querySelectorAll('input, select').forEach(el => {
            el.disabled = false;
        });

        // Reset progress display
        setTimeout(() => {
            document.getElementById('executionStatus').style.display = 'block';
            document.getElementById('executionProgress').style.display = 'none';
        }, 2000);
    }
}

// Show execution complete modal
function showExecutionCompleteModal() {
    const modal = document.getElementById('executionCompleteModal');
    const icon = document.getElementById('executionResultIcon');
    const title = document.getElementById('executionResultTitle');
    const summary = document.getElementById('executionResultSummary');

    const failed = currentExecutionData.failed;
    const passed = currentExecutionData.passed;
    const total = currentExecutionData.total;

    if (failed === 0) {
        icon.textContent = '✓';
        icon.style.color = 'var(--success-color)';
        title.textContent = 'All Tests Passed!';
        summary.textContent = `Successfully executed ${total} tests`;
    } else {
        icon.textContent = '⚠';
        icon.style.color = 'var(--warning-color)';
        title.textContent = 'Tests Completed with Failures';
        summary.textContent = `${failed} out of ${total} tests failed`;
    }

    document.getElementById('finalPassed').textContent = passed;
    document.getElementById('finalFailed').textContent = failed;
    document.getElementById('finalDuration').textContent = currentExecutionData.duration.toFixed(1) + 's';

    modal.classList.add('show');
}

// Close execution modal
function closeExecutionModal() {
    const modal = document.getElementById('executionCompleteModal');
    modal.classList.remove('show');
}

// Load recent executions
async function loadRecentExecutions() {
    try {
        const executions = await fetchRecentExecutions();
        renderRecentExecutions(executions);
    } catch (error) {
        console.error('Error loading recent executions:', error);
    }
}

// Fetch recent executions
async function fetchRecentExecutions() {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                {
                    id: 'EX-2025-001',
                    suite: 'API Test Suite',
                    status: 'passed',
                    passed: 43,
                    failed: 2,
                    total: 45,
                    duration: '2m 34s',
                    date: '2 hours ago'
                },
                {
                    id: 'EX-2025-002',
                    suite: 'UI Test Suite',
                    status: 'failed',
                    passed: 28,
                    failed: 4,
                    total: 32,
                    duration: '5m 12s',
                    date: '3 hours ago'
                },
                {
                    id: 'EX-2025-003',
                    suite: 'Integration Tests',
                    status: 'passed',
                    passed: 28,
                    failed: 0,
                    total: 28,
                    duration: '3m 45s',
                    date: '5 hours ago'
                }
            ]);
        }, 600);
    });
}

// Render recent executions
function renderRecentExecutions(executions) {
    const container = document.getElementById('recentExecutions');
    if (!container) return;

    if (executions.length === 0) {
        container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">No recent executions found</div>';
        return;
    }

    container.innerHTML = executions.map((exec, index) => `
        <div class="card mb-2" style="animation: slideInFromLeft 0.5s ease ${index * 0.1}s both;">
            <div class="card-body" style="padding: 1rem;">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <div class="d-flex align-items-center gap-2">
                            <strong>${exec.id}</strong>
                            <span class="badge badge-${exec.status === 'passed' ? 'success' : 'danger'}">${exec.status}</span>
                        </div>
                        <div class="text-muted" style="font-size: 0.875rem;">${exec.suite}</div>
                    </div>
                    <div class="text-right">
                        <div><strong>${exec.passed}/${exec.total}</strong> passed</div>
                        <div class="text-muted" style="font-size: 0.875rem;">${exec.duration} • ${exec.date}</div>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

// Quick run suite
function quickRunSuite(suiteId) {
    const suiteSelect = document.getElementById('testSuite');
    const suiteMap = {
        'api-test-suite': 'api',
        'ui-test-suite': 'ui',
        'integration-tests': 'integration',
        'regression-suite': 'regression',
        'smoke-test-suite': 'smoke'
    };

    suiteSelect.value = suiteMap[suiteId] || 'all';

    // Scroll to execution form
    document.getElementById('testExecutionForm').scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });

    // Highlight the form
    const card = document.getElementById('testExecutionForm').closest('.card');
    card.style.boxShadow = '0 0 0 3px rgba(79, 70, 229, 0.3)';
    setTimeout(() => {
        card.style.boxShadow = '';
    }, 2000);
}

// Refresh test suites
function refreshTestSuites() {
    showNotification('Refreshing test suites...', 'info');
    loadTestSuites();
}

// Show notification
function showNotification(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.style.position = 'fixed';
    alert.style.top = '20px';
    alert.style.right = '20px';
    alert.style.zIndex = '9999';
    alert.style.minWidth = '300px';
    alert.innerHTML = `
        <span>${getAlertIcon(type)}</span>
        <span>${message}</span>
    `;

    document.body.appendChild(alert);

    setTimeout(() => {
        alert.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => alert.remove(), 300);
    }, 3000);
}

// Get alert icon
function getAlertIcon(type) {
    const icons = {
        success: '✓',
        danger: '✗',
        warning: '⚠',
        info: 'ℹ'
    };
    return icons[type] || icons.info;
}

// Handle view report button
document.addEventListener('DOMContentLoaded', function() {
    const viewReportBtn = document.getElementById('viewReportBtn');
    if (viewReportBtn) {
        viewReportBtn.addEventListener('click', function() {
            window.location.href = '/dashboard/reports';
        });
    }
});

// Add slide out animation
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOut {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(100px);
        }
    }
    .btn-sm {
        padding: 0.5rem 1rem;
        font-size: 0.75rem;
    }
`;
document.head.appendChild(style);

