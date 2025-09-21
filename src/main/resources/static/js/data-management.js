// Data Management and API Integration Module

// Navigation and Section Management
async function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');

    navLinks.forEach(link => {
        link.addEventListener('click', async function(e) {
            e.preventDefault();
            const section = this.getAttribute('data-section');
            await switchSection(section);
        });
    });

    // Set initial active section
    await switchSection('dashboard');
}

async function switchSection(sectionName) {
    if (currentSection === sectionName) return;

    try {
        loadingManager.show(`Loading ${sectionName}...`);

        // Update navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        const targetLink = document.querySelector(`[data-section="${sectionName}"]`);
        if (targetLink) targetLink.classList.add('active');

        // Hide all content sections
        document.querySelectorAll('.content-section').forEach(section => {
            section.classList.remove('active');
        });

        // Show target section
        const targetSection = document.getElementById(`${sectionName}-section`);
        if (targetSection) targetSection.classList.add('active');

        // Update page title and breadcrumb
        updatePageTitle(sectionName);
        updateBreadcrumb(sectionName);

        // Load section-specific data
        await loadSectionData(sectionName);

        currentSection = sectionName;
    } catch (error) {
        console.error(`Error switching to section ${sectionName}:`, error);
        notificationManager.show(`Failed to load ${sectionName} section`, 'error');
    } finally {
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

        // Load dashboard metrics and recent activity in parallel
        const [metrics, recentActivity] = await Promise.allSettled([
            ApiClient.get('/dashboard/metrics').catch(e => {
                console.warn('Dashboard metrics endpoint not available, using fallback');
                return loadFallbackMetrics();
            }),
            ApiClient.get('/dashboard/recent-activity').catch(e => {
                console.warn('Recent activity endpoint not available, using fallback');
                return loadFallbackRecentActivity();
            })
        ]);

        // Update dashboard with loaded data
        if (metrics.status === 'fulfilled') {
            updateDashboardMetrics(metrics.value);
        }

        if (recentActivity.status === 'fulfilled') {
            updateRecentActivity(recentActivity.value);
        }

        // Load system health and performance stats
        await loadSystemHealth();

        console.log('Dashboard data loaded successfully');

    } catch (error) {
        console.error('Error loading dashboard data:', error);
        notificationManager.show('Some dashboard data may not be available', 'warning');

        // Load fallback data to show something useful
        await loadFallbackDashboardData();
    }
}

async function loadFallbackMetrics() {
    console.log('Loading fallback metrics from individual endpoints...');

    try {
        // Try to get data from individual endpoints
        const [testCases, batches, executions, schedules] = await Promise.allSettled([
            ApiClient.get('/testcases'),
            ApiClient.get('/execution/batches'),
            ApiClient.get('/execution/executions'),
            ApiClient.get('/schedules/active')
        ]);

        return {
            totalTestCases: testCases.status === 'fulfilled' ? (testCases.value?.length || 0) : 0,
            totalBatches: batches.status === 'fulfilled' ? (batches.value?.length || 0) : 0,
            totalExecutions: executions.status === 'fulfilled' ? (executions.value?.length || 0) : 0,
            activeSchedules: schedules.status === 'fulfilled' ? (schedules.value?.length || 0) : 0,
            successRate: calculateSuccessRate(executions.status === 'fulfilled' ? executions.value : []),
            testsToday: getTodayExecutions(executions.status === 'fulfilled' ? executions.value : [])
        };
    } catch (error) {
        console.warn('Fallback metrics also failed, using demo data');
        return {
            totalTestCases: 12,
            totalBatches: 8,
            totalExecutions: 45,
            activeSchedules: 3,
            successRate: 85.5,
            testsToday: 15
        };
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
            element.innerHTML = '<div class="skeleton w-8 h-8 rounded"></div>';
        }
    });

    // Show loading for activity lists
    const recentBatchesList = document.getElementById('recent-batches-list');
    if (recentBatchesList) {
        recentBatchesList.innerHTML = `
            <div class="skeleton w-full h-16 rounded-lg mb-3"></div>
            <div class="skeleton w-full h-16 rounded-lg mb-3"></div>
            <div class="skeleton w-full h-16 rounded-lg"></div>
        `;
    }

    const systemHealthDetails = document.getElementById('system-health-details');
    if (systemHealthDetails) {
        systemHealthDetails.innerHTML = `
            <div class="skeleton w-full h-6 rounded mb-2"></div>
            <div class="skeleton w-full h-6 rounded mb-2"></div>
            <div class="skeleton w-full h-6 rounded"></div>
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
            element.className = `text-3xl font-bold ${color} animate-counter`;
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

    // Update recent batches list
    const recentBatchesList = document.getElementById('recent-batches-list');
    if (recentBatchesList) {
        if (!activity.recentBatches || activity.recentBatches.length === 0) {
            recentBatchesList.innerHTML = `
                <div class="text-center py-8 text-gray-400">
                    <i class="fas fa-layer-group text-2xl mb-2"></i>
                    <p>No recent batches</p>
                    <button onclick="switchSection('execution')" class="text-cyan-400 hover:underline text-sm">
                        Start a new batch execution
                    </button>
                </div>
            `;
        } else {
            recentBatchesList.innerHTML = '';
            activity.recentBatches.slice(0, 5).forEach(batch => {
                const item = document.createElement('div');
                item.className = 'flex items-center justify-between p-3 glass-effect rounded-lg border border-white/10 hover:bg-white/5 transition-colors cursor-pointer';
                item.onclick = () => viewBatchDetails(batch.batchId);
                item.innerHTML = `
                    <div class="flex items-center space-x-3">
                        <div class="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center">
                            <i class="fas fa-layer-group text-blue-400 text-xs"></i>
                        </div>
                        <div>
                            <div class="font-medium text-white text-sm">${batch.batchId}</div>
                            <div class="text-xs text-gray-400">${batch.testSuite || 'Unknown'} • ${formatDateTime(batch.createdAt)}</div>
                        </div>
                    </div>
                    <span class="status-badge status-${batch.status?.toLowerCase() || 'unknown'}">${batch.status || 'UNKNOWN'}</span>
                `;
                recentBatchesList.appendChild(item);
            });
        }
    }

    // Update recent activity list (combined feed)
    const recentActivityList = document.getElementById('recent-activity-list');
    if (recentActivityList && activity.recentActivity) {
        updateRecentActivityFeed(activity.recentActivity);
    } else if (recentActivityList) {
        // Create activity feed from batches and executions
        const combinedActivity = createActivityFeed(activity.recentBatches);
        updateRecentActivityFeed(combinedActivity);
    }

    // Update system health details
    updateSystemHealthDisplay(activity.systemHealth);
}

function createActivityFeed(recentBatches) {
    const activities = [];

    if (recentBatches) {
        recentBatches.forEach(batch => {
            activities.push({
                type: 'batch',
                title: `Batch ${batch.batchId}`,
                description: `${batch.status} - ${batch.testSuite || 'Unknown suite'}`,
                timestamp: batch.createdAt,
                status: batch.status,
                icon: 'fa-layer-group'
            });
        });
    }

    // Sort by timestamp
    return activities.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
}

function updateRecentActivityFeed(activities) {
    const recentActivityList = document.getElementById('recent-activity-list');
    if (!recentActivityList) return;

    if (!activities || activities.length === 0) {
        recentActivityList.innerHTML = `
            <div class="text-center py-8 text-gray-400">
                <i class="fas fa-clock text-2xl mb-2"></i>
                <p>No recent activity</p>
                <p class="text-sm">Activity will appear here as you use the system</p>
            </div>
        `;
        return;
    }

    recentActivityList.innerHTML = '';
    activities.slice(0, 10).forEach(activity => {
        const item = document.createElement('div');
        item.className = 'flex items-start space-x-3 p-3 hover:bg-white/5 rounded-lg transition-colors';

        const statusColor = getActivityStatusColor(activity.status);
        item.innerHTML = `
            <div class="flex-shrink-0 w-8 h-8 rounded-full ${statusColor} flex items-center justify-center">
                <i class="fas ${activity.icon} text-xs"></i>
            </div>
            <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-white">${activity.title}</div>
                <div class="text-xs text-gray-400">${activity.description}</div>
                <div class="text-xs text-gray-500 mt-1">${formatDateTime(activity.timestamp)}</div>
            </div>
            <div class="flex-shrink-0">
                <span class="status-badge status-${activity.status?.toLowerCase() || 'unknown'}">${activity.status || 'UNKNOWN'}</span>
            </div>
        `;
        recentActivityList.appendChild(item);
    });
}

function getActivityStatusColor(status) {
    const colors = {
        'COMPLETED': 'bg-green-500/20 text-green-400',
        'RUNNING': 'bg-blue-500/20 text-blue-400',
        'FAILED': 'bg-red-500/20 text-red-400',
        'CANCELLED': 'bg-yellow-500/20 text-yellow-400',
        'SCHEDULED': 'bg-purple-500/20 text-purple-400'
    };
    return colors[status] || 'bg-gray-500/20 text-gray-400';
}

async function loadSystemHealth() {
    try {
        // Try to get system health from validation endpoint
        const healthData = await ApiClient.get('/validation/health').catch(e => {
            console.warn('Health check endpoint not available');
            return 'System operational';
        });

        // Update performance stats in header
        updatePerformanceStats();

        return healthData;
    } catch (error) {
        console.warn('System health check failed:', error);
        return null;
    }
}

function updateSystemHealthDisplay(systemHealth) {
    const systemHealthDetails = document.getElementById('system-health-details');
    if (!systemHealthDetails) return;

    const healthData = systemHealth || getDefaultSystemHealth();

    systemHealthDetails.innerHTML = '';
    const healthItems = [
        {
            label: 'API Server',
            value: healthData.apiStatus || 'OK',
            icon: 'fa-server',
            status: (healthData.apiStatus === 'OK' || !healthData.apiStatus) ? 'success' : 'error'
        },
        {
            label: 'Database',
            value: healthData.databaseStatus || 'Connected',
            icon: 'fa-database',
            status: (healthData.databaseStatus === 'Connected' || !healthData.databaseStatus) ? 'success' : 'error'
        },
        {
            label: 'Test Queue',
            value: healthData.queueStatus || 'Active',
            icon: 'fa-tasks',
            status: (healthData.queueStatus === 'Active' || !healthData.queueStatus) ? 'success' : 'warning'
        },
        {
            label: 'Memory Usage',
            value: healthData.memoryUsage || '45%',
            icon: 'fa-memory',
            status: parseFloat(healthData.memoryUsage || '45') < 80 ? 'success' : 'warning'
        }
    ];

    healthItems.forEach(item => {
        const healthItem = document.createElement('div');
        healthItem.className = 'flex items-center justify-between p-2 hover:bg-white/5 rounded transition-colors';

        const statusColor = {
            'success': 'text-green-400',
            'warning': 'text-yellow-400',
            'error': 'text-red-400'
        }[item.status] || 'text-gray-400';

        healthItem.innerHTML = `
            <div class="flex items-center space-x-2">
                <i class="fas ${item.icon} text-gray-400"></i>
                <span class="text-white text-sm">${item.label}</span>
            </div>
            <span class="${statusColor} text-sm font-medium">${item.value}</span>
        `;
        systemHealthDetails.appendChild(healthItem);
    });
}

function getDefaultSystemHealth() {
    return {
        apiStatus: 'OK',
        databaseStatus: 'Connected',
        queueStatus: 'Active',
        memoryUsage: '45%'
    };
}

async function updatePerformanceStats() {
    // Simulate performance measurements
    const performanceStats = {
        responseTime: Math.floor(Math.random() * 100) + 50, // 50-150ms
        activeConnections: Math.floor(Math.random() * 10) + 5, // 5-15
        memoryUsage: Math.floor(Math.random() * 30) + 40, // 40-70%
        queueLength: Math.floor(Math.random() * 5) // 0-5
    };

    const statElements = [
        { id: 'response-time', value: `${performanceStats.responseTime}ms` },
        { id: 'active-connections', value: performanceStats.activeConnections },
        { id: 'memory-usage', value: `${performanceStats.memoryUsage}%` },
        { id: 'queue-length', value: performanceStats.queueLength }
    ];

    statElements.forEach(({ id, value }) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    });
}

function animateCounter(element, start, end, duration) {
    const startTime = performance.now();
    const change = end - start;

    function updateCounter(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Easing function
        const easeOut = 1 - Math.pow(1 - progress, 3);
        const current = Math.floor(start + change * easeOut);

        element.textContent = current;

        if (progress < 1) {
            requestAnimationFrame(updateCounter);
        } else {
            element.textContent = end;
        }
    }

    requestAnimationFrame(updateCounter);
}

async function loadFallbackDashboardData() {
    console.log('Loading fallback dashboard data...');

    // Use demo data as fallback
    const fallbackMetrics = {
        totalTestCases: 25,
        totalBatches: 12,
        totalExecutions: 89,
        activeSchedules: 4,
        successRate: 87.5,
        testsToday: 18,
        newTestCasesThisWeek: 3,
        newBatchesToday: 2,
        runningExecutions: 1,
        nextSchedule: '2h 30m',
        avgDuration: 45000,
        queueSize: 2
    };

    const fallbackActivity = {
        recentBatches: [
            {
                batchId: 'batch-001',
                status: 'COMPLETED',
                testSuite: 'regression-suite',
                createdAt: new Date().toISOString()
            },
            {
                batchId: 'batch-002',
                status: 'RUNNING',
                testSuite: 'smoke-tests',
                createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString()
            }
        ],
        systemHealth: getDefaultSystemHealth()
    };

    updateDashboardMetrics(fallbackMetrics);
    updateRecentActivity(fallbackActivity);

    notificationManager.show('Using demo data - check your API connection', 'info');
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
