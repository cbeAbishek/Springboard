// Global Configuration
const API_BASE_URL = '/automation-framework/api';
let currentSection = 'dashboard';

// Initialize Dashboard
document.addEventListener('DOMContentLoaded', function () {
    // Check for mobile devices or desktop mode on mobile
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

    // A simple heuristic to guess if it's a mobile device in desktop mode
    const isLikelyDesktopModeOnMobile = window.screen.width < 1024 && window.innerWidth > 900;

    if (isMobile || isLikelyDesktopModeOnMobile) {
        const appContainer = document.getElementById('app-container');
        const unsupportedMessage = document.getElementById('unsupported-device-message');

        if (appContainer) appContainer.style.display = 'none';
        if (unsupportedMessage) unsupportedMessage.style.display = 'flex';

    } else {
        // Initialize the app only for desktop users
        initializeNavigation();
        initializeMobileMenu();
        checkApiStatus();
        loadDashboardData();
        initializeFormHandlers();

        // Set default date values for analytics
        const now = new Date();
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        const trendFromDate = document.getElementById('trend-from-date');
        const trendToDate = document.getElementById('trend-to-date');

        if (trendFromDate) trendFromDate.value = formatDateForInput(weekAgo);
        if (trendToDate) trendToDate.value = formatDateForInput(now);
    }
});

// Mobile Menu Functionality
function initializeMobileMenu() {
    const mobileToggle = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');

    if (mobileToggle && sidebar) {
        mobileToggle.addEventListener('click', function () {
            sidebar.classList.toggle('open');
        });

        // Close sidebar when clicking outside on mobile
        document.addEventListener('click', function (e) {
            if (window.innerWidth <= 1024) {
                if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target)) {
                    sidebar.classList.remove('open');
                }
            }
        });

        // Close sidebar on window resize to desktop
        window.addEventListener('resize', function () {
            if (window.innerWidth > 1024) {
                sidebar.classList.remove('open');
            }
        });
    }
}

// Navigation Functions
function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const section = this.getAttribute('data-section');
            switchSection(section);
        });
    });
}

function switchSection(section) {
    // Update navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    document.querySelector(`[data-section="${section}"]`).classList.add('active');

    // Update content
    document.querySelectorAll('.content-section').forEach(sec => {
        sec.classList.remove('active');
    });
    document.getElementById(section).classList.add('active');

    // Update page title
    const titles = {
        'dashboard': 'Dashboard',
        'testcases': 'Test Cases',
        'execution': 'Test Execution',
        'schedules': 'Schedules',
        'reports': 'Reports',
        'analytics': 'Analytics'
    };
    document.getElementById('page-title').textContent = titles[section];

    currentSection = section;

    // Load section-specific data
    loadSectionData(section);
}

function loadSectionData(section) {
    switch (section) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'testcases':
            loadTestCases();
            break;
        case 'execution':
            loadExecutions();
            break;
        case 'schedules':
            loadSchedules();
            break;
        case 'reports':
            loadReports();
            break;
        case 'analytics':
            // Analytics are generated on demand
            break;
    }
}

// API Status Check
async function checkApiStatus() {
    try {
        const response = await fetch(`${API_BASE_URL}/testcases`);
        const statusIndicator = document.getElementById('api-status');
        const statusText = document.getElementById('status-text');

        if (response.ok) {
            statusIndicator.className = 'status-indicator online';
            statusText.textContent = 'API Online';
        } else {
            throw new Error('API not responding');
        }
    } catch (error) {
        const statusIndicator = document.getElementById('api-status');
        const statusText = document.getElementById('status-text');
        statusIndicator.className = 'status-indicator offline';
        statusText.textContent = 'API Offline';
    }
}

// Dashboard Functions
async function loadDashboardData() {
    try {
        showLoading();

        // Load statistics
        const [testCases, batches, schedules, executions] = await Promise.all([
            fetch(`${API_BASE_URL}/testcases`).then(r => r.json()),
            fetch(`${API_BASE_URL}/execution/batches/recent?limit=20`).then(r => r.json()),
            fetch(`${API_BASE_URL}/schedules/active`).then(r => r.json()),
            fetch(`${API_BASE_URL}/execution/batches`).then(r => r.json())
        ]);

        // Update statistics
        document.getElementById('total-testcases').textContent = testCases.length;
        document.getElementById('total-executions').textContent = executions.length;
        document.getElementById('active-schedules').textContent = schedules.length;

        // Calculate success rate from individual executions
        const passedExecutions = executions.filter(e => e.status === 'PASSED').length;
        const successRate = executions.length > 0 ? Math.round((passedExecutions / executions.length) * 100) : 0;
        document.getElementById('success-rate').textContent = `${successRate}%`;

        // Load recent batches (use the first 10 from our recent batches call)
        loadRecentBatches(batches.slice(0, 10));

    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showNotification('Error loading dashboard data', 'error');
    } finally {
        hideLoading();
    }
}

function loadRecentBatches(batches) {
    const tbody = document.getElementById('recent-batches-body');
    tbody.innerHTML = '';

    batches.forEach(batch => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${batch.batchId}</td>
            <td><span class="status-badge status-${batch.status.toLowerCase()}">${batch.status}</span></td>
            <td>${batch.environment}</td>
            <td>${formatDateTime(batch.startTime)}</td>
            <td>${calculateDuration(batch.startTime, batch.endTime)}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-primary" onclick="viewBatchDetails('${batch.batchId}')">
                        <i class="fas fa-eye"></i> View
                    </button>
                    <button class="btn btn-sm btn-secondary" onclick="generateAllReports('${batch.batchId}')">
                        <i class="fas fa-file-alt"></i> Report
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Test Cases Functions
async function loadTestCases() {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/testcases`);
        const testCases = await response.json();

        displayTestCases(testCases);

    } catch (error) {
        console.error('Error loading test cases:', error);
        showNotification('Error loading test cases', 'error');
    } finally {
        hideLoading();
    }
}

function displayTestCases(testCases) {
    const tbody = document.getElementById('testcases-body');
    tbody.innerHTML = '';

    testCases.forEach(testCase => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${testCase.id}</td>
            <td>${testCase.name}</td>
            <td><span class="status-badge">${testCase.testType}</span></td>
            <td><span class="priority-${testCase.priority.toLowerCase()}">${testCase.priority}</span></td>
            <td>${testCase.environment}</td>
            <td>${testCase.testSuite}</td>
            <td><span class="status-badge ${testCase.isActive ? 'status-active' : 'status-inactive'}">${testCase.isActive ? 'Active' : 'Inactive'}</span></td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-primary" onclick="editTestCase(${testCase.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-success" onclick="executeSingleTest(${testCase.id})">
                        <i class="fas fa-play"></i> Run
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteTestCase(${testCase.id})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Test Execution Functions
async function loadExecutions() {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/execution/batches`);
        const executions = await response.json();

        displayExecutions(executions);

    } catch (error) {
        console.error('Error loading executions:', error);
        showNotification('Error loading executions', 'error');
    } finally {
        hideLoading();
    }
}

function displayExecutions(executions) {
    const tbody = document.getElementById('executions-body');
    tbody.innerHTML = '';

    executions.forEach(execution => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${execution.executionId}</td>
            <td>${execution.testCase ? execution.testCase.name : 'N/A'}</td>
            <td><span class="status-badge status-${execution.status.toLowerCase()}">${execution.status}</span></td>
            <td>${execution.environment}</td>
            <td>${formatDateTime(execution.startTime)}</td>
            <td>${execution.executionDuration || 0}ms</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-primary" onclick="viewExecutionDetails('${execution.executionId}')">
                        <i class="fas fa-eye"></i> View
                    </button>
                    ${execution.screenshotPath ? `<button class="btn btn-sm btn-secondary" onclick="viewScreenshot('${execution.screenshotPath}')"><i class="fas fa-image"></i> Screenshot</button>` : ''}
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Schedule Functions
async function loadSchedules() {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/schedules`);
        const schedules = await response.json();

        displaySchedules(schedules);

    } catch (error) {
        console.error('Error loading schedules:', error);
        showNotification('Error loading schedules', 'error');
    } finally {
        hideLoading();
    }
}

function displaySchedules(schedules) {
    const tbody = document.getElementById('schedules-body');
    tbody.innerHTML = '';

    schedules.forEach(schedule => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${schedule.id}</td>
            <td>${schedule.scheduleName}</td>
            <td><code>${schedule.cronExpression}</code></td>
            <td>${schedule.testSuite}</td>
            <td>${schedule.environment}</td>
            <td><span class="status-badge ${schedule.isActive ? 'status-active' : 'status-inactive'}">${schedule.isActive ? 'Active' : 'Inactive'}</span></td>
            <td>${schedule.nextExecution ? formatDateTime(schedule.nextExecution) : 'N/A'}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-primary" onclick="editSchedule(${schedule.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm ${schedule.isActive ? 'btn-warning' : 'btn-success'}" onclick="toggleSchedule(${schedule.id}, ${schedule.isActive})">
                        <i class="fas fa-${schedule.isActive ? 'pause' : 'play'}"></i> ${schedule.isActive ? 'Pause' : 'Activate'}
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteSchedule(${schedule.id})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Report Functions
async function loadReports() {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/reports/list`);
        if (!response.ok) {
            throw new Error('Failed to fetch reports');
        }
        const reports = await response.json();
        displayReports(reports);
    } catch (error) {
        console.error('Error loading reports:', error);
        showNotification('Error loading reports', 'error');
    } finally {
        hideLoading();
    }
}

function displayReports(reports) {
    const tbody = document.getElementById('reports-body');
    tbody.innerHTML = '';

    if (reports.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="6" class="text-center text-gray-500">No reports available</td>
        `;
        tbody.appendChild(row);
        return;
    }

    reports.forEach(report => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${report.filename}</td>
            <td>${report.batchId || 'N/A'}</td>
            <td><span class="status-badge">${report.type}</span></td>
            <td>${report.formattedSize}</td>
            <td>${report.formattedCreatedAt}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-primary" onclick="viewReport('${report.filename}')">
                        <i class="fas fa-eye"></i> View
                    </button>
                    <button class="btn btn-sm btn-success" onclick="downloadReport('${report.filename}')">
                        <i class="fas fa-download"></i> Download
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteReport('${report.filename}')">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function viewReport(filename) {
    const viewUrl = `${window.location.origin}/automation-framework/api/reports/view/${filename}`;
    window.open(viewUrl, '_blank');
}

function downloadReport(filename) {
    const downloadUrl = `${API_BASE_URL}/reports/download/${filename}`;
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showNotification(`Downloading ${filename}...`, 'success');
}

async function deleteReport(filename) {
    if (confirm(`Are you sure you want to delete the report "${filename}"?`)) {
        try {
            showLoading();
            const response = await fetch(`${API_BASE_URL}/reports/delete/${filename}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                showNotification(`Report "${filename}" deleted successfully!`, 'success');
                loadReports(); // Refresh the reports list
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'Failed to delete report');
            }
        } catch (error) {
            console.error('Error deleting report:', error);
            showNotification(`Error: ${error.message}`, 'error');
        } finally {
            hideLoading();
        }
    }
}

// Form Handlers
function initializeFormHandlers() {
    // Batch execution form
    document.getElementById('batch-execution-form').addEventListener('submit', handleBatchExecution);

    // Single test execution form
    document.getElementById('single-execution-form').addEventListener('submit', handleSingleExecution);

    // Test case creation form
    document.getElementById('create-testcase-form').addEventListener('submit', handleCreateTestCase);

    // Schedule creation form
    document.getElementById('create-schedule-form').addEventListener('submit', handleCreateSchedule);

    // Analytics forms
    document.getElementById('trend-analysis-form').addEventListener('submit', handleTrendAnalysis);
    document.getElementById('regression-metrics-form').addEventListener('submit', handleRegressionMetrics);

    // Search and filter handlers
    document.getElementById('testcase-search').addEventListener('input', filterTestCases);
    document.getElementById('testcase-filter-type').addEventListener('change', filterTestCases);
    document.getElementById('testcase-filter-env').addEventListener('change', filterTestCases);
}

async function handleBatchExecution(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        testSuite: formData.get('testSuite'),
        environment: formData.get('environment'),
        parallelThreads: parseInt(formData.get('parallelThreads'))
    };

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/execution/batch`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (response.ok) {
            showNotification('Batch execution started successfully!', 'success');
            e.target.reset();
            loadExecutions();
        } else {
            throw new Error(result.message || 'Failed to start batch execution');
        }

    } catch (error) {
        console.error('Error executing batch:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function handleSingleExecution(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const testCaseId = formData.get('testCaseId');
    const environment = formData.get('environment');

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/execution/single/${testCaseId}?environment=${environment}`, {
            method: 'POST'
        });

        if (response.ok) {
            const result = await response.json();
            showNotification('Test execution started successfully!', 'success');
            e.target.reset();
            loadExecutions();
        } else {
            throw new Error('Failed to start test execution');
        }

    } catch (error) {
        console.error('Error executing test:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function handleCreateTestCase(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        name: formData.get('name'),
        description: formData.get('description'),
        testType: formData.get('testType'),
        priority: formData.get('priority'),
        testSuite: formData.get('testSuite'),
        environment: formData.get('environment'),
        testData: formData.get('testData'),
        expectedResult: formData.get('expectedResult')
    };

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/testcases`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const result = await response.json();
            showNotification('Test case created successfully!', 'success');
            closeModal('testcase-modal');
            e.target.reset();
            if (currentSection === 'testcases') {
                loadTestCases();
            }
        } else {
            throw new Error('Failed to create test case');
        }

    } catch (error) {
        console.error('Error creating test case:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function handleCreateSchedule(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        scheduleName: formData.get('scheduleName'),
        cronExpression: formData.get('cronExpression'),
        testSuite: formData.get('testSuite'),
        environment: formData.get('environment'),
        parallelThreads: parseInt(formData.get('parallelThreads'))
    };

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/schedules`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const result = await response.json();
            showNotification('Schedule created successfully!', 'success');
            closeModal('schedule-modal');
            e.target.reset();
            if (currentSection === 'schedules') {
                loadSchedules();
            }
        } else {
            throw new Error('Failed to create schedule');
        }

    } catch (error) {
        console.error('Error creating schedule:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

// Analytics Functions
async function handleTrendAnalysis(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const fromDate = formData.get('fromDate');
    const toDate = formData.get('toDate');

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/analytics/trends?fromDate=${fromDate}&toDate=${toDate}`);

        if (response.ok) {
            const data = await response.json();
            displayTrendAnalysis(data);
        } else {
            throw new Error('Failed to get trend analysis');
        }

    } catch (error) {
        console.error('Error getting trend analysis:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function handleRegressionMetrics(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const environment = formData.get('environment');
    const days = formData.get('days');

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/analytics/regression/${environment}?days=${days}`);

        if (response.ok) {
            const data = await response.json();
            displayRegressionMetrics(data);
        } else {
            throw new Error('Failed to get regression metrics');
        }

    } catch (error) {
        console.error('Error getting regression metrics:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

function displayTrendAnalysis(data) {
    const container = document.getElementById('trend-results');
    container.innerHTML = `
        <h4>Trend Analysis Results</h4>
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.totalExecutions}</h3>
                    <p>Total Executions</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.passRate ? data.passRate.toFixed(1) : 0}%</h3>
                    <p>Pass Rate</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.averageExecutionTime ? data.averageExecutionTime.toFixed(0) : 0}ms</h3>
                    <p>Avg Execution Time</p>
                </div>
            </div>
        </div>
        <div class="mt-20">
            <h5>Environment Statistics</h5>
            <div id="env-stats">
                ${Object.entries(data.environmentStats || {}).map(([env, count]) =>
        `<p><strong>${env}:</strong> ${count} executions</p>`
    ).join('')}
            </div>
        </div>
    `;
}

function displayRegressionMetrics(data) {
    const container = document.getElementById('regression-results');
    container.innerHTML = `
        <h4>Regression Metrics - ${data.environment}</h4>
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.stabilityScore ? data.stabilityScore.toFixed(1) : 0}%</h3>
                    <p>Stability Score</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.regressionDetectionRate ? data.regressionDetectionRate.toFixed(1) : 0}%</h3>
                    <p>Regression Detection Rate</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-content">
                    <h3>${data.totalExecutions}</h3>
                    <p>Total Executions (${data.days} days)</p>
                </div>
            </div>
        </div>
    `;
}

// Report Functions
async function generateAllReports(batchId) {
    if (!batchId) {
        batchId = document.getElementById('report-batch-id').value;
    }

    if (!batchId) {
        showNotification('Please enter a batch ID', 'warning');
        return;
    }

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/reports/generate/${batchId}`, {
            method: 'POST'
        });

        if (response.ok) {
            const message = await response.text();
            showNotification(message, 'success');
        } else {
            throw new Error('Failed to generate reports');
        }

    } catch (error) {
        console.error('Error generating reports:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function generateHtmlReport() {
    const batchId = document.getElementById('report-batch-id').value;

    if (!batchId) {
        showNotification('Please enter a batch ID', 'warning');
        return;
    }

    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/reports/html/${batchId}`, {
            method: 'POST'
        });

        if (response.ok) {
            const result = await response.json();
            showNotification('HTML report generated successfully!', 'success');
            // You could add logic here to display the report path or open it
        } else {
            throw new Error('Failed to generate HTML report');
        }

    } catch (error) {
        console.error('Error generating HTML report:', error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

// Modal Functions
function showCreateTestCaseModal() {
    document.getElementById('testcase-modal').style.display = 'block';
}

function showCreateScheduleModal() {
    document.getElementById('schedule-modal').style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// Utility Functions
function showLoading() {
    document.getElementById('loading-overlay').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loading-overlay').style.display = 'none';
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
            <span>${message}</span>
            <button class="notification-close" onclick="this.parentElement.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;

    // Add notification styles if not already added
    if (!document.querySelector('#notification-styles')) {
        const styles = document.createElement('style');
        styles.id = 'notification-styles';
        styles.textContent = `
            .notification {
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 4000;
                min-width: 300px;
                padding: 15px;
                border-radius: 8px;
                box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
                animation: slideInRight 0.3s ease;
            }
            .notification-success { background: #d4edda; border-left: 4px solid #28a745; color: #155724; }
            .notification-error { background: #f8d7da; border-left: 4px solid #dc3545; color: #721c24; }
            .notification-warning { background: #fff3cd; border-left: 4px solid #ffc107; color: #856404; }
            .notification-info { background: #d1ecf1; border-left: 4px solid #17a2b8; color: #0c5460; }
            .notification-content { display: flex; align-items: center; gap: 10px; }
            .notification-close { background: none; border: none; color: inherit; cursor: pointer; margin-left: auto; }
            @keyframes slideInRight { from { transform: translateX(100%); } to { transform: translateX(0); } }
        `;
        document.head.appendChild(styles);
    }

    // Add to page
    document.body.appendChild(notification);

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentElement) {
            notification.remove();
        }
    }, 5000);
}

function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
}

function formatDateForInput(date) {
    return date.toISOString().slice(0, 16);
}

function calculateDuration(startTime, endTime) {
    if (!startTime || !endTime) return 'N/A';
    const start = new Date(startTime);
    const end = new Date(endTime);
    const duration = end - start;

    const hours = Math.floor(duration / (1000 * 60 * 60));
    const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((duration % (1000 * 60)) / 1000);

    return `${hours}h ${minutes}m ${seconds}s`;
}

function filterTestCases() {
    const search = document.getElementById('testcase-search').value.toLowerCase();
    const typeFilter = document.getElementById('testcase-filter-type').value;
    const envFilter = document.getElementById('testcase-filter-env').value;

    const rows = document.querySelectorAll('#testcases-body tr');

    rows.forEach(row => {
        const cells = row.children;
        const name = cells[1].textContent.toLowerCase();
        const type = cells[2].textContent;
        const environment = cells[4].textContent;

        const matchesSearch = name.includes(search);
        const matchesType = !typeFilter || type.includes(typeFilter);
        const matchesEnv = !envFilter || environment.includes(envFilter);

        row.style.display = matchesSearch && matchesType && matchesEnv ? '' : 'none';
    });
}

// Additional Action Functions
async function editTestCase(id) {
    // This is a placeholder for a proper modal-based editor.
    // For now, we'll just toggle the active status as an example of an update.
    try {
        showLoading();
        // First, get the current test case data
        const response = await fetch(`${API_BASE_URL}/testcases/${id}`);
        if (!response.ok) throw new Error('Failed to fetch test case for editing.');

        const testCase = await response.json();

        // In a real app, you would populate a form with this data.
        // Here, we'll just create a mock updated object.
        const updatedTestCase = { ...testCase, name: `${testCase.name} (edited)` }; // Example change

        const updateResponse = await fetch(`${API_BASE_URL}/testcases/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedTestCase)
        });

        if (updateResponse.ok) {
            showNotification(`Test case ${id} updated successfully!`, 'success');
            loadTestCases();
        } else {
            const error = await updateResponse.json();
            throw new Error(error.message || 'Failed to update test case.');
        }
    } catch (error) {
        console.error(`Error editing test case ${id}:`, error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

async function deleteTestCase(id) {
    if (confirm('Are you sure you want to delete this test case?')) {
        try {
            showLoading();
            const response = await fetch(`${API_BASE_URL}/testcases/${id}`, {
                method: 'DELETE'
            });

            if (response.status === 204) {
                showNotification(`Test case ${id} deleted successfully.`, 'success');
                loadTestCases();
            } else {
                const error = await response.json();
                throw new Error(error.message || 'Failed to delete test case.');
            }
        } catch (error) {
            console.error(`Error deleting test case ${id}:`, error);
            showNotification(`Error: ${error.message}`, 'error');
        } finally {
            hideLoading();
        }
    }
}

function executeSingleTest(id) {
    document.getElementById('single-test-id').value = id;
    switchSection('execution');
}

async function viewBatchDetails(batchId) {
    try {
        showLoading();
        const response = await fetch(`${API_BASE_URL}/execution/batch/${batchId}`);
        if (!response.ok) throw new Error('Failed to fetch batch details.');

        const batch = await response.json();

        // For now, display in a simple alert or modal. A dedicated view would be better.
        const details = `
            Batch ID: ${batch.batchId}
            Status: ${batch.status}
            Environment: ${batch.environment}
            Start Time: ${formatDateTime(batch.startTime)}
            End Time: ${formatDateTime(batch.endTime)}
            Duration: ${calculateDuration(batch.startTime, batch.endTime)}
        `;
        alert(details); // Replace with a proper modal later

    } catch (error) {
        console.error(`Error viewing batch details for ${batchId}:`, error);
        showNotification(`Error: ${error.message}`, 'error');
    } finally {
        hideLoading();
    }
}

function viewExecutionDetails(executionId) {
    showNotification(`View execution ${executionId} details - No specific endpoint. Details are in batch view.`, 'info');
}

function viewScreenshot(path) {
    // Assuming screenshots are served from a known path
    window.open(`/screenshots/${path}`, '_blank');
}

async function editSchedule(id) {
    // Placeholder for editing a schedule. Similar to editTestCase, this would open a modal.
    showNotification(`Edit schedule ${id} - Feature coming soon!`, 'info');
}

async function toggleSchedule(id, isActive) {
    const action = isActive ? 'deactivate' : 'activate';
    if (confirm(`Are you sure you want to ${action} this schedule?`)) {
        try {
            showLoading();
            const response = await fetch(`${API_BASE_URL}/schedules/${id}/${action}`, {
                method: 'POST'
            });

            if (response.ok) {
                showNotification(`Schedule ${id} ${action}d successfully!`, 'success');
                loadSchedules();
            } else {
                const error = await response.json();
                throw new Error(error.message || `Failed to ${action} schedule.`);
            }
        } catch (error) {
            console.error(`Error toggling schedule ${id}:`, error);
            showNotification(`Error: ${error.message}`, 'error');
        } finally {
            hideLoading();
        }
    }
}

async function deleteSchedule(id) {
    if (confirm('Are you sure you want to delete this schedule?')) {
        try {
            showLoading();
            const response = await fetch(`${API_BASE_URL}/schedules/${id}`, {
                method: 'DELETE'
            });

            if (response.status === 204) {
                showNotification(`Schedule ${id} deleted successfully.`, 'success');
                loadSchedules();
            } else {
                const error = await response.json();
                throw new Error(error.message || 'Failed to delete schedule.');
            }
        } catch (error) {
            console.error(`Error deleting schedule ${id}:`, error);
            showNotification(`Error: ${error.message}`, 'error');
        } finally {
            hideLoading();
        }
    }
}

// Close modals when clicking outside
window.onclick = function (event) {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}
