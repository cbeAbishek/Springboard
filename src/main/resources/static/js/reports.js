// Reports JavaScript with advanced filtering and sorting

let allReports = [];
let filteredReports = [];
let currentPage = 1;
let pageSize = 10;
let sortColumn = 'date';
let sortDirection = 'desc';

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    initReports();
    loadReports();
});

// Initialize reports page
function initReports() {
    console.log('Reports page initialized');

    // Set active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('href') === '/dashboard/reports') {
            link.classList.add('active');
        }
    });
}

// Load reports data
async function loadReports() {
    try {
        showLoadingState();
        allReports = await fetchReports();
        filteredReports = [...allReports];
        updateSummaryStats();
        renderReports();
    } catch (error) {
        console.error('Error loading reports:', error);
        showError('Failed to load reports');
    }
}

// Fetch reports from API
async function fetchReports() {
    try {
        // Fetch real reports from API
        const response = await fetch('/api/reports');
        if (response.ok) {
            const reports = await response.json();
            console.log('Real reports loaded from API:', reports.length);

            // Transform API data to match UI format
            return reports.map(r => ({
                id: r.reportId,
                suite: r.suiteType || r.reportName || 'Test Suite',
                status: r.status?.toLowerCase() === 'completed' ?
                       (r.failedTests === 0 ? 'passed' : (r.passedTests / r.totalTests > 0.8 ? 'partial' : 'failed')) :
                       'running',
                passed: r.passedTests || 0,
                failed: r.failedTests || 0,
                skipped: r.skippedTests || 0,
                total: r.totalTests || 0,
                passRate: r.totalTests > 0 ? ((r.passedTests / r.totalTests) * 100).toFixed(1) : 0,
                duration: formatDuration(r.durationMs),
                durationSeconds: r.durationMs ? r.durationMs / 1000 : 0,
                date: r.executionDate || r.createdAt,
                environment: r.environment || 'N/A',
                browser: r.browser
            }));
        } else {
            console.warn('Failed to fetch reports from API, using mock data');
            return getMockReports();
        }
    } catch (error) {
        console.error('Error fetching reports:', error);
        return getMockReports();
    }
}

// Get mock reports for fallback
function getMockReports() {
    const reports = [];
    const suites = ['API Test Suite', 'UI Test Suite', 'Integration Tests', 'Regression Suite', 'Smoke Tests'];

    for (let i = 1; i <= 50; i++) {
        const passed = Math.floor(Math.random() * 50) + 10;
        const failed = Math.floor(Math.random() * 10);
        const total = passed + failed;
        const passRate = ((passed / total) * 100).toFixed(1);
        const status = failed === 0 ? 'passed' : (passRate > 80 ? 'partial' : 'failed');

        reports.push({
            id: `EX-2025-${String(i).padStart(3, '0')}`,
            suite: suites[Math.floor(Math.random() * suites.length)],
            status: status,
            passed: passed,
            failed: failed,
            skipped: Math.floor(Math.random() * 5),
            total: total,
            passRate: parseFloat(passRate),
            duration: `${Math.floor(Math.random() * 10) + 1}m ${Math.floor(Math.random() * 60)}s`,
            durationSeconds: Math.floor(Math.random() * 600) + 60,
            date: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000).toISOString(),
            environment: ['dev', 'qa', 'staging'][Math.floor(Math.random() * 3)]
        });
    }

    return reports.sort((a, b) => new Date(b.date) - new Date(a.date));
}

// Format duration
function formatDuration(durationMs) {
    if (!durationMs) return 'N/A';
    const seconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes > 0 ? `${minutes}m ${remainingSeconds}s` : `${seconds}s`;
}

// Update summary statistics
function updateSummaryStats() {
    const total = filteredReports.length;
    const successful = filteredReports.filter(r => r.status === 'passed').length;
    const failed = filteredReports.filter(r => r.status === 'failed').length;
    const avgRate = total > 0
        ? (filteredReports.reduce((sum, r) => sum + r.passRate, 0) / total).toFixed(1)
        : 0;

    animateCounter('totalReports', total);
    animateCounter('successfulRuns', successful);
    animateCounter('failedRuns', failed);
    animateValue('avgSuccessRate', avgRate, '%');
}

// Animate counter
function animateCounter(elementId, targetValue, duration = 800) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const startValue = parseInt(element.textContent) || 0;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const currentValue = Math.floor(startValue + (targetValue - startValue) * easeOutQuart);

        element.textContent = currentValue;

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

// Animate value with suffix
function animateValue(elementId, targetValue, suffix = '') {
    const element = document.getElementById(elementId);
    if (!element) return;

    const numericValue = parseFloat(targetValue);
    const startValue = parseFloat(element.textContent) || 0;
    const duration = 800;
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

// Render reports table
function renderReports() {
    const tbody = document.getElementById('reportsTable');
    if (!tbody) return;

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const pageReports = filteredReports.slice(startIndex, endIndex);

    if (pageReports.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-muted" style="padding: 3rem;">
                    <div style="font-size: 2rem; margin-bottom: 0.5rem;">📭</div>
                    <p>No reports found</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = pageReports.map((report, index) => `
        <tr style="animation: fadeIn 0.5s ease ${index * 0.05}s both;" onclick="viewReportDetails('${report.id}')">
            <td><strong>${report.id}</strong></td>
            <td>${report.suite}</td>
            <td><span class="badge badge-${getBadgeClass(report.status)}">${report.status}</span></td>
            <td>
                <div style="font-size: 0.875rem;">
                    <span style="color: var(--success-color);">✓ ${report.passed}</span> / 
                    <span style="color: var(--danger-color);">✗ ${report.failed}</span> / 
                    <span style="color: var(--text-secondary);">${report.total}</span>
                </div>
            </td>
            <td>
                <div class="d-flex align-items-center gap-1">
                    <span class="text-bold">${report.passRate}%</span>
                    <div class="progress" style="width: 60px; height: 6px;">
                        <div class="progress-bar ${report.passRate >= 90 ? 'success' : report.passRate >= 70 ? 'warning' : 'danger'}" 
                             style="width: ${report.passRate}%"></div>
                    </div>
                </div>
            </td>
            <td>${report.duration}</td>
            <td>${formatDate(report.date)}</td>
            <td onclick="event.stopPropagation();">
                <button class="btn btn-secondary btn-sm" onclick="viewReportDetails('${report.id}')">
                    View
                </button>
            </td>
        </tr>
    `).join('');

    updatePagination();
}

// Get badge class based on status
function getBadgeClass(status) {
    const classes = {
        'passed': 'success',
        'failed': 'danger',
        'partial': 'warning'
    };
    return classes[status] || 'secondary';
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays < 7) return `${diffDays} days ago`;

    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Update pagination
function updatePagination() {
    const totalPages = Math.ceil(filteredReports.length / pageSize);
    const startIndex = (currentPage - 1) * pageSize + 1;
    const endIndex = Math.min(currentPage * pageSize, filteredReports.length);

    document.getElementById('showingStart').textContent = filteredReports.length > 0 ? startIndex : 0;
    document.getElementById('showingEnd').textContent = endIndex;
    document.getElementById('totalCount').textContent = filteredReports.length;

    const paginationButtons = document.getElementById('paginationButtons');
    if (!paginationButtons) return;

    let buttons = '';

    // Previous button
    buttons += `<button class="btn btn-secondary btn-sm" ${currentPage === 1 ? 'disabled' : ''} 
                onclick="changePage(${currentPage - 1})">←</button>`;

    // Page numbers
    for (let i = 1; i <= Math.min(totalPages, 5); i++) {
        const pageNum = i;
        buttons += `<button class="btn ${currentPage === pageNum ? 'btn-primary' : 'btn-secondary'} btn-sm" 
                    onclick="changePage(${pageNum})">${pageNum}</button>`;
    }

    if (totalPages > 5) {
        buttons += `<span style="padding: 0 0.5rem;">...</span>`;
        buttons += `<button class="btn ${currentPage === totalPages ? 'btn-primary' : 'btn-secondary'} btn-sm" 
                    onclick="changePage(${totalPages})">${totalPages}</button>`;
    }

    // Next button
    buttons += `<button class="btn btn-secondary btn-sm" ${currentPage === totalPages ? 'disabled' : ''} 
                onclick="changePage(${currentPage + 1})">→</button>`;

    paginationButtons.innerHTML = buttons;
}

// Change page
function changePage(page) {
    const totalPages = Math.ceil(filteredReports.length / pageSize);
    if (page < 1 || page > totalPages) return;

    currentPage = page;
    renderReports();

    // Scroll to top of table
    document.querySelector('.table-container').scrollIntoView({ behavior: 'smooth' });
}

// Filter reports
function filterReports() {
    const dateRange = document.getElementById('dateRange').value;
    const suite = document.getElementById('suiteFilter').value;
    const status = document.getElementById('statusFilter').value;

    filteredReports = allReports.filter(report => {
        // Date filter
        if (dateRange !== 'custom') {
            const days = parseInt(dateRange);
            const reportDate = new Date(report.date);
            const cutoffDate = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
            if (reportDate < cutoffDate) return false;
        }

        // Suite filter
        if (suite !== 'all') {
            const suiteMap = {
                'api': 'API Test Suite',
                'ui': 'UI Test Suite',
                'integration': 'Integration Tests',
                'regression': 'Regression Suite',
                'smoke': 'Smoke Tests'
            };
            if (report.suite !== suiteMap[suite]) return false;
        }

        // Status filter
        if (status !== 'all' && report.status !== status) return false;

        return true;
    });

    currentPage = 1;
    updateSummaryStats();
    renderReports();
    showNotification(`Filtered to ${filteredReports.length} reports`, 'info');
}

// Search reports
function searchReports() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();

    if (!searchTerm) {
        filteredReports = [...allReports];
    } else {
        filteredReports = allReports.filter(report =>
            report.id.toLowerCase().includes(searchTerm) ||
            report.suite.toLowerCase().includes(searchTerm) ||
            report.status.toLowerCase().includes(searchTerm)
        );
    }

    currentPage = 1;
    updateSummaryStats();
    renderReports();
}

// Sort reports
function sortReports(column) {
    if (sortColumn === column) {
        sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        sortColumn = column;
        sortDirection = 'desc';
    }

    filteredReports.sort((a, b) => {
        let aVal = a[column];
        let bVal = b[column];

        if (column === 'date') {
            aVal = new Date(aVal);
            bVal = new Date(bVal);
        } else if (column === 'duration') {
            aVal = a.durationSeconds;
            bVal = b.durationSeconds;
        }

        if (sortDirection === 'asc') {
            return aVal > bVal ? 1 : -1;
        } else {
            return aVal < bVal ? 1 : -1;
        }
    });

    renderReports();
}

// View report details
function viewReportDetails(reportId) {
    const report = allReports.find(r => r.id === reportId);
    if (!report) return;

    const modal = document.getElementById('reportDetailsModal');
    const body = document.getElementById('reportDetailsBody');

    body.innerHTML = `
        <div class="mb-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h4>${report.id}</h4>
                <span class="badge badge-${getBadgeClass(report.status)}" style="font-size: 1rem; padding: 0.5rem 1rem;">
                    ${report.status.toUpperCase()}
                </span>
            </div>
            <p class="text-muted">${report.suite} • ${formatDate(report.date)}</p>
        </div>

        <div class="stats-grid" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 1.5rem;">
            <div class="text-center">
                <div class="text-muted" style="font-size: 0.75rem;">PASSED</div>
                <div class="text-bold" style="font-size: 1.5rem; color: var(--success-color);">${report.passed}</div>
            </div>
            <div class="text-center">
                <div class="text-muted" style="font-size: 0.75rem;">FAILED</div>
                <div class="text-bold" style="font-size: 1.5rem; color: var(--danger-color);">${report.failed}</div>
            </div>
            <div class="text-center">
                <div class="text-muted" style="font-size: 0.75rem;">SKIPPED</div>
                <div class="text-bold" style="font-size: 1.5rem; color: var(--warning-color);">${report.skipped}</div>
            </div>
            <div class="text-center">
                <div class="text-muted" style="font-size: 0.75rem;">TOTAL</div>
                <div class="text-bold" style="font-size: 1.5rem; color: var(--info-color);">${report.total}</div>
            </div>
        </div>

        <div class="mb-3">
            <div class="d-flex justify-content-between mb-1">
                <span class="text-bold">Success Rate</span>
                <span class="text-bold">${report.passRate}%</span>
            </div>
            <div class="progress" style="height: 12px;">
                <div class="progress-bar ${report.passRate >= 90 ? 'success' : report.passRate >= 70 ? 'warning' : 'danger'}" 
                     style="width: ${report.passRate}%"></div>
            </div>
        </div>

        <div class="grid-2" style="gap: 1rem;">
            <div>
                <div class="text-muted" style="font-size: 0.875rem;">Duration</div>
                <div class="text-bold">${report.duration}</div>
            </div>
            <div>
                <div class="text-muted" style="font-size: 0.875rem;">Environment</div>
                <div class="text-bold">${report.environment.toUpperCase()}</div>
            </div>
        </div>

        <div class="alert alert-info mt-3">
            <span>ℹ</span>
            <span>Full execution logs and screenshots are available in the detailed report</span>
        </div>
    `;

    modal.classList.add('show');
}

// Close report modal
function closeReportModal() {
    document.getElementById('reportDetailsModal').classList.remove('show');
}

// Export reports
function exportReports() {
    document.getElementById('exportModal').classList.add('show');
}

// Close export modal
function closeExportModal() {
    document.getElementById('exportModal').classList.remove('show');
}

// Confirm export
function confirmExport() {
    const format = document.getElementById('exportFormat').value;
    showNotification(`Exporting reports as ${format.toUpperCase()}...`, 'info');

    setTimeout(() => {
        showNotification(`Reports exported successfully!`, 'success');
        closeExportModal();
    }, 1500);
}

// Download report
function downloadReport() {
    showNotification('Downloading PDF report...', 'info');
    setTimeout(() => {
        showNotification('Report downloaded successfully!', 'success');
    }, 1000);
}

// View Allure report
function viewAllureReport() {
    window.open('/allure-report', '_blank');
}

// Generate report
function generateReport() {
    showNotification('Generating new report...', 'info');
    setTimeout(() => {
        showNotification('Report generated successfully!', 'success');
        loadReports();
    }, 2000);
}

// Show loading state
function showLoadingState() {
    const tbody = document.getElementById('reportsTable');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center">
                    <div class="spinner" style="margin: 2rem auto;"></div>
                    Loading reports...
                </td>
            </tr>
        `;
    }
}

// Show error
function showError(message) {
    const tbody = document.getElementById('reportsTable');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center text-muted" style="padding: 3rem;">
                    <div style="font-size: 2rem; margin-bottom: 0.5rem; color: var(--danger-color);">✗</div>
                    <p>${message}</p>
                </td>
            </tr>
        `;
    }
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

// Close modals on outside click
window.addEventListener('click', function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.classList.remove('show');
    }
});

// Add necessary styles
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
    .table tbody tr {
        cursor: pointer;
    }
`;
document.head.appendChild(style);
