// Dashboard JavaScript with animations and interactivity

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initDashboard();
    loadDashboardData();
    initCharts();
    animateCounters();
});

// Initialize dashboard
function initDashboard() {
    console.log('Dashboard initialized');

    // Add active state to current nav link
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // Add intersection observer for scroll animations
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.card').forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(card);
    });
}

// Load dashboard data
async function loadDashboardData() {
    try {
        // Simulate API call - replace with actual endpoint
        const data = await fetchDashboardStats();
        updateDashboardStats(data);
        loadRecentExecutions();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showNotification('Failed to load dashboard data', 'danger');
    }
}

// Fetch dashboard statistics
async function fetchDashboardStats() {
    // Simulate API call with mock data
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                totalTests: 247,
                passedTests: 210,
                failedTests: 25,
                skippedTests: 12,
                avgDuration: 145.5
            });
        }, 500);
    });
}

// Update dashboard statistics with animation
function updateDashboardStats(data) {
    const total = data.totalTests;
    const passed = data.passedTests;
    const failed = data.failedTests;
    const skipped = data.skippedTests;
    const successRate = total > 0 ? ((passed / total) * 100).toFixed(1) : 0;

    // Animate counters
    animateCounter('totalTests', total);
    animateCounter('passedTests', passed);
    animateCounter('failedTests', failed);
    animateCounter('skippedTests', skipped);
    animateValue('successRate', successRate, '%');
    animateValue('avgDuration', data.avgDuration.toFixed(1), 's');

    // Update progress bars with animation
    setTimeout(() => {
        updateProgressBar('passedProgress', (passed / total) * 100);
        updateProgressBar('failedProgress', (failed / total) * 100);
        updateProgressBar('skippedProgress', (skipped / total) * 100);
    }, 300);
}

// Animate counter values
function animateCounter(elementId, targetValue, duration = 1000) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const startValue = 0;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Easing function for smooth animation
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const currentValue = Math.floor(startValue + (targetValue - startValue) * easeOutQuart);

        element.textContent = currentValue;

        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            element.textContent = targetValue;
        }
    }

    requestAnimationFrame(update);
}

// Animate value with suffix
function animateValue(elementId, targetValue, suffix = '') {
    const element = document.getElementById(elementId);
    if (!element) return;

    const numericValue = parseFloat(targetValue);
    const startValue = 0;
    const duration = 1000;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const currentValue = (startValue + (numericValue - startValue) * easeOutQuart).toFixed(1);

        element.textContent = currentValue + suffix;

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

// Update progress bar with animation
function updateProgressBar(elementId, percentage) {
    const progressBar = document.getElementById(elementId);
    if (!progressBar) return;

    progressBar.style.width = percentage + '%';
}

// Load recent test executions
async function loadRecentExecutions() {
    try {
        // Simulate API call - replace with actual endpoint
        const executions = await fetchRecentExecutions();
        renderRecentExecutions(executions);
    } catch (error) {
        console.error('Error loading recent executions:', error);
        document.getElementById('recentExecutions').innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-muted">
                    Failed to load recent executions
                </td>
            </tr>
        `;
    }
}

// Fetch recent executions
async function fetchRecentExecutions() {
    // Simulate API call with mock data
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                {
                    id: 'EX-2025-001',
                    suite: 'API Test Suite',
                    status: 'passed',
                    total: 45,
                    passed: 43,
                    failed: 2,
                    passRate: 95.6,
                    duration: '2m 34s',
                    date: '2025-10-05 14:30'
                },
                {
                    id: 'EX-2025-002',
                    suite: 'UI Test Suite',
                    status: 'failed',
                    total: 32,
                    passed: 28,
                    failed: 4,
                    passRate: 87.5,
                    duration: '5m 12s',
                    date: '2025-10-05 13:15'
                },
                {
                    id: 'EX-2025-003',
                    suite: 'Integration Tests',
                    status: 'passed',
                    total: 28,
                    passed: 28,
                    failed: 0,
                    passRate: 100,
                    duration: '3m 45s',
                    date: '2025-10-05 11:20'
                },
                {
                    id: 'EX-2025-004',
                    suite: 'Regression Suite',
                    status: 'passed',
                    total: 67,
                    passed: 65,
                    failed: 2,
                    passRate: 97.0,
                    duration: '8m 22s',
                    date: '2025-10-04 18:45'
                },
                {
                    id: 'EX-2025-005',
                    suite: 'Smoke Test Suite',
                    status: 'passed',
                    total: 15,
                    passed: 15,
                    failed: 0,
                    passRate: 100,
                    duration: '1m 18s',
                    date: '2025-10-04 16:30'
                }
            ]);
        }, 800);
    });
}

// Render recent executions in table
function renderRecentExecutions(executions) {
    const tbody = document.getElementById('recentExecutions');
    if (!tbody) return;

    if (executions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-muted">No recent executions found</td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = executions.map((exec, index) => `
        <tr style="animation: fadeIn 0.5s ease ${index * 0.1}s both;">
            <td><strong>${exec.id}</strong></td>
            <td>${exec.suite}</td>
            <td><span class="badge badge-${exec.status === 'passed' ? 'success' : 'danger'}">${exec.status}</span></td>
            <td>${exec.passed}/${exec.total}</td>
            <td>
                <div class="d-flex align-items-center gap-1">
                    <span>${exec.passRate}%</span>
                    <div class="progress" style="width: 60px; height: 6px;">
                        <div class="progress-bar ${exec.passRate >= 90 ? 'success' : exec.passRate >= 70 ? 'warning' : 'danger'}" 
                             style="width: ${exec.passRate}%"></div>
                    </div>
                </div>
            </td>
            <td>${exec.duration}</td>
            <td>${exec.date}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="viewExecutionReport('${exec.id}')">
                    View Report
                </button>
            </td>
        </tr>
    `).join('');
}

// Initialize charts
let resultsChart, trendChart;

function initCharts() {
    initResultsChart();
    initTrendChart();
}

// Initialize results distribution chart
function initResultsChart() {
    const ctx = document.getElementById('resultsChart');
    if (!ctx) return;

    resultsChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Passed', 'Failed', 'Skipped'],
            datasets: [{
                data: [210, 25, 12],
                backgroundColor: [
                    'rgba(16, 185, 129, 0.8)',
                    'rgba(239, 68, 68, 0.8)',
                    'rgba(245, 158, 11, 0.8)'
                ],
                borderColor: [
                    'rgb(16, 185, 129)',
                    'rgb(239, 68, 68)',
                    'rgb(245, 158, 11)'
                ],
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        font: {
                            size: 12,
                            family: 'Inter'
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

// Initialize trend chart
function initTrendChart() {
    const ctx = document.getElementById('trendChart');
    if (!ctx) return;

    const labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    trendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Passed',
                    data: [42, 48, 45, 52, 49, 55, 58],
                    borderColor: 'rgb(16, 185, 129)',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    tension: 0.4,
                    fill: true
                },
                {
                    label: 'Failed',
                    data: [5, 3, 6, 4, 5, 3, 2],
                    borderColor: 'rgb(239, 68, 68)',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    tension: 0.4,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        font: {
                            size: 12,
                            family: 'Inter'
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            },
            animation: {
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

// Animate counters on scroll
function animateCounters() {
    const counters = document.querySelectorAll('.stat-value');

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting && !entry.target.classList.contains('animated')) {
                entry.target.classList.add('animated');
                // Counter animation is already handled by updateDashboardStats
            }
        });
    }, { threshold: 0.5 });

    counters.forEach(counter => observer.observe(counter));
}

// View execution report
function viewExecutionReport(executionId) {
    window.location.href = `/dashboard/execution-report?id=${executionId}`;
}

// Show coverage modal
function showCoverageModal() {
    const modal = document.getElementById('coverageModal');
    if (modal) {
        modal.classList.add('show');
    }
}

// Close coverage modal
function closeCoverageModal() {
    const modal = document.getElementById('coverageModal');
    if (modal) {
        modal.classList.remove('show');
    }
}

// Close modal on outside click
window.addEventListener('click', function(event) {
    const modal = document.getElementById('coverageModal');
    if (event.target === modal) {
        closeCoverageModal();
    }
});

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
        alert.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => alert.remove(), 300);
    }, 3000);
}

// Get alert icon based on type
function getAlertIcon(type) {
    const icons = {
        success: '✓',
        danger: '✗',
        warning: '⚠',
        info: 'ℹ'
    };
    return icons[type] || icons.info;
}

// Handle chart period change
const chartPeriodSelect = document.getElementById('chartPeriod');
if (chartPeriodSelect) {
    chartPeriodSelect.addEventListener('change', function(e) {
        const period = e.target.value;
        updateChartData(period);
    });
}

// Update chart data based on period
function updateChartData(period) {
    // Simulate updating chart data
    showNotification(`Chart updated for last ${period} days`, 'success');

    // In a real application, you would fetch new data and update the chart
    if (resultsChart) {
        resultsChart.data.datasets[0].data = [
            Math.floor(Math.random() * 200) + 100,
            Math.floor(Math.random() * 50) + 10,
            Math.floor(Math.random() * 30) + 5
        ];
        resultsChart.update('active');
    }
}

// Refresh dashboard data periodically
setInterval(() => {
    loadDashboardData();
}, 60000); // Refresh every minute

// Add CSS animation for fadeOut
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeOut {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(20px);
        }
    }
`;
document.head.appendChild(style);

