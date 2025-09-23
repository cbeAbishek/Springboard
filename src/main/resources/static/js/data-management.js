// Data Management and API Integration Module

// Navigation and Section Management
async function initializeNavigation() {
    console.log('Initializing navigation system...');

    const navLinks = document.querySelectorAll('.nav-link');

    if (navLinks.length === 0) {
        console.error('No navigation links found!');
        return;
    }

    navLinks.forEach(link => {
        link.addEventListener('click', async function(e) {
            e.preventDefault();
            const section = this.getAttribute('data-section');
            console.log(`Navigation clicked: ${section}`);
            await switchSection(section);
        });
    });

    // Initialize mobile menu toggle
    initializeMobileNavigation();

    // Set initial active section
    console.log('Setting initial section to dashboard...');
    await switchSection('dashboard');
}

function initializeMobileNavigation() {
    const mobileToggle = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');

    if (mobileToggle && sidebar) {
        mobileToggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            const icon = mobileToggle.querySelector('i');
            if (sidebar.classList.contains('open')) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            } else {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        });

        // Close mobile menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target)) {
                sidebar.classList.remove('open');
                const icon = mobileToggle.querySelector('i');
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        });
    }
}

async function switchSection(sectionName) {
    if (currentSection === sectionName) {
        console.log(`Already in section: ${sectionName}`);
        return;
    }

    try {
        console.log(`Switching to section: ${sectionName}`);

        // Show loading state
        const loadingId = loadingManager.show(`Loading ${sectionName}...`);

        // Update navigation active state
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
            link.removeAttribute('aria-current');
        });

        const targetLink = document.querySelector(`[data-section="${sectionName}"]`);
        if (targetLink) {
            targetLink.classList.add('active');
            targetLink.setAttribute('aria-current', 'page');
        }

        // Hide all content sections with animation
        const allSections = document.querySelectorAll('.content-section');
        allSections.forEach(section => {
            section.classList.remove('active');
        });

        // Small delay to allow fade out animation
        await new Promise(resolve => setTimeout(resolve, 150));

        // Show target section
        const targetSection = document.getElementById(`${sectionName}-section`);
        if (targetSection) {
            targetSection.classList.add('active');
            console.log(`Section ${sectionName} activated`);
        } else {
            console.error(`Section not found: ${sectionName}-section`);
            notificationManager.show(`Section "${sectionName}" not found`, 'error');
            loadingManager.hide(loadingId);
            return;
        }

        // Update page title and breadcrumb
        updatePageTitle(sectionName);
        updateBreadcrumb(sectionName);

        // Load section-specific data
        await loadSectionData(sectionName);

        // Update current section
        currentSection = sectionName;

        // Close mobile menu if open
        const sidebar = document.querySelector('.sidebar');
        if (sidebar && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            const mobileToggle = document.getElementById('mobile-menu-toggle');
            if (mobileToggle) {
                const icon = mobileToggle.querySelector('i');
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        }

        // Hide loading
        loadingManager.hide(loadingId);

        console.log(`Successfully switched to section: ${sectionName}`);

    } catch (error) {
        console.error(`Error switching to section ${sectionName}:`, error);
        notificationManager.show(`Failed to load ${sectionName} section`, 'error');
        loadingManager.hide();
    }
}

function updatePageTitle(sectionName) {
    const titles = {
        'dashboard': { title: 'Dashboard', subtitle: 'Real-time monitoring and test execution' },
        'testcases': { title: 'Test Cases Management', subtitle: 'Create, manage, and organize your test cases' },
        'execution': { title: 'Test Execution', subtitle: 'Execute single tests or batch operations' },
        'batches': { title: 'Batch Management', subtitle: 'Monitor and manage test batches' },
        'schedules': { title: 'Test Schedules', subtitle: 'Create and manage automated test schedules' },
        'reports': { title: 'Test Reports', subtitle: 'Generate and view test execution reports' },
        'analytics': { title: 'Analytics & Insights', subtitle: 'Performance trends and regression analysis' },
        'validation': { title: 'System Validation', subtitle: 'Validate framework integrity and health' },
        'demo': { title: 'Demo Data', subtitle: 'Sample data for testing and demonstration' },
        'docs': { title: 'API Documentation', subtitle: 'Complete API reference and endpoint documentation' }
    };

    const titleElement = document.getElementById('page-title');
    const subtitleElement = document.getElementById('page-subtitle');

    if (titleElement && titles[sectionName]) {
        titleElement.textContent = titles[sectionName].title;
    }
    if (subtitleElement && titles[sectionName]) {
        subtitleElement.textContent = titles[sectionName].subtitle;
    }
}

function updateBreadcrumb(sectionName) {
    const breadcrumbCurrent = document.getElementById('breadcrumb-current');
    if (breadcrumbCurrent) {
        const sectionTitles = {
            'dashboard': 'Dashboard',
            'testcases': 'Test Cases',
            'execution': 'Execution',
            'batches': 'Batches',
            'schedules': 'Schedules',
            'reports': 'Reports',
            'analytics': 'Analytics',
            'validation': 'Validation',
            'demo': 'Demo',
            'docs': 'Documentation'
        };
        breadcrumbCurrent.textContent = sectionTitles[sectionName] || sectionName;
    }
}

async function loadSectionData(sectionName) {
    switch (sectionName) {
        case 'dashboard':
            await loadDashboardData();
            break;
        case 'testcases':
            await loadTestCases();
            break;
        case 'execution':
            await loadExecutions();
            break;
        case 'batches':
            await loadBatches();
            break;
        case 'schedules':
            await loadSchedules();
            break;
        case 'reports':
            await loadReports();
            break;
        case 'analytics':
            await loadAnalytics();
            break;
        case 'validation':
            await loadValidation();
            break;
        case 'demo':
            await loadDemoData();
            break;
        case 'docs':
            await loadApiDocs();
            break;
    }
}

// Enhanced Data Loading Functions
async function loadDashboardData() {
    console.log('Loading dashboard data...');

    try {
        // Show loading state for dashboard elements
        showDashboardLoading();

        // Always try to load real data first, but with shorter timeouts
        const [metricsResult, recentActivityResult] = await Promise.allSettled([
            ApiClient.get('/dashboard/metrics', { timeout: 3000 }).catch(async () => {
                console.warn('Primary dashboard metrics endpoint failed, trying fallback');
                return await loadFallbackMetrics();
            }),
            ApiClient.get('/dashboard/recent-activity', { timeout: 3000 }).catch(async () => {
                console.warn('Primary recent activity endpoint failed, trying fallback');
                return await loadFallbackRecentActivity();
            })
        ]);

        let metricsData = null;
        let activityData = null;

        // Process metrics result
        if (metricsResult.status === 'fulfilled' && metricsResult.value) {
            metricsData = transformMetricsData(metricsResult.value);
        }

        // If no metrics data, load fallback immediately
        if (!metricsData) {
            console.log('Loading fallback metrics due to API failure');
            metricsData = await loadFallbackMetrics();
        }

        // Process activity result
        if (recentActivityResult.status === 'fulfilled' && recentActivityResult.value) {
            activityData = recentActivityResult.value;
        }

        // If no activity data, load fallback immediately
        if (!activityData) {
            console.log('Loading fallback activity due to API failure');
            activityData = await loadFallbackRecentActivity();
        }

        // Always update dashboard with whatever data we have
        updateDashboardMetrics(metricsData);
        updateRecentActivity(activityData);

        // Load system health
        await loadSystemHealth();

        console.log('Dashboard data loaded successfully');

    } catch (error) {
        console.error('Error loading dashboard data:', error);

        // Ensure we always show something useful
        console.log('Loading complete fallback dashboard data due to error');
        await loadFallbackDashboardData();

        notificationManager.show('Dashboard loaded with demo data - API may be starting up', 'info', { duration: 5000 });
    }
}

// Transform API metrics data to match UI expectations
function transformMetricsData(apiMetrics) {
    console.log('Transforming API metrics:', apiMetrics);

    if (!apiMetrics) return null;

    const transformed = {
        totalTestCases: apiMetrics.testCases?.total || 0,
        activeTestCases: apiMetrics.testCases?.active || 0,
        totalBatches: apiMetrics.batches?.total || 0,
        completedBatches: apiMetrics.batches?.completed || 0,
        totalExecutions: apiMetrics.executions?.total || 0,
        passedExecutions: apiMetrics.executions?.passed || 0,
        failedExecutions: apiMetrics.executions?.failed || 0,
        activeSchedules: apiMetrics.schedules?.active || 0,
        totalSchedules: apiMetrics.schedules?.total || 0,
        successRate: apiMetrics.executions?.successRate || 0,
        // Calculate additional metrics
        newTestCasesThisWeek: Math.floor(Math.random() * 5) + 1, // TODO: Get from API
        newBatchesToday: Math.floor(Math.random() * 3) + 1, // TODO: Get from API
        runningExecutions: Math.floor(Math.random() * 2), // TODO: Get from API
        nextSchedule: 'Tomorrow 9:00 AM', // TODO: Get from API
        lastRunTime: new Date().toISOString(),
        avgDuration: 45000, // TODO: Get from API
        testsToday: Math.floor(Math.random() * 10) + 5, // TODO: Get from API
        queueSize: Math.floor(Math.random() * 5) // TODO: Get from API
    };

    console.log('Transformed metrics:', transformed);
    return transformed;
}

async function loadFallbackMetrics() {
    console.log('Loading fallback metrics from individual endpoints...');

    try {
        // Try to get data from individual endpoints
        const [testCasesResult, batchesResult, executionsResult, schedulesResult] = await Promise.allSettled([
            ApiClient.get('/testcases'),
            ApiClient.get('/execution/batches'),
            ApiClient.get('/execution/executions'),
            ApiClient.get('/schedules/active')
        ]);

        const testCases = testCasesResult.status === 'fulfilled' ? testCasesResult.value : [];
        const batches = batchesResult.status === 'fulfilled' ? batchesResult.value : [];
        const executions = executionsResult.status === 'fulfilled' ? executionsResult.value : [];
        const schedules = schedulesResult.status === 'fulfilled' ? schedulesResult.value : [];

        return {
            totalTestCases: Array.isArray(testCases) ? testCases.length : 0,
            activeTestCases: Array.isArray(testCases) ? testCases.filter(tc => tc.isActive !== false).length : 0,
            totalBatches: Array.isArray(batches) ? batches.length : 0,
            completedBatches: Array.isArray(batches) ? batches.filter(b => b.status === 'COMPLETED').length : 0,
            totalExecutions: Array.isArray(executions) ? executions.length : 0,
            passedExecutions: Array.isArray(executions) ? executions.filter(e => e.status === 'PASSED').length : 0,
            failedExecutions: Array.isArray(executions) ? executions.filter(e => e.status === 'FAILED').length : 0,
            activeSchedules: Array.isArray(schedules) ? schedules.length : 0,
            successRate: calculateSuccessRate(executions),
            newTestCasesThisWeek: 2,
            newBatchesToday: 1,
            runningExecutions: Array.isArray(executions) ? executions.filter(e => e.status === 'RUNNING').length : 0,
            nextSchedule: 'Tomorrow 9:00 AM',
            lastRunTime: new Date().toISOString(),
            avgDuration: 45000,
            testsToday: getTodayExecutions(executions),
            queueSize: 0
        };
    } catch (error) {
        console.warn('Fallback metrics also failed, using demo data');
        return getDemoMetrics();
    }
}

function getDemoMetrics() {
    return {
        totalTestCases: 12,
        activeTestCases: 10,
        totalBatches: 8,
        completedBatches: 6,
        totalExecutions: 45,
        passedExecutions: 38,
        failedExecutions: 7,
        activeSchedules: 3,
        totalSchedules: 5,
        successRate: 84.4,
        newTestCasesThisWeek: 2,
        newBatchesToday: 1,
        runningExecutions: 0,
        nextSchedule: 'Tomorrow 9:00 AM',
        lastRunTime: new Date().toISOString(),
        avgDuration: 45000,
        testsToday: 15,
        queueSize: 0
    };
}

async function loadFallbackDashboardData() {
    console.log('Loading complete fallback dashboard data...');

    const demoMetrics = getDemoMetrics();
    const demoActivity = {
        recentBatches: [
            {
                batchId: 'demo-001',
                name: 'Smoke Test Suite',
                status: 'COMPLETED',
                totalTests: 12,
                passedTests: 11,
                failedTests: 1,
                startTime: new Date(Date.now() - 3600000).toISOString(),
                endTime: new Date(Date.now() - 1800000).toISOString()
            },
            {
                batchId: 'demo-002',
                name: 'Regression Suite',
                status: 'RUNNING',
                totalTests: 25,
                passedTests: 18,
                failedTests: 2,
                startTime: new Date(Date.now() - 1800000).toISOString(),
                endTime: null
            }
        ],
        systemHealth: getDefaultSystemHealth()
    };

    updateDashboardMetrics(demoMetrics);
    updateRecentActivity(demoActivity);
    await loadSystemHealth();
}

async function loadTestCases() {
    try {
        console.log('Loading test cases...');
        const testCases = await ApiClient.get('/testcases');
        updateTestCasesTable(testCases);
        populateTestCaseFilters(testCases);
        updateTestCaseCount(testCases?.length || 0);
    } catch (error) {
        console.error('Error loading test cases:', error);
        notificationManager.show('Failed to load test cases', 'error');

        // Show empty state
        const tbody = document.getElementById('testcases-table-body');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="px-6 py-8 text-center text-gray-400">
                        <div class="empty-state">
                            <div class="empty-state-icon">
                                <i class="fas fa-exclamation-triangle"></i>
                            </div>
                            <div class="empty-state-title">Failed to load test cases</div>
                            <div class="empty-state-description">
                                Check your API connection and try again
                            </div>
                            <button onclick="loadTestCases()" class="btn btn-primary mt-4">
                                <i class="fas fa-retry mr-2"></i>Retry
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }
    }
}

function updateTestCaseCount(count) {
    const countElement = document.getElementById('testcase-count');
    if (countElement) {
        countElement.textContent = count;
    }
}

function updateTestCasesTable(testCases) {
    const tbody = document.getElementById('testcases-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!testCases || testCases.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="px-6 py-8 text-center text-gray-400">
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-clipboard-list"></i>
                        </div>
                        <div class="empty-state-title">No test cases found</div>
                        <div class="empty-state-description">
                            Create your first test case to get started
                        </div>
                        <button onclick="showModal('testcase-modal')" class="btn btn-primary mt-4">
                            <i class="fas fa-plus mr-2"></i>Create Test Case
                        </div>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    testCases.forEach(testCase => {
        const row = document.createElement('tr');
        row.className = 'hover:bg-white/5 transition-colors';
        row.innerHTML = `
            <td class="px-6 py-4">
                <input type="checkbox" class="custom-checkbox testcase-checkbox" value="${testCase.id}">
            </td>
            <td class="px-6 py-4 text-white font-medium">${testCase.name}</td>
            <td class="px-6 py-4 text-gray-300">${testCase.testSuite || 'default'}</td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                    ${testCase.testType || 'WEB_UI'}
                </span>
            </td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">
                    ${testCase.environment || 'dev'}
                </span>
            </td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full 
                    ${testCase.priority === 'HIGH' ? 'bg-red-100 text-red-800' : 
                      testCase.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' : 
                      'bg-green-100 text-green-800'}">
                    ${testCase.priority || 'MEDIUM'}
                </span>
            </td>
            <td class="px-6 py-4 text-gray-400 text-xs">
                ${testCase.lastRun ? formatDateTime(testCase.lastRun) : 'Never'}
            </td>
            <td class="px-6 py-4">
                <div class="flex space-x-2">
                    <button onclick="executeTestCase(${testCase.id})" 
                            class="text-green-400 hover:text-green-300 transition-colors" 
                            title="Execute Test" data-tooltip="Execute this test case">
                        <i class="fas fa-play"></i>
                    </button>
                    <button onclick="editTestCase(${testCase.id})" 
                            class="text-blue-400 hover:text-blue-300 transition-colors" 
                            title="Edit Test" data-tooltip="Edit this test case">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button onclick="deleteTestCase(${testCase.id})" 
                            class="text-red-400 hover:text-red-300 transition-colors" 
                            title="Delete Test" data-tooltip="Delete this test case">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });

    // Setup checkbox handlers for bulk actions
    setupBulkActionHandlers();
}

function setupBulkActionHandlers() {
    const selectAllCheckbox = document.getElementById('select-all-testcases');
    const checkboxes = document.querySelectorAll('.testcase-checkbox');
    const bulkPanel = document.getElementById('bulk-actions-panel');
    const selectedCountSpan = document.getElementById('selected-count');

    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            checkboxes.forEach(cb => {
                cb.checked = this.checked;
            });
            updateBulkPanel();
        });
    }

    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', updateBulkPanel);
    });

    function updateBulkPanel() {
        const selectedCheckboxes = document.querySelectorAll('.testcase-checkbox:checked');
        const count = selectedCheckboxes.length;

        if (selectedCountSpan) {
            selectedCountSpan.textContent = count;
        }

        if (bulkPanel) {
            if (count > 0) {
                bulkPanel.classList.remove('scale-0');
                bulkPanel.classList.add('scale-100');
            } else {
                bulkPanel.classList.remove('scale-100');
                bulkPanel.classList.add('scale-0');
            }
        }
    }
}

function populateTestCaseFilters(testCases) {
    // Populate test suite filter
    const suiteFilter = document.getElementById('testcase-suite-filter');
    if (suiteFilter && testCases) {
        const suites = [...new Set(testCases.map(tc => tc.testSuite).filter(Boolean))];
        suiteFilter.innerHTML = '<option value="">All Test Suites</option>';
        suites.forEach(suite => {
            const option = document.createElement('option');
            option.value = suite;
            option.textContent = suite;
            suiteFilter.appendChild(option);
        });
    }

    // Populate test case dropdown for single execution
    const singleTestSelect = document.getElementById('single-test-select');
    if (singleTestSelect && testCases) {
        singleTestSelect.innerHTML = '<option value="">Select a test case...</option>';
        testCases.forEach(testCase => {
            const option = document.createElement('option');
            option.value = testCase.id;
            option.textContent = `${testCase.name} (${testCase.testType || 'WEB_UI'})`;
            singleTestSelect.appendChild(option);
        });
    }
}

async function loadBatches() {
    try {
        console.log('Loading batches...');
        const [batches, activeBatches, recentBatches] = await Promise.allSettled([
            ApiClient.get('/execution/batches'),
            ApiClient.get('/execution/batches/active'),
            ApiClient.get('/execution/batches/recent')
        ]);

        const batchesData = batches.status === 'fulfilled' ? batches.value : [];
        const activeBatchesData = activeBatches.status === 'fulfilled' ? activeBatches.value : [];
        const recentBatchesData = recentBatches.status === 'fulfilled' ? recentBatches.value : [];

        updateBatchesTable(batchesData);
        updateBatchStats(activeBatchesData, recentBatchesData, batchesData);
        populateBatchDropdowns(batchesData);
    } catch (error) {
        console.error('Error loading batches:', error);
        notificationManager.show('Failed to load batches', 'error');
    }
}

function updateBatchesTable(batches) {
    const tbody = document.getElementById('batches-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!batches || batches.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-400">
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-layer-group"></i>
                        </div>
                        <div class="empty-state-title">No batches found</div>
                        <div class="empty-state-description">Execute some tests to see batches here</div>
                        <button onclick="switchSection('execution')" class="btn btn-primary mt-4">
                            <i class="fas fa-play mr-2"></i>Start Execution
                        </button>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    batches.slice(0, 50).forEach(batch => {
        const row = document.createElement('tr');
        row.className = 'hover:bg-white/5 transition-colors';
        row.innerHTML = `
            <td class="px-6 py-4 font-mono text-sm text-white">${batch.batchId}</td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full 
                    ${getStatusColor(batch.status)}">
                    ${batch.status}
                </span>
            </td>
            <td class="px-6 py-4 text-gray-300">${batch.testSuite || 'N/A'}</td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">
                    ${batch.environment || 'dev'}
                </span>
            </td>
            <td class="px-6 py-4 text-gray-300">${formatDateTime(batch.createdAt)}</td>
            <td class="px-6 py-4">
                <div class="flex space-x-2">
                    <button onclick="viewBatchDetails('${batch.batchId}')" 
                            class="text-blue-400 hover:text-blue-300 transition-colors" 
                            title="View Details" data-tooltip="View batch details">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button onclick="generateBatchReport('${batch.batchId}')" 
                            class="text-green-400 hover:text-green-300 transition-colors" 
                            title="Generate Report" data-tooltip="Generate batch report">
                        <i class="fas fa-file-alt"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function updateBatchStats(activeBatches, recentBatches, allBatches) {
    const activeCount = document.getElementById('active-batches-count');
    const recentCount = document.getElementById('recent-batches-count');
    const successRate = document.getElementById('batch-success-rate');

    if (activeCount) activeCount.textContent = activeBatches?.length || 0;
    if (recentCount) recentCount.textContent = recentBatches?.length || 0;

    if (successRate && allBatches?.length > 0) {
        const completedBatches = allBatches.filter(b => b.status === 'COMPLETED');
        const rate = Math.round((completedBatches.length / allBatches.length) * 100);
        successRate.textContent = `${rate}%`;
    } else if (successRate) {
        successRate.textContent = '--';
    }
}

function populateBatchDropdowns(batches) {
    // Populate report batch dropdown
    const reportBatchId = document.getElementById('report-batch-id');
    if (reportBatchId && batches) {
        reportBatchId.innerHTML = '<option value="">Select batch...</option>';
        batches.slice(0, 20).forEach(batch => {
            const option = document.createElement('option');
            option.value = batch.batchId;
            option.textContent = `${batch.batchId} - ${batch.status}`;
            reportBatchId.appendChild(option);
        });
    }
}

function getStatusColor(status) {
    const colors = {
        'COMPLETED': 'bg-green-100 text-green-800',
        'RUNNING': 'bg-blue-100 text-blue-800',
        'SCHEDULED': 'bg-yellow-100 text-yellow-800',
        'FAILED': 'bg-red-100 text-red-800',
        'CANCELLED': 'bg-gray-100 text-gray-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
}

async function loadExecutions() {
    try {
        const executions = await ApiClient.get('/execution/executions');
        updateCurrentExecutions(executions);
        updateExecutionStats(executions);
    } catch (error) {
        console.error('Error loading executions:', error);
        notificationManager.show('Failed to load executions', 'error');
    }
}

function updateCurrentExecutions(executions) {
    const container = document.getElementById('current-executions');
    if (!container) return;

    container.innerHTML = '';

    if (!executions || executions.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-play-circle"></i>
                </div>
                <div class="empty-state-title">No Active Executions</div>
                <div class="empty-state-description">Start a test execution to see real-time progress here</div>
            </div>
        `;
        return;
    }

    const recentExecutions = executions.slice(0, 10);
    recentExecutions.forEach(execution => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10';
        item.innerHTML = `
            <div>
                <div class="font-medium text-white">${execution.testCase?.name || 'Unknown Test'}</div>
                <div class="text-sm text-gray-400">ID: ${execution.id} | ${execution.environment || 'dev'}</div>
            </div>
            <div class="text-right">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(execution.status)}">
                    ${execution.status}
                </span>
                <div class="text-xs text-gray-400 mt-1">${formatDateTime(execution.startTime)}</div>
            </div>
        `;
        container.appendChild(item);
    });
}

function updateExecutionStats(executions) {
    if (!executions) return;

    const stats = {
        running: executions.filter(e => e.status === 'RUNNING').length,
        queued: executions.filter(e => e.status === 'SCHEDULED').length,
        completed: executions.filter(e => e.status === 'COMPLETED' && isToday(e.completedAt)).length,
        failed: executions.filter(e => e.status === 'FAILED' && isToday(e.completedAt)).length
    };

    const statElements = [
        { id: 'executions-running', value: stats.running },
        { id: 'executions-queued', value: stats.queued },
        { id: 'executions-completed', value: stats.completed },
        { id: 'executions-failed', value: stats.failed }
    ];

    statElements.forEach(({ id, value }) => {
        const element = document.getElementById(id);
        if (element) {
            animateCounter(element, 0, value, 500);
        }
    });
}

function isToday(dateString) {
    if (!dateString) return false;
    const today = new Date().toDateString();
    return new Date(dateString).toDateString() === today;
}

async function loadSchedules() {
    try {
        const schedules = await ApiClient.get('/schedules');
        updateSchedulesTable(schedules);
    } catch (error) {
        console.error('Error loading schedules:', error);
        notificationManager.show('Failed to load schedules', 'error');
    }
}

function updateSchedulesTable(schedules) {
    const tbody = document.getElementById('schedules-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!schedules || schedules.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-8 text-center text-gray-400">
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-clock"></i>
                        </div>
                        <div class="empty-state-title">No schedules found</div>
                        <div class="empty-state-description">
                            Create your first schedule to automate test execution
                        </div>
                        <button onclick="showCreateScheduleModal()" class="btn btn-primary mt-4">
                            <i class="fas fa-plus mr-2"></i>Create Schedule
                        </button>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    schedules.forEach(schedule => {
        const row = document.createElement('tr');
        row.className = 'hover:bg-white/5 transition-colors';
        row.innerHTML = `
            <td class="px-6 py-4 text-white font-medium">${schedule.scheduleName}</td>
            <td class="px-6 py-4 text-gray-300">${schedule.testSuite}</td>
            <td class="px-6 py-4 font-mono text-sm text-gray-300">${schedule.cronExpression}</td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full 
                    ${schedule.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}">
                    ${schedule.active ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td class="px-6 py-4">
                <span class="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-800">
                    ${schedule.environment}
                </span>
            </td>
            <td class="px-6 py-4">
                <div class="flex space-x-2">
                    <button onclick="toggleSchedule(${schedule.id}, ${!schedule.active})" 
                            class="text-${schedule.active ? 'yellow' : 'green'}-400 hover:text-${schedule.active ? 'yellow' : 'green'}-300 transition-colors" 
                            title="${schedule.active ? 'Disable' : 'Enable'} Schedule" data-tooltip="${schedule.active ? 'Disable' : 'Enable'} this schedule">
                        <i class="fas fa-${schedule.active ? 'pause' : 'play'}"></i>
                    </button>
                    <button onclick="executeScheduleNow(${schedule.id})" 
                            class="text-blue-400 hover:text-blue-300 transition-colors" 
                            title="Execute Now" data-tooltip="Execute this schedule immediately">
                        <i class="fas fa-bolt"></i>
                    </button>
                    <button onclick="deleteSchedule(${schedule.id})" 
                            class="text-red-400 hover:text-red-300 transition-colors" 
                            title="Delete Schedule" data-tooltip="Delete this schedule">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

async function loadReports() {
    try {
        // Since there's no specific reports endpoint, we'll simulate with batch data
        const batches = await ApiClient.get('/execution/batches');
        updateReportsDisplay(batches);
    } catch (error) {
        console.error('Error loading reports:', error);
        notificationManager.show('Failed to load reports', 'error');
    }
}

function updateReportsDisplay(batches) {
    const reportsList = document.getElementById('reports-list');
    if (!reportsList) return;

    reportsList.innerHTML = '';

    if (!batches || batches.length === 0) {
        reportsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-chart-bar"></i>
                </div>
                <div class="empty-state-title">No reports available</div>
                <div class="empty-state-description">Execute some test batches to generate reports</div>
            </div>
        `;
        return;
    }

    const completedBatches = batches.filter(b => b.status === 'COMPLETED').slice(0, 10);
    completedBatches.forEach(batch => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10';
        item.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center">
                    <i class="fas fa-file-alt text-green-400"></i>
                </div>
                <div>
                    <div class="font-medium text-white">Report for ${batch.batchId}</div>
                    <div class="text-sm text-gray-400">Generated: ${formatDateTime(batch.completedAt || batch.createdAt)}</div>
                </div>
            </div>
            <div class="flex space-x-2">
                <button onclick="generateBatchReport('${batch.batchId}')" 
                        class="btn btn-sm btn-primary">
                    <i class="fas fa-file-alt mr-1"></i>Generate
                </button>
                <button onclick="downloadReport('${batch.batchId}')" 
                        class="btn btn-sm btn-secondary">
                    <i class="fas fa-download mr-1"></i>Download
                </button>
            </div>
        `;
        reportsList.appendChild(item);
    });
}

async function loadAnalytics() {
    // Analytics loads on demand
    const container = document.getElementById('trend-analysis-content');
    if (container) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-chart-line"></i>
                </div>
                <div class="empty-state-title">Analytics Ready</div>
                <div class="empty-state-description">Select date range and click "Load Trends" to view analytics</div>
            </div>
        `;
    }

    const regressionContainer = document.getElementById('regression-metrics-content');
    if (regressionContainer) {
        regressionContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-chart-area"></i>
                </div>
                <div class="empty-state-title">Regression Analysis Ready</div>
                <div class="empty-state-description">Configure settings and click "Load Regression Data" to view metrics</div>
            </div>
        `;
    }
}

async function loadValidation() {
    // Validation loads on demand
    const healthResults = document.getElementById('health-check-results');
    const frameworkResults = document.getElementById('framework-validation-results');

    if (healthResults) {
        healthResults.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-heartbeat"></i>
                </div>
                <div class="empty-state-title">Health Check Ready</div>
                <div class="empty-state-description">Click "Health Check" to validate system status</div>
            </div>
        `;
    }

    if (frameworkResults) {
        frameworkResults.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-shield-alt"></i>
                </div>
                <div class="empty-state-title">Framework Validation Ready</div>
                <div class="empty-state-description">Click "Full Validation" to perform comprehensive checks</div>
            </div>
        `;
    }
}

async function loadDemoData() {
    const container = document.getElementById('demo-content-display');
    if (container) {
        container.innerHTML = `
            <div class="text-center py-8 text-gray-400">
                <i class="fas fa-flask text-2xl mb-2"></i>
                <p>Demo Data Ready</p>
                <p class="text-sm">Select a demo data type above to view sample content</p>
            </div>
        `;
    }
}

async function loadApiDocs() {
    try {
        const docs = await ApiClient.get('/docs/endpoints');
        updateApiDocsDisplay(docs);
    } catch (error) {
        console.error('Error loading API docs:', error);
        const container = document.getElementById('api-docs-content');
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">
                        <i class="fas fa-exclamation-triangle"></i>
                    </div>
                    <div class="empty-state-title">Failed to load API documentation</div>
                    <div class="empty-state-description">${error.message}</div>
                    <button onclick="loadApiDocs()" class="btn btn-primary mt-4">
                        <i class="fas fa-retry mr-2"></i>Retry
                    </button>
                </div>
            `;
        }
    }
}

function updateApiDocsDisplay(docs) {
    const container = document.getElementById('api-docs-content');
    if (!container) return;

    if (!docs || !docs.endpoints) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-book"></i>
                </div>
                <div class="empty-state-title">No API documentation available</div>
                <div class="empty-state-description">Documentation will be loaded when available</div>
            </div>
        `;
        return;
    }

    let html = '<div class="space-y-6">';

    // Group endpoints by category
    const categories = {};
    docs.endpoints.forEach(endpoint => {
        const category = endpoint.path.split('/')[2] || 'General';
        if (!categories[category]) categories[category] = [];
        categories[category].push(endpoint);
    });

    Object.entries(categories).forEach(([category, endpoints]) => {
        html += `
            <div class="border border-white/10 rounded-lg p-4">
                <h3 class="text-lg font-semibold text-white mb-4 capitalize flex items-center">
                    <i class="fas fa-folder text-cyan-400 mr-2"></i>
                    ${category} Endpoints
                </h3>
                <div class="space-y-3">
        `;

        endpoints.forEach(endpoint => {
            const methodColor = {
                'GET': 'bg-green-100 text-green-800',
                'POST': 'bg-blue-100 text-blue-800',
                'PUT': 'bg-yellow-100 text-yellow-800',
                'DELETE': 'bg-red-100 text-red-800'
            };

            html += `
                <div class="bg-gray-800 rounded p-3 hover:bg-gray-700 transition-colors">
                    <div class="flex items-center space-x-3 mb-2">
                        <span class="inline-flex px-2 py-1 text-xs font-semibold rounded ${methodColor[endpoint.method] || 'bg-gray-100 text-gray-800'}">
                            ${endpoint.method}
                        </span>
                        <code class="text-cyan-400 text-sm">${endpoint.path}</code>
                    </div>
                    <p class="text-gray-300 text-sm mb-2">${endpoint.description}</p>
                    ${endpoint.parameters ? `<div class="text-xs text-gray-400">Parameters: ${Object.keys(endpoint.parameters).join(', ')}</div>` : ''}
                </div>
            `;
        });

        html += '</div></div>';
    });

    html += '</div>';
    container.innerHTML = html;
}

// Utility functions for dashboard
function animateCounter(element, start, end, duration) {
    if (!element) return;

    const startTime = performance.now();
    const difference = end - start;

    function updateCounter(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Use easing function for smooth animation
        const easeOutCubic = 1 - Math.pow(1 - progress, 3);
        const currentValue = Math.floor(start + (difference * easeOutCubic));

        element.textContent = currentValue;

        if (progress < 1) {
            requestAnimationFrame(updateCounter);
        } else {
            element.textContent = end; // Ensure final value is exact
        }
    }

    requestAnimationFrame(updateCounter);
}

function formatDateTime(dateString) {
    if (!dateString) return '--';

    try {
        const date = new Date(dateString);
        const now = new Date();
        const diffInMs = now - date;
        const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
        const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
        const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

        if (diffInMinutes < 1) return 'Just now';
        if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
        if (diffInHours < 24) return `${diffInHours}h ago`;
        if (diffInDays < 7) return `${diffInDays}d ago`;

        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
        });
    } catch (error) {
        console.warn('Error formatting date:', error);
        return '--';
    }
}

function formatDuration(milliseconds) {
    if (!milliseconds || milliseconds < 0) return '--';

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
}

// Global function to view batch details
function viewBatchDetails(batchId) {
    console.log('Viewing batch details for:', batchId);
    // TODO: Implement batch details modal or navigation
    notificationManager.show(`Viewing details for batch ${batchId}`, 'info');
}

// Add to the end of data-management.js file
function calculateSuccessRate(executions) {
    if (!executions || executions.length === 0) return 0;

    const completedExecutions = executions.filter(e => e.status === 'COMPLETED' || e.status === 'PASSED');
    return Math.round((completedExecutions.length / executions.length) * 100);
}

function getTodayExecutions(executions) {
    if (!executions) return 0;

    const today = new Date().toDateString();
    return executions.filter(e => {
        const executionDate = new Date(e.startTime || e.createdAt).toDateString();
        return executionDate === today;
    }).length;
}

function showDashboardLoading() {
    // Show skeleton loading for metrics
    const metricElements = ['total-test-cases', 'total-batches', 'total-executions', 'active-schedules'];
    metricElements.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.innerHTML = '<div class="animate-pulse bg-gray-600 rounded h-8 w-16"></div>';
        }
    });

    // Show loading for activity lists
    const recentActivityList = document.getElementById('recent-activity-list');
    if (recentActivityList) {
        recentActivityList.innerHTML = `
            <div class="animate-pulse space-y-3">
                <div class="bg-gray-600 rounded-lg h-16"></div>
                <div class="bg-gray-600 rounded-lg h-16"></div>
                <div class="bg-gray-600 rounded-lg h-16"></div>
            </div>
        `;
    }

    const systemHealthDetails = document.getElementById('system-health-details');
    if (systemHealthDetails) {
        systemHealthDetails.innerHTML = `
            <div class="animate-pulse space-y-2">
                <div class="bg-gray-600 rounded h-6"></div>
                <div class="bg-gray-600 rounded h-6"></div>
                <div class="bg-gray-600 rounded h-6"></div>
            </div>
        `;
    }
}

function updateDashboardMetrics(metrics) {
    console.log('Updating dashboard metrics:', metrics);

    if (!metrics) return;

    // Update main metric cards with animation
    const updates = [
        { id: 'total-test-cases', value: metrics.totalTestCases, color: 'text-cyan-400' },
        { id: 'total-batches', value: metrics.totalBatches, color: 'text-blue-400' },
        { id: 'total-executions', value: metrics.totalExecutions, color: 'text-green-400' },
        { id: 'active-schedules', value: metrics.activeSchedules, color: 'text-yellow-400' }
    ];

    updates.forEach(({ id, value, color }) => {
        const element = document.getElementById(id);
        if (element) {
            // Animate counter
            animateCounter(element, 0, value || 0, 1000);
            element.className = `text-3xl font-bold text-white animate-counter`;
        }
    });

    // Update progress bars
    updateProgressBars(metrics);

    // Update trend indicators
    updateTrendIndicators(metrics);

    // Update mini stats in sidebar
    updateMiniStats(metrics);

    // Update quick stats
    updateQuickStats(metrics);
}

function updateProgressBars(metrics) {
    const progressBars = [
        { id: 'test-cases-progress', value: Math.min((metrics.totalTestCases || 0) * 2, 100) },
        { id: 'batches-progress', value: Math.min((metrics.totalBatches || 0) * 5, 100) },
        { id: 'executions-progress', value: Math.min((metrics.totalExecutions || 0), 100) },
        { id: 'schedules-progress', value: Math.min((metrics.activeSchedules || 0) * 10, 100) }
    ];

    progressBars.forEach(({ id, value }) => {
        const progressBar = document.getElementById(id);
        if (progressBar) {
            setTimeout(() => {
                progressBar.style.width = `${value}%`;
            }, 300);
        }
    });
}

function updateTrendIndicators(metrics) {
    const trends = [
        { id: 'test-cases-trend', value: `+${metrics.newTestCasesThisWeek || 0} this week` },
        { id: 'batches-trend', value: `+${metrics.newBatchesToday || 0} today` },
        { id: 'executions-trend', value: `${metrics.runningExecutions || 0} running` },
        { id: 'schedules-trend', value: `Next: ${metrics.nextSchedule || '--'}` }
    ];

    trends.forEach(({ id, value }) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    });
}

function updateMiniStats(metrics) {
    const miniStats = [
        { id: 'mini-active-tests', value: metrics.runningExecutions || 0 },
        { id: 'mini-success-rate', value: `${metrics.successRate || 0}%` },
        { id: 'mini-last-run', value: formatDateTime(metrics.lastRunTime) || '--' }
    ];

    miniStats.forEach(({ id, value }) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    });
}

function updateQuickStats(metrics) {
    const quickStats = [
        { id: 'overall-success-rate', value: `${metrics.successRate || 0}%` },
        { id: 'avg-duration', value: formatDuration(metrics.avgDuration) || '--' },
        { id: 'tests-today', value: metrics.testsToday || 0 },
        { id: 'queue-size', value: metrics.queueSize || 0 }
    ];

    quickStats.forEach(({ id, value }) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    });
}

function updateRecentActivity(activity) {
    console.log('Updating recent activity:', activity);

    if (!activity) return;

    // Update recent batches/activity list
    const recentActivityList = document.getElementById('recent-activity-list');
    if (recentActivityList) {
        if (!activity.recentBatches || activity.recentBatches.length === 0) {
            recentActivityList.innerHTML = `
                <div class="text-center py-8 text-gray-400">
                    <i class="fas fa-layer-group text-2xl mb-2"></i>
                    <p>No recent activity</p>
                    <button onclick="switchSection('execution')" class="text-cyan-400 hover:underline text-sm mt-2">
                        Start a new test execution
                    </button>
                </div>
            `;
        } else {
            recentActivityList.innerHTML = '';
            activity.recentBatches.slice(0, 5).forEach(batch => {
                const item = document.createElement('div');
                item.className = 'flex items-center justify-between p-3 glass-effect rounded-lg border border-white/10 hover:bg-white/5 transition-colors cursor-pointer';
                item.onclick = () => viewBatchDetails(batch.batchId);

                const statusColor = batch.status === 'COMPLETED' ? 'text-green-400' :
                                  batch.status === 'RUNNING' ? 'text-blue-400' :
                                  batch.status === 'FAILED' ? 'text-red-400' : 'text-yellow-400';

                item.innerHTML = `
                    <div class="flex items-center space-x-3">
                        <div class="w-8 h-8 rounded-full bg-${batch.status === 'COMPLETED' ? 'green' : batch.status === 'RUNNING' ? 'blue' : 'red'}-500/20 flex items-center justify-center">
                            <i class="fas fa-${batch.status === 'COMPLETED' ? 'check' : batch.status === 'RUNNING' ? 'play' : 'times'} text-xs ${statusColor}"></i>
                        </div>
                        <div>
                            <div class="font-medium text-white text-sm">${batch.name || batch.batchId}</div>
                            <div class="text-xs text-gray-400">
                                ${batch.totalTests || 0} tests • ${formatDateTime(batch.startTime)}
                            </div>
                        </div>
                    </div>
                    <div class="text-right">
                        <div class="text-xs ${statusColor} font-semibold">${batch.status}</div>
                        <div class="text-xs text-gray-400">
                            ${batch.passedTests || 0}/${batch.totalTests || 0}
                        </div>
                    </div>
                `;
                recentActivityList.appendChild(item);
            });
        }
    }
}

async function loadSystemHealth() {
    try {
        const health = await loadSystemHealthData();
        updateSystemHealthDisplay(health);
    } catch (error) {
        console.warn('Failed to load system health:', error);
        updateSystemHealthDisplay(getDefaultSystemHealth());
    }
}

async function loadSystemHealthData() {
    try {
        // Try to get health data from API
        const health = await ApiClient.get('/actuator/health');
        return health;
    } catch (error) {
        console.warn('Health endpoint not available, using simulated data');
        return getDefaultSystemHealth();
    }
}

function getDefaultSystemHealth() {
    return {
        status: 'UP',
        components: {
            database: { status: 'UP', details: { connection: 'active' } },
            selenium: { status: 'UP', details: { drivers: 'ready' } },
            scheduler: { status: 'UP', details: { jobs: 'active' } },
            memory: { status: 'UP', details: { usage: '45%' } }
        },
        performance: {
            responseTime: Math.floor(Math.random() * 100) + 50,
            activeConnections: Math.floor(Math.random() * 10) + 5,
            memoryUsage: Math.floor(Math.random() * 30) + 40,
            queueLength: Math.floor(Math.random() * 5)
        }
    };
}

function updateSystemHealthDisplay(health) {
    const systemHealthDetails = document.getElementById('system-health-details');
    if (systemHealthDetails && health) {
        const healthItems = [];

        if (health.components) {
            Object.entries(health.components).forEach(([component, details]) => {
                const isUp = details.status === 'UP';
                healthItems.push(`
                    <div class="flex items-center justify-between py-2">
                        <span class="text-sm text-gray-300 capitalize">${component}</span>
                        <span class="flex items-center">
                            <div class="w-2 h-2 rounded-full ${isUp ? 'bg-green-400' : 'bg-red-400'} mr-2"></div>
                            <span class="text-xs ${isUp ? 'text-green-400' : 'text-red-400'}">${details.status}</span>
                        </span>
                    </div>
                `);
            });
        }

        systemHealthDetails.innerHTML = healthItems.join('') || `
            <div class="text-center py-4 text-gray-400">
                <i class="fas fa-heartbeat text-green-400 text-lg mb-2"></i>
                <div class="text-sm">System Healthy</div>
            </div>
        `;
    }

    // Update performance stats in header
    if (health.performance) {
        const responseTime = document.getElementById('response-time');
        const activeConnections = document.getElementById('active-connections');
        const memoryUsage = document.getElementById('memory-usage');
        const queueLength = document.getElementById('queue-length');

        if (responseTime) responseTime.textContent = `${health.performance.responseTime}ms`;
        if (activeConnections) activeConnections.textContent = health.performance.activeConnections;
        if (memoryUsage) memoryUsage.textContent = `${health.performance.memoryUsage}%`;
        if (queueLength) queueLength.textContent = health.performance.queueLength;
    }
}

async function loadFallbackRecentActivity() {
    try {
        const [recentBatches, systemHealth] = await Promise.allSettled([
            ApiClient.get('/execution/batches/recent'),
            loadSystemHealthData()
        ]);

        return {
            recentBatches: recentBatches.status === 'fulfilled' ? recentBatches.value : [],
            systemHealth: systemHealth.status === 'fulfilled' ? systemHealth.value : getDefaultSystemHealth()
        };
    } catch (error) {
        return {
            recentBatches: [],
            systemHealth: getDefaultSystemHealth()
        };
    }
}

// Auto-refresh dashboard data
function startDashboardAutoRefresh() {
    // Refresh dashboard data every 30 seconds
    setInterval(async () => {
        if (currentSection === 'dashboard') {
            try {
                await loadDashboardData();
            } catch (error) {
                console.debug('Auto-refresh failed:', error);
            }
        }
    }, 30000);
}

// Export functions for global access
window.initializeNavigation = initializeNavigation;
window.switchSection = switchSection;
window.loadDashboardData = loadDashboardData;
window.loadTestCases = loadTestCases;
window.loadBatches = loadBatches;
window.loadExecutions = loadExecutions;
window.loadSchedules = loadSchedules;
window.loadReports = loadReports;
window.loadAnalytics = loadAnalytics;
window.loadValidation = loadValidation;
window.loadDemoData = loadDemoData;
window.loadApiDocs = loadApiDocs;
window.updateDashboardMetrics = updateDashboardMetrics;
window.updateRecentActivity = updateRecentActivity;
window.startDashboardAutoRefresh = startDashboardAutoRefresh;
