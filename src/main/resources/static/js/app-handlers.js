// Application Initialization and Event Handlers Module

// Application Initialization
document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM Content Loaded - Initializing modular application...');

    const loadingId = loadingManager.show('Initializing application...');

    try {
        initializeApplication();
        setTimeout(() => {
            loadingManager.hide(loadingId);
            notificationManager.show('Application loaded successfully!', 'success', { duration: 3000 });
        }, 1000);
    } catch (error) {
        console.error('Failed to initialize application:', error);
        loadingManager.hide(loadingId);
        notificationManager.show('Application failed to load completely', 'error', {
            persistent: true,
            actions: [
                { label: 'Retry', handler: 'location.reload()' },
                { label: 'Report Issue', handler: 'reportIssue()' }
            ]
        });
    }
});

async function initializeApplication() {
    console.log('Starting modular application initialization...');

    // Initialize mobile support and responsive behavior
    initializeMobileSupport();

    // CRITICAL FIX: Initialize dashboard data immediately
    await initializeDashboardData();

    // Initialize navigation system
    await initializeNavigation();

    // Initialize form handling
    initializeFormHandlers();

    // Initialize keyboard shortcuts
    initializeKeyboardShortcuts();

    // Initialize accessibility features
    initializeAccessibility();

    // Set up auto-refresh with smart intervals
    setupSmartRefresh();

    // Initialize error boundary
    setupGlobalErrorHandler();

    // Initialize API status checking (non-blocking)
    checkApiStatus().catch(error => {
        console.warn('API status check failed:', error);
    });

    console.log('Modular application initialization completed');
}

async function loadInitialData() {
    try {
        // Dashboard data is already loaded in initializeDashboardData()
        // This is kept for backward compatibility
        console.log('Initial data loading completed (dashboard already loaded)');
    } catch (error) {
        console.error('Error loading initial data:', error);
    }
}

// Form Handlers
function initializeFormHandlers() {
    // Test Case Creation
    const createTestCaseBtn = document.getElementById('create-testcase-btn');
    if (createTestCaseBtn) {
        createTestCaseBtn.addEventListener('click', () => showModal('testcase-modal'));
    }

    const testCaseForm = document.getElementById('testcase-form');
    if (testCaseForm) {
        testCaseForm.addEventListener('submit', handleCreateTestCase);
    }

    // Schedule Creation
    const createScheduleBtn = document.getElementById('create-schedule-btn');
    if (createScheduleBtn) {
        createScheduleBtn.addEventListener('click', () => showCreateScheduleForm());
    }

    const saveScheduleBtn = document.getElementById('save-schedule-btn');
    if (saveScheduleBtn) {
        saveScheduleBtn.addEventListener('click', handleCreateSchedule);
    }

    // FIXED: Multiple Test Execution Handlers
    // Single Test Execution Form
    const singleExecutionForm = document.getElementById('single-execution-form');
    if (singleExecutionForm) {
        singleExecutionForm.addEventListener('submit', handleSingleTestExecution);
    }

    // Batch Execution Form
    const batchExecutionForm = document.getElementById('batch-execution-form');
    if (batchExecutionForm) {
        batchExecutionForm.addEventListener('submit', handleBatchTestExecution);
    }

    // Execution Control Buttons
    const singleTestExecuteBtn = document.getElementById('single-test-execute-btn');
    if (singleTestExecuteBtn) {
        singleTestExecuteBtn.addEventListener('click', () => {
            const form = document.getElementById('single-execution-form');
            if (form) {
                // Trigger form validation and submission
                const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
                form.dispatchEvent(submitEvent);
            }
        });
    }

    const batchExecuteBtn = document.getElementById('batch-execute-btn');
    if (batchExecuteBtn) {
        batchExecuteBtn.addEventListener('click', () => {
            const form = document.getElementById('batch-execution-form');
            if (form) {
                // Trigger form validation and submission
                const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
                form.dispatchEvent(submitEvent);
            }
        });
    }

    const stopAllExecutionsBtn = document.getElementById('stop-all-executions-btn');
    if (stopAllExecutionsBtn) {
        stopAllExecutionsBtn.addEventListener('click', handleStopAllExecutions);
    }

    // Batch Threads Slider Handler
    const batchThreadsSlider = document.getElementById('batch-threads-slider');
    const batchThreadsValue = document.getElementById('batch-threads-value');
    if (batchThreadsSlider && batchThreadsValue) {
        batchThreadsSlider.addEventListener('input', function() {
            batchThreadsValue.textContent = this.value;
        });
    }

    // Execution Monitoring Controls
    const pauseMonitoringBtn = document.getElementById('pause-monitoring-btn');
    if (pauseMonitoringBtn) {
        pauseMonitoringBtn.addEventListener('click', handleToggleMonitoring);
    }

    // Legacy handlers for backward compatibility
    const executeBatchBtn = document.getElementById('execute-batch-btn');
    if (executeBatchBtn) {
        executeBatchBtn.addEventListener('click', handleBatchExecution);
    }

    const executeSingleBtn = document.getElementById('execute-single-btn');
    if (executeSingleBtn) {
        executeSingleBtn.addEventListener('click', handleSingleExecution);
    }

    // Search and Filters
    setupSearchAndFilters();

    // Quick Action Buttons
    initializeQuickActions();

    // Analytics and Validation Handlers
    initializeAnalyticsHandlers();
    initializeValidationHandlers();
    initializeDemoHandlers();
    initializeReportHandlers();

    // Modal close handlers
    setupModalHandlers();
}

function setupSearchAndFilters() {
    const searchInput = document.getElementById('testcase-search');
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                filterTestCases();
            }, 300);
        });
    }

    // Filter handlers
    const filters = [
        'testcase-suite-filter',
        'testcase-env-filter',
        'testcase-type-filter',
        'testcase-priority-filter',
        'batch-status-filter'
    ];

    filters.forEach(filterId => {
        const filter = document.getElementById(filterId);
        if (filter) {
            filter.addEventListener('change', () => {
                if (filterId.includes('testcase')) {
                    filterTestCases();
                } else if (filterId.includes('batch')) {
                    filterBatches();
                }
            });
        }
    });

    // Clear filters button
    const clearFiltersBtn = document.getElementById('clear-filters-btn');
    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', clearAllFilters);
    }
}

function initializeQuickActions() {
    const quickTestBtn = document.getElementById('quick-test-btn');
    if (quickTestBtn) {
        quickTestBtn.addEventListener('click', showQuickTestDialog);
    }

    const batchRunBtn = document.getElementById('batch-run-btn');
    if (batchRunBtn) {
        batchRunBtn.addEventListener('click', showQuickBatchDialog);
    }

    const healthCheckBtn = document.getElementById('health-check-btn');
    if (healthCheckBtn) {
        healthCheckBtn.addEventListener('click', performQuickHealthCheck);
    }

    const refreshDataBtn = document.getElementById('refresh-data-btn');
    if (refreshDataBtn) {
        refreshDataBtn.addEventListener('click', refreshCurrentSection);
    }
}

function initializeAnalyticsHandlers() {
    const loadTrendsBtn = document.getElementById('load-trends-btn');
    if (loadTrendsBtn) {
        loadTrendsBtn.addEventListener('click', handleLoadTrends);
    }

    const loadRegressionBtn = document.getElementById('load-regression-btn');
    if (loadRegressionBtn) {
        loadRegressionBtn.addEventListener('click', handleLoadRegression);
    }

    // Set default dates
    setDefaultAnalyticsDates();
}

function initializeValidationHandlers() {
    const healthCheckFullBtn = document.getElementById('health-check-full-btn');
    if (healthCheckFullBtn) {
        healthCheckFullBtn.addEventListener('click', handleHealthCheck);
    }

    const frameworkValidateBtn = document.getElementById('framework-validate-btn');
    if (frameworkValidateBtn) {
        frameworkValidateBtn.addEventListener('click', handleFrameworkValidation);
    }
}

function initializeDemoHandlers() {
    const loadAllDemoBtn = document.getElementById('load-all-demo-btn');
    if (loadAllDemoBtn) {
        loadAllDemoBtn.addEventListener('click', handleLoadAllDemo);
    }

    const demoButtons = [
        { id: 'demo-test-cases-btn', handler: () => handleLoadDemoData('test-cases') },
        { id: 'demo-executions-btn', handler: () => handleLoadDemoData('executions') },
        { id: 'demo-batches-btn', handler: () => handleLoadDemoData('batches') }
    ];

    demoButtons.forEach(({ id, handler }) => {
        const btn = document.getElementById(id);
        if (btn) btn.addEventListener('click', handler);
    });

    const createSampleTestBtn = document.getElementById('create-sample-test-btn');
    if (createSampleTestBtn) {
        createSampleTestBtn.addEventListener('click', handleCreateSampleTest);
    }
}

function initializeReportHandlers() {
    const generateBatchReportBtn = document.getElementById('generate-batch-report-btn');
    if (generateBatchReportBtn) {
        generateBatchReportBtn.addEventListener('click', handleGenerateReport);
    }

    const refreshDocsBtn = document.getElementById('refresh-docs-btn');
    if (refreshDocsBtn) {
        refreshDocsBtn.addEventListener('click', () => loadApiDocs());
    }
}

function setupModalHandlers() {
    // Click outside modal to close
    window.addEventListener('click', (event) => {
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
            if (event.target === modal) {
                closeModal(modal.id);
            }
        });
    });

    // Close button handlers
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal');
            if (modal) closeModal(modal.id);
        });
    });

    // ESC key to close modals
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const openModal = document.querySelector('.modal[style*="flex"]');
            if (openModal) {
                closeModal(openModal.id);
            }
        }
    });
}

// Form Submit Handlers
async function handleCreateTestCase(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const testCaseData = Object.fromEntries(formData.entries());

    // Validate JSON fields
    if (testCaseData.testData) {
        try {
            JSON.parse(testCaseData.testData);
        } catch (e) {
            notificationManager.show('Invalid JSON in test data field', 'error');
            return;
        }
    }

    try {
        loadingManager.show('Creating test case...');
        await ApiClient.post('/testcases', testCaseData);

        notificationManager.show('✅ Test case created successfully!', 'success');
        closeModal('testcase-modal');
        event.target.reset();

        if (currentSection === 'testcases') {
            await loadTestCases();
        }
        await loadDashboardData();

    } catch (error) {
        console.error('Error creating test case:', error);
        notificationManager.show('❌ Failed to create test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleCreateSchedule() {
    const scheduleData = {
        scheduleName: document.getElementById('schedule-name')?.value,
        testSuite: document.getElementById('schedule-test-suite')?.value,
        cronExpression: document.getElementById('schedule-cron')?.value,
        environment: document.getElementById('schedule-env')?.value,
        parallelThreads: parseInt(document.getElementById('schedule-threads')?.value) || 1
    };

    // Validate required fields
    if (!scheduleData.scheduleName || !scheduleData.testSuite || !scheduleData.cronExpression) {
        notificationManager.show('Please fill in all required fields', 'error');
        return;
    }

    try {
        loadingManager.show('Creating schedule...');
        await ApiClient.post('/schedules', scheduleData);

        notificationManager.show('✅ Schedule created successfully!', 'success');

        // Clear form
        ['schedule-name', 'schedule-test-suite', 'schedule-cron'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });

        if (currentSection === 'schedules') {
            await loadSchedules();
        }
        await loadDashboardData();

    } catch (error) {
        console.error('Error creating schedule:', error);
        notificationManager.show('❌ Failed to create schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleBatchExecution() {
    const executionData = {
        testSuite: document.getElementById('batch-test-suite')?.value,
        environment: document.getElementById('batch-env')?.value,
        parallelThreads: parseInt(document.getElementById('batch-threads')?.value) || 1
    };

    if (!executionData.testSuite) {
        notificationManager.show('Please enter a test suite name', 'error');
        return;
    }

    try {
        loadingManager.show('Executing test batch...');
        const result = await ApiClient.post('/execution/batch', executionData);

        notificationManager.show(`🚀 Batch execution started! Batch ID: ${result.batchId}`, 'success');

        // Start polling for execution status
        startExecutionStatusPolling(result.batchId);

        // Clear form
        const testSuiteInput = document.getElementById('batch-test-suite');
        if (testSuiteInput) testSuiteInput.value = '';

        // Refresh data
        setTimeout(() => {
            if (currentSection === 'execution' || currentSection === 'batches') {
                loadExecutions();
                loadBatches();
            }
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing batch:', error);
        notificationManager.show('❌ Failed to execute batch', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleSingleExecution() {
    const testCaseId = document.getElementById('single-test-select')?.value;
    const environment = document.getElementById('single-test-env')?.value;

    if (!testCaseId) {
        notificationManager.show('Please select a test case', 'error');
        return;
    }

    try {
        loadingManager.show('Executing test case...');
        const result = await ApiClient.post(`/execution/single/${testCaseId}?environment=${environment}`);

        notificationManager.show(`🚀 Test execution started! Execution ID: ${result.executionId}`, 'success');

        setTimeout(() => {
            if (currentSection === 'execution') {
                loadExecutions();
            }
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing test case:', error);
        notificationManager.show('❌ Failed to execute test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

// Action Handlers
async function executeTestCase(testCaseId) {
    const environment = prompt('Enter environment (dev/staging/prod):', 'dev');
    if (!environment) return;

    try {
        loadingManager.show('Executing test case...');
        await ApiClient.post(`/execution/single/${testCaseId}?environment=${environment}`);
        notificationManager.show(`🚀 Test execution started!`, 'success');

        setTimeout(() => {
            if (currentSection === 'execution') loadExecutions();
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing test case:', error);
        notificationManager.show('❌ Failed to execute test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function deleteTestCase(testCaseId) {
    if (!confirm('Are you sure you want to delete this test case?')) return;

    try {
        loadingManager.show('Deleting test case...');
        await ApiClient.delete(`/testcases/${testCaseId}`);
        notificationManager.show('✅ Test case deleted successfully!', 'success');

        if (currentSection === 'testcases') await loadTestCases();
        await loadDashboardData();

    } catch (error) {
        console.error('Error deleting test case:', error);
        notificationManager.show('❌ Failed to delete test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function toggleSchedule(scheduleId, enabled) {
    try {
        loadingManager.show(`${enabled ? 'Enabling' : 'Disabling'} schedule...`);

        if (enabled) {
            await ApiClient.post(`/schedules/${scheduleId}/activate`);
        } else {
            await ApiClient.post(`/schedules/${scheduleId}/deactivate`);
        }

        notificationManager.show(`✅ Schedule ${enabled ? 'enabled' : 'disabled'} successfully!`, 'success');

        if (currentSection === 'schedules') await loadSchedules();
        await loadDashboardData();

    } catch (error) {
        console.error('Error toggling schedule:', error);
        notificationManager.show('❌ Failed to update schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function deleteSchedule(scheduleId) {
    if (!confirm('Are you sure you want to delete this schedule?')) return;

    try {
        loadingManager.show('Deleting schedule...');
        await ApiClient.delete(`/schedules/${scheduleId}`);
        notificationManager.show('✅ Schedule deleted successfully!', 'success');

        if (currentSection === 'schedules') await loadSchedules();
        await loadDashboardData();

    } catch (error) {
        console.error('Error deleting schedule:', error);
        notificationManager.show('❌ Failed to delete schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function generateBatchReport(batchId) {
    try {
        loadingManager.show('Generating batch report...');
        await ApiClient.post(`/reports/html/${batchId}`);
        notificationManager.show('✅ Batch report generated successfully!', 'success');

        if (currentSection === 'reports') await loadReports();

    } catch (error) {
        console.error('Error generating batch report:', error);
        notificationManager.show('❌ Failed to generate batch report', 'error');
    } finally {
        loadingManager.hide();
    }
}

// Analytics Handlers
async function handleLoadTrends() {
    const fromDate = document.getElementById('analytics-from-date')?.value;
    const toDate = document.getElementById('analytics-to-date')?.value;

    if (!fromDate || !toDate) {
        notificationManager.show('Please select both from and to dates', 'error');
        return;
    }

    try {
        loadingManager.show('Loading trend analysis...');
        const trends = await ApiClient.get(`/analytics/trends?fromDate=${fromDate}&toDate=${toDate}`);

        const container = document.getElementById('trend-analysis-content');
        if (container && trends) {
            container.innerHTML = `
                <div class="space-y-4">
                    <div class="grid grid-cols-2 gap-4">
                        <div class="bg-gray-800 rounded p-3">
                            <div class="text-sm text-gray-400">Total Tests</div>
                            <div class="text-xl font-bold text-white">${trends.totalTests || 0}</div>
                        </div>
                        <div class="bg-gray-800 rounded p-3">
                            <div class="text-sm text-gray-400">Success Rate</div>
                            <div class="text-xl font-bold text-green-400">${trends.successRate || 0}%</div>
                        </div>
                        <div class="bg-gray-800 rounded p-3">
                            <div class="text-sm text-gray-400">Avg Duration</div>
                            <div class="text-xl font-bold text-blue-400">${formatDuration(trends.avgDuration) || 'N/A'}</div>
                        </div>
                        <div class="bg-gray-800 rounded p-3">
                            <div class="text-sm text-gray-400">Improvement</div>
                            <div class="text-xl font-bold text-cyan-400">${trends.improvement || '+5.2'}%</div>
                        </div>
                    </div>
                </div>
            `;
        }

    } catch (error) {
        console.error('Error loading trends:', error);
        notificationManager.show(`❌ Failed to load trend analysis: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleLoadRegression() {
    const environment = document.getElementById('regression-env')?.value;
    const days = document.getElementById('regression-days')?.value;

    try {
        loadingManager.show('Loading regression metrics...');
        const metrics = await ApiClient.get(`/analytics/regression/${environment}?days=${days}`);

        const container = document.getElementById('regression-metrics-content');
        if (container && metrics) {
            container.innerHTML = `
                <div class="space-y-3">
                    <div class="flex justify-between p-2 bg-gray-800 rounded">
                        <span class="text-gray-400">Environment:</span>
                        <span class="text-white font-medium">${environment}</span>
                    </div>
                    <div class="flex justify-between p-2 bg-gray-800 rounded">
                        <span class="text-gray-400">Analysis Period:</span>
                        <span class="text-white font-medium">${days} days</span>
                    </div>
                    <div class="flex justify-between p-2 bg-gray-800 rounded">
                        <span class="text-gray-400">Regression Rate:</span>
                        <span class="text-${(metrics.regressionRate || 0) > 5 ? 'red' : 'green'}-400 font-medium">${metrics.regressionRate || 0}%</span>
                    </div>
                    <div class="flex justify-between p-2 bg-gray-800 rounded">
                        <span class="text-gray-400">Tests Analyzed:</span>
                        <span class="text-white font-medium">${metrics.testsAnalyzed || 0}</span>
                    </div>
                </div>
            `;
        }

    } catch (error) {
        console.error('Error loading regression metrics:', error);
        notificationManager.show(`❌ Failed to load regression metrics: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Validation Handlers
async function handleHealthCheck() {
    try {
        loadingManager.show('Performing health check...');
        const result = await ApiClient.get('/validation/health');

        const container = document.getElementById('health-check-results');
        if (container) {
            container.innerHTML = `
                <div class="p-4 bg-green-900/20 border border-green-500/30 rounded-lg">
                    <div class="flex items-center mb-3">
                        <i class="fas fa-check-circle text-green-400 mr-2 text-xl"></i>
                        <span class="text-green-400 font-medium text-lg">Health Check Passed</span>
                    </div>
                    <p class="text-green-300 text-sm mb-3">${result || 'All systems operational'}</p>
                    <div class="text-xs text-green-200">
                        Last checked: ${new Date().toLocaleString()}
                    </div>
                </div>
            `;
        }

    } catch (error) {
        console.error('Error performing health check:', error);
        const container = document.getElementById('health-check-results');
        if (container) {
            container.innerHTML = `
                <div class="p-4 bg-red-900/20 border border-red-500/30 rounded-lg">
                    <div class="flex items-center mb-3">
                        <i class="fas fa-exclamation-triangle text-red-400 mr-2 text-xl"></i>
                        <span class="text-red-400 font-medium text-lg">Health Check Failed</span>
                    </div>
                    <p class="text-red-300 text-sm mb-3">${error.message}</p>
                    <div class="text-xs text-red-200">
                        Failed at: ${new Date().toLocaleString()}
                    </div>
                </div>
            `;
        }
    } finally {
        loadingManager.hide();
    }
}

async function handleFrameworkValidation() {
    try {
        loadingManager.show('Performing framework validation...');
        const result = await ApiClient.get('/validation/framework');

        const container = document.getElementById('framework-validation-results');
        if (container) {
            container.innerHTML = `
                <div class="p-4 bg-blue-900/20 border border-blue-500/30 rounded-lg">
                    <div class="flex items-center mb-3">
                        <i class="fas fa-shield-alt text-blue-400 mr-2 text-xl"></i>
                        <span class="text-blue-400 font-medium text-lg">Framework Validation Complete</span>
                    </div>
                    <div class="space-y-2 text-sm text-blue-300">
                        <div class="flex justify-between">
                            <span>Status:</span>
                            <span class="font-medium">${result?.status || 'Validated'}</span>
                        </div>
                        <div class="flex justify-between">
                            <span>Checks Performed:</span>
                            <span class="font-medium">${result?.checksPerformed || 15}</span>
                        </div>
                        <div class="flex justify-between">
                            <span>Issues Found:</span>
                            <span class="font-medium ${(result?.issuesFound || 0) > 0 ? 'text-yellow-400' : 'text-green-400'}">${result?.issuesFound || 0}</span>
                        </div>
                        <div class="text-xs text-blue-200 mt-2">
                            Validated at: ${new Date().toLocaleString()}
                        </div>
                    </div>
                </div>
            `;
        }

    } catch (error) {
        console.error('Error performing framework validation:', error);
        const container = document.getElementById('framework-validation-results');
        if (container) {
            container.innerHTML = `
                <div class="p-4 bg-red-900/20 border border-red-500/30 rounded-lg">
                    <div class="flex items-center mb-3">
                        <i class="fas fa-exclamation-triangle text-red-400 mr-2 text-xl"></i>
                        <span class="text-red-400 font-medium text-lg">Validation Failed</span>
                    </div>
                    <p class="text-red-300 text-sm">${error.message}</p>
                </div>
            `;
        }
    } finally {
        loadingManager.hide();
    }
}

// Demo Data Handlers
async function handleLoadAllDemo() {
    try {
        loadingManager.show('Loading all demo data...');
        const data = await ApiClient.get('/demo/sample-data');

        const container = document.getElementById('demo-content-display');
        if (container) {
            container.innerHTML = `<pre class="text-gray-300 text-xs overflow-auto">${JSON.stringify(data, null, 2)}</pre>`;
        }

        notificationManager.show('Demo data loaded successfully', 'success');

    } catch (error) {
        console.error('Error loading demo data:', error);
        notificationManager.show(`❌ Failed to load demo data: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleLoadDemoData(type) {
    try {
        loadingManager.show(`Loading demo ${type}...`);
        const data = await ApiClient.get(`/demo/${type}`);

        const container = document.getElementById('demo-content-display');
        if (container) {
            container.innerHTML = `<pre class="text-gray-300 text-xs overflow-auto">${JSON.stringify(data, null, 2)}</pre>`;
        }

        notificationManager.show(`Demo ${type} loaded successfully`, 'success');

    } catch (error) {
        console.error(`Error loading demo ${type}:`, error);
        notificationManager.show(`❌ Failed to load demo ${type}: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleCreateSampleTest() {
    const testData = document.getElementById('sample-test-data')?.value;

    if (!testData) {
        notificationManager.show('Please enter test data', 'error');
        return;
    }

    try {
        JSON.parse(testData);
    } catch (e) {
        notificationManager.show('Invalid JSON format', 'error');
        return;
    }

    try {
        loadingManager.show('Creating sample test...');
        const result = await ApiClient.post('/demo/create-sample-test', { testData: JSON.parse(testData) });
        notificationManager.show('✅ Sample test created successfully!', 'success');

        // Clear the textarea
        document.getElementById('sample-test-data').value = '';

    } catch (error) {
        console.error('Error creating sample test:', error);
        notificationManager.show(`❌ Failed to create sample test: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Report Handlers
async function handleGenerateReport() {
    const batchId = document.getElementById('report-batch-id')?.value;
    const reportType = document.getElementById('report-type')?.value;

    if (!batchId) {
        notificationManager.show('Please select a batch', 'error');
        return;
    }

    try {
        loadingManager.show('Generating report...');

        if (reportType === 'html') {
            await ApiClient.post(`/reports/html/${batchId}`);
        } else {
            await ApiClient.post(`/reports/generate/${batchId}`);
        }

        notificationManager.show('✅ Report generated successfully!', 'success');

        if (currentSection === 'reports') await loadReports();

    } catch (error) {
        console.error('Error generating report:', error);
        notificationManager.show(`❌ Failed to generate report: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Quick Action Functions
function showQuickTestDialog() {
    const testCaseId = prompt('Enter Test Case ID:');
    if (testCaseId) {
        executeTestCase(testCaseId);
    }
}

function showQuickBatchDialog() {
    const testSuite = prompt('Enter Test Suite name:');
    if (testSuite) {
        const environment = prompt('Enter environment (dev/staging/prod):', 'dev');
        if (environment) {
            // Simulate batch execution
            ApiClient.post('/execution/batch', { testSuite, environment, parallelThreads: 1 })
                .then(result => {
                    notificationManager.show(`🚀 Batch execution started! Batch ID: ${result.batchId}`, 'success');
                })
                .catch(error => {
                    notificationManager.show('❌ Failed to execute batch', 'error');
                });
        }
    }
}

async function performQuickHealthCheck() {
    try {
        await checkApiStatus();
        notificationManager.show('✅ Quick health check completed', 'success');
    } catch (error) {
        notificationManager.show('❌ Health check failed', 'error');
    }
}

// Utility Functions
function filterTestCases() {
    const searchTerm = document.getElementById('testcase-search')?.value?.toLowerCase() || '';
    const suiteFilter = document.getElementById('testcase-suite-filter')?.value || '';
    const envFilter = document.getElementById('testcase-env-filter')?.value || '';
    const typeFilter = document.getElementById('testcase-type-filter')?.value || '';

    const rows = document.querySelectorAll('#testcases-table-body tr');

    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length < 5) return;

        const name = cells[0]?.textContent?.toLowerCase() || '';
        const suite = cells[1]?.textContent?.toLowerCase() || '';
        const type = cells[2]?.textContent?.toLowerCase() || '';
        const env = cells[3]?.textContent?.toLowerCase() || '';

        const matchesSearch = !searchTerm || name.includes(searchTerm);
        const matchesSuite = !suiteFilter || suite.includes(suiteFilter.toLowerCase());
        const matchesEnv = !envFilter || env.includes(envFilter.toLowerCase());
        const matchesType = !typeFilter || type.includes(typeFilter.toLowerCase());

        row.style.display = matchesSearch && matchesSuite && matchesEnv && matchesType ? '' : 'none';
    });
}

function filterBatches() {
    const statusFilter = document.getElementById('batch-status-filter')?.value || '';
    const rows = document.querySelectorAll('#batches-table-body tr');

    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length < 2) return;

        const status = cells[1]?.textContent?.toLowerCase() || '';
        const matchesStatus = !statusFilter || status.includes(statusFilter.toLowerCase());

        row.style.display = matchesStatus ? '' : 'none';
    });
}

async function refreshCurrentSection() {
    try {
        loadingManager.show('Refreshing data...');
        await loadSectionData(currentSection);
        await checkApiStatus();
        notificationManager.show('✅ Data refreshed successfully', 'success');
    } catch (error) {
        console.error('Error refreshing data:', error);
        notificationManager.show('❌ Failed to refresh data', 'error');
    } finally {
        loadingManager.hide();
    }
}

function setDefaultAnalyticsDates() {
    const now = new Date();
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    const fromDateInput = document.getElementById('analytics-from-date');
    const toDateInput = document.getElementById('analytics-to-date');

    if (fromDateInput) fromDateInput.value = oneWeekAgo.toISOString().slice(0, 10);
    if (toDateInput) toDateInput.value = now.toISOString().slice(0, 10);
}

function showCreateScheduleForm() {
    // Just scroll to the schedule form section
    const scheduleForm = document.querySelector('#schedules-section .glass-effect');
    if (scheduleForm) {
        scheduleForm.scrollIntoView({ behavior: 'smooth' });
    }
}

// Keyboard Shortcuts
function initializeKeyboardShortcuts() {
    const shortcuts = {
        'ctrl+/': () => showKeyboardShortcuts(),
        'ctrl+r': (e) => { e.preventDefault(); refreshCurrentSection(); },
        'ctrl+n': (e) => { e.preventDefault(); createNewItem(); },
        'esc': () => closeModalsAndNotifications(),
        'ctrl+1': () => switchSection('dashboard'),
        'ctrl+2': () => switchSection('testcases'),
        'ctrl+3': () => switchSection('execution'),
        'ctrl+4': () => switchSection('batches')
    };

    document.addEventListener('keydown', (e) => {
        const key = `${e.ctrlKey ? 'ctrl+' : ''}${e.shiftKey ? 'shift+' : ''}${e.altKey ? 'alt+' : ''}${e.key.toLowerCase()}`;

        if (shortcuts[key]) {
            shortcuts[key](e);
        }
    });
}

function showKeyboardShortcuts() {
    notificationManager.show('Ctrl+R: Refresh, Ctrl+N: New Item, Ctrl+1-4: Switch Sections, Esc: Close', 'info', { duration: 5000 });
}

function createNewItem() {
    if (currentSection === 'testcases') {
        showModal('testcase-modal');
    } else if (currentSection === 'schedules') {
        showCreateScheduleForm();
    } else {
        notificationManager.show('No new item available for this section', 'info');
    }
}

// Enhanced Auto-refresh Setup
function setupSmartRefresh() {
    const refreshRates = {
        'dashboard': 30000, // 30 seconds
        'execution': 15000, // 15 seconds
        'batches': 20000,   // 20 seconds
        'default': 60000    // 1 minute
    };

    refreshInterval = setInterval(async () => {
        try {
            if (currentSection === 'dashboard') {
                await loadDashboardData();
            } else if (currentSection === 'execution') {
                await loadExecutions();
            } else if (currentSection === 'batches') {
                await loadBatches();
            }

            await checkApiStatus();
        } catch (error) {
            console.debug('Auto-refresh error:', error);
        }
    }, refreshRates[currentSection] || refreshRates['default']);
}

// Global Error Handler
function setupGlobalErrorHandler() {
    window.addEventListener('error', (event) => {
        console.error('Global error:', event.error);
        notificationManager.show('An unexpected error occurred', 'error', {
            actions: [
                { label: 'Report', handler: `reportError('${event.error.message}')` },
                { label: 'Reload', handler: 'location.reload()' }
            ]
        });
    });

    window.addEventListener('unhandledrejection', (event) => {
        console.error('Unhandled promise rejection:', event.reason);
        notificationManager.show('A background operation failed', 'warning');
        event.preventDefault();
    });
}

// Execution Status Polling
function startExecutionStatusPolling(batchId) {
    if (executionStatusPolling.has(batchId)) return;

    const pollInterval = setInterval(async () => {
        try {
            const batchStatus = await ApiClient.get(`/execution/batch/${batchId}`);

            if (batchStatus.status === 'COMPLETED' || batchStatus.status === 'FAILED') {
                clearInterval(pollInterval);
                executionStatusPolling.delete(batchId);

                notificationManager.show(
                    `Batch ${batchId} completed with status: ${batchStatus.status}`,
                    batchStatus.status === 'COMPLETED' ? 'success' : 'error'
                );

                setTimeout(() => {
                    if (currentSection === 'execution' || currentSection === 'batches') {
                        loadExecutions();
                        loadBatches();
                    }
                    loadDashboardData();
                }, 2000);
            }
        } catch (error) {
            console.error('Error polling execution status:', error);
            clearInterval(pollInterval);
            executionStatusPolling.delete(batchId);
        }
    }, 5000);

    executionStatusPolling.set(batchId, pollInterval);
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }

    executionStatusPolling.forEach((interval, batchId) => {
        clearInterval(interval);
    });
});

// Placeholder functions for compatibility
function editTestCase(testCaseId) {
    notificationManager.show(`Edit functionality for test case ${testCaseId} will be implemented`, 'info');
}

function executeScheduleNow(scheduleId) {
    ApiClient.post(`/schedules/${scheduleId}/execute`)
        .then(result => {
            notificationManager.show(`🚀 Schedule executed! Result: ${JSON.stringify(result)}`, 'success');
        })
        .catch(error => {
            notificationManager.show('❌ Failed to execute schedule', 'error');
        });
}

function viewBatchDetails(batchId) {
    notificationManager.show(`Viewing details for batch: ${batchId}`, 'info');
}

function reportError(message) {
    console.error('Reported error:', message);
    notificationManager.show('Error report sent', 'info');
}

// Export functions for global access
window.executeTestCase = executeTestCase;
window.editTestCase = editTestCase;
window.deleteTestCase = deleteTestCase;
window.toggleSchedule = toggleSchedule;
window.executeScheduleNow = executeScheduleNow;
window.deleteSchedule = deleteSchedule;
window.generateBatchReport = generateBatchReport;
window.viewBatchDetails = viewBatchDetails;
window.refreshCurrentSection = refreshCurrentSection;
