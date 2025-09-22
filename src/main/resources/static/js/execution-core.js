// Multiple Test Execution Core Module
// This module handles single test execution, batch execution, and real-time monitoring

// Global execution monitoring state
let executionMonitors = new Map();
let isMonitoringPaused = false;

// Handle Single Test Execution Form Submission
async function handleSingleTestExecution(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const testCaseId = formData.get('testCaseId') || document.getElementById('single-test-select')?.value;
    const environment = formData.get('environment') || document.getElementById('single-test-env')?.value;
    const browser = formData.get('browser') || document.getElementById('single-test-browser')?.value;
    const debugMode = document.getElementById('single-test-debug')?.checked;

    if (!testCaseId) {
        notificationManager.show('Please select a test case', 'error');
        return;
    }

    try {
        loadingManager.show('Starting single test execution...');

        console.log('Executing single test:', {
            testCaseId: parseInt(testCaseId),
            environment: environment,
            browser: browser,
            debugMode: debugMode
        });

        const result = await ApiClient.post(`/execution/single/${testCaseId}`, null, {
            params: { environment: environment }
        });

        notificationManager.show(
            `🚀 Test execution started successfully!\nExecution ID: ${result.executionId || result.id}\nTest Case: ${testCaseId}\nEnvironment: ${environment}`,
            'success',
            { duration: 6000 }
        );

        // Start monitoring this execution
        if (result.executionId || result.id) {
            startSingleExecutionMonitoring(result.executionId || result.id);
        }

        // Update UI
        await updateExecutionStats();

        // Refresh displays after a short delay
        setTimeout(async () => {
            if (currentSection === 'execution') {
                await loadExecutions();
            }
            await loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing single test:', error);
        notificationManager.show(`❌ Failed to execute test: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Handle Batch Test Execution Form Submission
async function handleBatchTestExecution(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const testSuite = formData.get('testSuite') || document.getElementById('batch-test-suite')?.value;
    const environment = formData.get('environment') || document.getElementById('batch-env')?.value;
    const parallelThreads = parseInt(formData.get('parallelThreads') || document.getElementById('batch-threads-slider')?.value || 1);
    const priority = formData.get('priority') || document.getElementById('batch-priority')?.value || 'normal';
    const stopOnFailure = document.getElementById('batch-stop-on-failure')?.checked;

    if (!testSuite) {
        notificationManager.show('Please enter a test suite name', 'error');
        return;
    }

    try {
        loadingManager.show('Starting batch test execution...');

        const batchRequest = {
            testSuite: testSuite,
            environment: environment,
            parallelThreads: parallelThreads,
            priority: priority,
            stopOnFailure: stopOnFailure
        };

        console.log('Executing batch tests:', batchRequest);

        const result = await ApiClient.post('/execution/batch', batchRequest);

        notificationManager.show(
            `🚀 Batch execution started successfully!\nBatch ID: ${result.batchId}\nTest Suite: ${testSuite}\nParallel Threads: ${parallelThreads}\nEnvironment: ${environment}`,
            'success',
            { duration: 8000 }
        );

        // Start monitoring this batch execution
        if (result.batchId) {
            startBatchExecutionMonitoring(result.batchId);
        }

        // Clear form after successful submission
        event.target.reset();
        const threadsValue = document.getElementById('batch-threads-value');
        if (threadsValue) threadsValue.textContent = '1';

        // Update execution statistics
        await updateExecutionStats();

        // Refresh displays after a short delay
        setTimeout(async () => {
            if (currentSection === 'execution' || currentSection === 'batches') {
                await loadExecutions();
                await loadBatches();
            }
            await loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing batch tests:', error);
        notificationManager.show(`❌ Failed to execute batch: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Handle Stop All Executions
async function handleStopAllExecutions() {
    const confirmed = confirm('Are you sure you want to stop all running executions?\n\nThis will cancel all active batch executions and cannot be undone.');

    if (!confirmed) return;

    try {
        loadingManager.show('Stopping all executions...');

        // Get all active batches and stop them
        const activeBatches = await ApiClient.get('/execution/batches/active');

        if (activeBatches && activeBatches.length > 0) {
            const stopPromises = activeBatches.map(batch =>
                ApiClient.post(`/execution/batch/${batch.batchId}/cancel`)
                    .catch(error => {
                        console.warn(`Failed to stop batch ${batch.batchId}:`, error);
                        return { error: true, batchId: batch.batchId };
                    })
            );

            const results = await Promise.allSettled(stopPromises);
            const successCount = results.filter(r => r.status === 'fulfilled' && !r.value.error).length;

            notificationManager.show(
                `✅ Stop command sent to ${activeBatches.length} execution(s)\n${successCount} stopped successfully`,
                'success'
            );
        } else {
            notificationManager.show('No active executions found to stop', 'info');
        }

        // Clear all execution monitors
        executionMonitors.forEach((interval, key) => {
            clearInterval(interval);
        });
        executionMonitors.clear();

        // Refresh displays
        setTimeout(async () => {
            if (currentSection === 'execution' || currentSection === 'batches') {
                await loadExecutions();
                await loadBatches();
            }
            await loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error stopping executions:', error);
        notificationManager.show(`❌ Failed to stop executions: ${error.message}`, 'error');
    } finally {
        loadingManager.hide();
    }
}

// Handle Toggle Execution Monitoring
function handleToggleMonitoring() {
    const btn = document.getElementById('pause-monitoring-btn');
    const icon = btn?.querySelector('i');

    if (isMonitoringPaused) {
        // Resume monitoring
        isMonitoringPaused = false;
        if (icon) icon.className = 'fas fa-pause mr-1';
        btn.innerHTML = '<i class="fas fa-pause mr-1"></i>Pause';

        // Restart monitoring for all active executions
        restartAllMonitoring();

        notificationManager.show('✅ Execution monitoring resumed', 'success');
    } else {
        // Pause monitoring
        isMonitoringPaused = true;
        if (icon) icon.className = 'fas fa-play mr-1';
        btn.innerHTML = '<i class="fas fa-play mr-1"></i>Resume';

        // Pause all monitoring intervals
        pauseAllMonitoring();

        notificationManager.show('⏸️ Execution monitoring paused', 'info');
    }
}

// Start Single Execution Monitoring
function startSingleExecutionMonitoring(executionId) {
    if (executionMonitors.has(`single-${executionId}`) || isMonitoringPaused) return;

    const pollInterval = setInterval(async () => {
        if (isMonitoringPaused) return;

        try {
            const execution = await ApiClient.get(`/execution/executions/${executionId}`);

            if (execution.status === 'COMPLETED' || execution.status === 'FAILED' || execution.status === 'PASSED') {
                clearInterval(pollInterval);
                executionMonitors.delete(`single-${executionId}`);

                const isSuccess = execution.status === 'COMPLETED' || execution.status === 'PASSED';
                notificationManager.show(
                    `Single test execution ${isSuccess ? 'completed successfully' : 'failed'}\nExecution ID: ${executionId}\nStatus: ${execution.status}\nDuration: ${formatDuration(execution.duration || 0)}`,
                    isSuccess ? 'success' : 'error',
                    { duration: 8000 }
                );

                // Update displays
                await updateExecutionStats();
                if (currentSection === 'execution') {
                    await loadExecutions();
                }
            }
        } catch (error) {
            console.error('Error monitoring single execution:', error);
            clearInterval(pollInterval);
            executionMonitors.delete(`single-${executionId}`);
        }
    }, 3000); // Check every 3 seconds for single executions

    executionMonitors.set(`single-${executionId}`, pollInterval);
}

// Start Batch Execution Monitoring
function startBatchExecutionMonitoring(batchId) {
    if (executionMonitors.has(batchId) || isMonitoringPaused) return;

    const pollInterval = setInterval(async () => {
        if (isMonitoringPaused) return;

        try {
            const batchProgress = await ApiClient.get(`/execution/batch/${batchId}/progress`);

            // Update progress display
            updateBatchProgressDisplay(batchProgress);

            if (batchProgress.status === 'COMPLETED' || batchProgress.status === 'FAILED' || batchProgress.status === 'CANCELLED') {
                clearInterval(pollInterval);
                executionMonitors.delete(batchId);

                const isSuccess = batchProgress.status === 'COMPLETED';
                const icon = isSuccess ? '✅' : batchProgress.status === 'CANCELLED' ? '⏹️' : '❌';

                notificationManager.show(
                    `${icon} Batch execution ${batchProgress.status.toLowerCase()}\nBatch ID: ${batchId}\nTotal Tests: ${batchProgress.totalTests || 0}\nPassed: ${batchProgress.passedTests || 0}\nFailed: ${batchProgress.failedTests || 0}\nDuration: ${formatDuration(batchProgress.duration || 0)}`,
                    isSuccess ? 'success' : batchProgress.status === 'CANCELLED' ? 'warning' : 'error',
                    { duration: 12000 }
                );

                // Remove from current executions display
                removeBatchFromCurrentExecutions(batchId);

                // Update displays
                await updateExecutionStats();
                if (currentSection === 'execution' || currentSection === 'batches') {
                    await loadExecutions();
                    await loadBatches();
                }
                await loadDashboardData();
            }
        } catch (error) {
            console.error('Error monitoring batch execution:', error);
            clearInterval(pollInterval);
            executionMonitors.delete(batchId);
        }
    }, 5000); // Check every 5 seconds for batch executions

    executionMonitors.set(batchId, pollInterval);
}

// Update Batch Progress Display
function updateBatchProgressDisplay(batchProgress) {
    if (!batchProgress) return;

    // Update timeline progress
    const timelineProgress = document.getElementById('execution-timeline-progress');
    if (timelineProgress) {
        const percentage = Math.min(batchProgress.progressPercentage || 0, 100);
        timelineProgress.style.width = `${percentage}%`;
    }

    // Update timeline duration
    const timelineDuration = document.getElementById('timeline-duration');
    if (timelineDuration) {
        const duration = batchProgress.duration || 0;
        timelineDuration.textContent = formatDuration(duration);
    }

    // Update current executions display with batch info
    const currentExecutions = document.getElementById('current-executions');
    if (currentExecutions && batchProgress.status !== 'COMPLETED') {
        const existingBatchDiv = currentExecutions.querySelector(`[data-batch-id="${batchProgress.batchId}"]`);

        const batchProgressDiv = existingBatchDiv || document.createElement('div');
        if (!existingBatchDiv) {
            batchProgressDiv.setAttribute('data-batch-id', batchProgress.batchId);
            batchProgressDiv.className = 'execution-monitor-item flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10 mb-3';
        }

        const statusColor = getExecutionStatusColor(batchProgress.status);
        const progressPercentage = Math.round(batchProgress.progressPercentage || 0);

        batchProgressDiv.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center">
                    <i class="fas fa-layer-group text-blue-400"></i>
                </div>
                <div>
                    <div class="font-medium text-white">Batch: ${batchProgress.batchId}</div>
                    <div class="text-sm text-gray-400">
                        ${batchProgress.completedTests || 0}/${batchProgress.totalTests || 0} tests completed
                        ${batchProgress.parallelThreads ? ` • ${batchProgress.parallelThreads} threads` : ''}
                    </div>
                    <div class="text-xs text-gray-500 mt-1">
                        Started: ${formatDateTime(batchProgress.startTime)}
                    </div>
                </div>
            </div>
            <div class="text-right">
                <div class="text-sm ${statusColor} font-semibold mb-1">${batchProgress.status}</div>
                <div class="text-xs text-gray-400 mb-2">
                    ${progressPercentage}% complete
                </div>
                <div class="w-20 bg-gray-700 rounded-full h-2">
                    <div class="bg-blue-400 h-2 rounded-full transition-all duration-300" style="width: ${progressPercentage}%"></div>
                </div>
            </div>
        `;

        if (!existingBatchDiv) {
            // Remove empty state if it exists
            const emptyState = currentExecutions.querySelector('.empty-state');
            if (emptyState) {
                emptyState.remove();
            }
            currentExecutions.appendChild(batchProgressDiv);
        }
    }
}

// Remove batch from current executions display
function removeBatchFromCurrentExecutions(batchId) {
    const currentExecutions = document.getElementById('current-executions');
    if (currentExecutions) {
        const batchDiv = currentExecutions.querySelector(`[data-batch-id="${batchId}"]`);
        if (batchDiv) {
            batchDiv.remove();

            // If no more executions, show empty state
            const remainingExecutions = currentExecutions.querySelectorAll('.execution-monitor-item');
            if (remainingExecutions.length === 0) {
                currentExecutions.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">
                            <i class="fas fa-play-circle"></i>
                        </div>
                        <div class="empty-state-title">No Active Executions</div>
                        <div class="empty-state-description">Start a test execution to see real-time progress here</div>
                    </div>
                `;
            }
        }
    }
}

// Update Execution Statistics
async function updateExecutionStats() {
    try {
        // Get current execution statistics
        const [executions, batches] = await Promise.allSettled([
            ApiClient.get('/execution/executions').catch(() => []),
            ApiClient.get('/execution/batches/active').catch(() => [])
        ]);

        const executionsData = executions.status === 'fulfilled' ? executions.value : [];
        const batchesData = batches.status === 'fulfilled' ? batches.value : [];

        // Calculate statistics
        const stats = {
            running: countByStatus(executionsData, 'RUNNING') + countByStatus(batchesData, 'RUNNING'),
            queued: countByStatus(executionsData, 'SCHEDULED') + countByStatus(batchesData, 'SCHEDULED'),
            completed: countByStatus(executionsData.filter(e => isToday(e.completedAt)), 'COMPLETED'),
            failed: countByStatus(executionsData.filter(e => isToday(e.completedAt)), 'FAILED')
        };

        // Update UI elements with animation
        const statElements = [
            { id: 'executions-running', value: stats.running },
            { id: 'executions-queued', value: stats.queued },
            { id: 'executions-completed', value: stats.completed },
            { id: 'executions-failed', value: stats.failed }
        ];

        statElements.forEach(({ id, value }) => {
            const element = document.getElementById(id);
            if (element) {
                const currentValue = parseInt(element.textContent) || 0;
                if (currentValue !== value) {
                    animateCounter(element, currentValue, value, 500);
                }
            }
        });

    } catch (error) {
        console.error('Error updating execution stats:', error);
    }
}

// Utility Functions
function getExecutionStatusColor(status) {
    const colors = {
        'RUNNING': 'text-blue-400',
        'COMPLETED': 'text-green-400',
        'FAILED': 'text-red-400',
        'CANCELLED': 'text-yellow-400',
        'SCHEDULED': 'text-gray-400'
    };
    return colors[status] || 'text-gray-400';
}

function countByStatus(items, status) {
    if (!Array.isArray(items)) return 0;
    return items.filter(item => item.status === status).length;
}

function isToday(dateString) {
    if (!dateString) return false;
    const today = new Date().toDateString();
    return new Date(dateString).toDateString() === today;
}

function formatDuration(milliseconds) {
    if (!milliseconds || milliseconds < 0) return '00:00:00';

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    const h = hours.toString().padStart(2, '0');
    const m = (minutes % 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');

    return `${h}:${m}:${s}`;
}

function formatDateTime(dateString) {
    if (!dateString) return '--';

    try {
        const date = new Date(dateString);
        return date.toLocaleTimeString('en-US', {
            hour12: false,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch (error) {
        return '--';
    }
}

// Monitoring Control Functions
function pauseAllMonitoring() {
    executionMonitors.forEach((interval, key) => {
        clearInterval(interval);
    });
}

function restartAllMonitoring() {
    // This would need to track which executions were being monitored
    // For now, we'll rely on the next refresh to pick up active executions
    setTimeout(async () => {
        if (currentSection === 'execution') {
            await loadExecutions();
        }
    }, 1000);
}

// Cleanup function
function cleanupExecutionMonitoring() {
    executionMonitors.forEach((interval, key) => {
        clearInterval(interval);
    });
    executionMonitors.clear();
}

// Export functions for global access
window.handleSingleTestExecution = handleSingleTestExecution;
window.handleBatchTestExecution = handleBatchTestExecution;
window.handleStopAllExecutions = handleStopAllExecutions;
window.handleToggleMonitoring = handleToggleMonitoring;
window.updateExecutionStats = updateExecutionStats;
window.startBatchExecutionMonitoring = startBatchExecutionMonitoring;
window.startSingleExecutionMonitoring = startSingleExecutionMonitoring;
window.cleanupExecutionMonitoring = cleanupExecutionMonitoring;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('Multiple Test Execution module loaded');
});

// Cleanup on page unload
window.addEventListener('beforeunload', cleanupExecutionMonitoring);
