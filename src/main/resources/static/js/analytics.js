// Analytics & Reporting Module
// Provides functions referenced by navigation and handlers (loadAnalytics, handleLoadTrends, handleLoadRegression, handleGenerateReport, loadReports)

async function loadAnalytics() {
    try {
        setDefaultAnalyticsDates();
        // Optionally auto-load trends for last 7 days
        await handleLoadTrends();
    } catch (e) {
        console.warn('loadAnalytics failed:', e);
    }
}

function setDefaultAnalyticsDates() {
    const fromInput = document.getElementById('analytics-from-date');
    const toInput = document.getElementById('analytics-to-date');
    const today = new Date();
    const sevenDaysAgo = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000);
    if (fromInput && !fromInput.value) fromInput.value = sevenDaysAgo.toISOString().substring(0,10);
    if (toInput && !toInput.value) toInput.value = today.toISOString().substring(0,10);
}

function buildISODate(dateStr, endOfDay = false) {
    if (!dateStr) return null;
    return endOfDay ? `${dateStr}T23:59:59` : `${dateStr}T00:00:00`;
}

async function handleLoadTrends() {
    const fromInput = document.getElementById('analytics-from-date');
    const toInput = document.getElementById('analytics-to-date');
    const container = document.getElementById('trend-analysis-content');

    if (!container) return;

    const fromDate = buildISODate(fromInput?.value);
    const toDate = buildISODate(toInput?.value, true);

    if (!fromDate || !toDate) {
        notificationManager?.show?.('Select valid from/to dates', 'error');
        return;
    }

    container.innerHTML = '<div class="text-gray-400 text-sm">Loading trend analysis...</div>';
    try {
        const data = await ApiClient.get(`/analytics/trends?fromDate=${encodeURIComponent(fromDate)}&toDate=${encodeURIComponent(toDate)}`);
        renderTrendAnalysis(data);
    } catch (e) {
        console.error('Trend analysis load failed', e);
        container.innerHTML = '<div class="text-red-400 text-sm">Failed to load trend analysis</div>';
    }
}

function renderTrendAnalysis(data) {
    const container = document.getElementById('trend-analysis-content');
    if (!container) return;
    if (!data) {
        container.innerHTML = '<div class="text-gray-400">No data available</div>';
        return;
    }
    const passRate = (data.passRate ?? 0).toFixed(2);
    const failRate = (data.failRate ?? 0).toFixed(2);
    const avgTime = (data.averageExecutionTime ?? 0).toFixed(0);

    const topFailing = data.topFailingTests ? Object.entries(data.topFailingTests).map(([k,v]) => `<li class="flex justify-between"><span>${k}</span><span class="text-red-400 font-semibold">${v}</span></li>`).join('') : '<li class="text-gray-500">None</li>';
    const envStats = data.environmentStats ? Object.entries(data.environmentStats).map(([k,v]) => `<li class="flex justify-between"><span>${k}</span><span class="text-cyan-400 font-semibold">${v}</span></li>`).join('') : '<li class="text-gray-500">None</li>';

    container.innerHTML = `
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Total Executions</div>
          <div class="text-lg font-semibold text-white">${data.totalExecutions || 0}</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Pass Rate</div>
          <div class="text-lg font-semibold text-green-400">${passRate}%</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Fail Rate</div>
          <div class="text-lg font-semibold text-red-400">${failRate}%</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Avg Duration (ms)</div>
            <div class="text-lg font-semibold text-blue-400">${avgTime}</div>
        </div>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <h4 class="text-sm font-semibold text-white mb-2">Top Failing Tests</h4>
          <ul class="space-y-1 text-sm">${topFailing}</ul>
        </div>
        <div>
          <h4 class="text-sm font-semibold text-white mb-2">Environment Distribution</h4>
          <ul class="space-y-1 text-sm">${envStats}</ul>
        </div>
      </div>`;
}

async function handleLoadRegression() {
    const envSelect = document.getElementById('regression-env');
    const daysInput = document.getElementById('regression-days');
    const container = document.getElementById('regression-metrics-content');
    if (!container) return;

    const env = envSelect?.value || 'dev';
    const days = parseInt(daysInput?.value) || 30;
    container.innerHTML = '<div class="text-gray-400 text-sm">Loading regression metrics...</div>';
    try {
        const data = await ApiClient.get(`/analytics/regression/${encodeURIComponent(env)}?days=${days}`);
        renderRegressionMetrics(data);
    } catch (e) {
        console.error('Regression metrics load failed', e);
        container.innerHTML = '<div class="text-red-400 text-sm">Failed to load regression metrics</div>';
    }
}

function renderRegressionMetrics(data) {
    const container = document.getElementById('regression-metrics-content');
    if (!container) return;
    if (!data) { container.innerHTML = '<div class="text-gray-400">No data</div>'; return; }

    container.innerHTML = `
      <div class="grid grid-cols-2 gap-4 mb-4">
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Stability Score</div>
          <div class="text-lg font-semibold text-cyan-400">${(data.stabilityScore||0).toFixed(2)}%</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Regression Detection</div>
          <div class="text-lg font-semibold text-yellow-400">${(data.regressionDetectionRate||0).toFixed(2)}%</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Avg Exec Time (ms)</div>
          <div class="text-lg font-semibold text-blue-400">${(data.averageExecutionTime||0).toFixed(0)}</div>
        </div>
        <div class="p-3 rounded-lg bg-white/5 border border-white/10 text-center">
          <div class="text-xs text-gray-400">Total Executions</div>
          <div class="text-lg font-semibold text-white">${data.totalExecutions||0}</div>
        </div>
      </div>`;
}

// Reporting
async function loadReports() {
    const list = document.getElementById('reports-list');
    if (!list) return;
    list.innerHTML = '<div class="text-gray-400 text-sm">Loading reports...</div>';
    try {
        const reports = await ApiClient.get('/reports/list');
        if (!Array.isArray(reports) || reports.length === 0) {
            list.innerHTML = '<div class="text-gray-400">No reports found</div>';
            return;
        }
        list.innerHTML = reports.map(r => renderReportItem(r)).join('');
    } catch (e) {
        console.error('Failed to load reports', e);
        list.innerHTML = '<div class="text-red-400 text-sm">Failed to load reports</div>';
    }
}

function renderReportItem(r) {
    const size = formatFileSize(r.size || 0);
    const created = r.createdAt ? new Date(r.createdAt).toLocaleString() : '';
    return `<div class="p-4 rounded-lg bg-white/5 border border-white/10 flex items-center justify-between">
        <div>
          <div class="text-sm font-medium text-white">${r.filename}</div>
          <div class="text-xs text-gray-400">${r.type||'Unknown'} • ${size} • ${created}</div>
        </div>
        <div class="flex space-x-2">
          <a class="btn btn-secondary btn-sm" href="/api/reports/view/${encodeURIComponent(r.filename)}" target="_blank"><i class="fas fa-eye mr-1"></i>View</a>
          <a class="btn btn-primary btn-sm" href="/api/reports/download/${encodeURIComponent(r.filename)}"><i class="fas fa-download mr-1"></i>Download</a>
          <button class="btn btn-danger btn-sm" onclick="deleteReport('${r.filename}')"><i class="fas fa-trash mr-1"></i>Delete</button>
        </div>
      </div>`;
}

async function deleteReport(filename) {
    if (!confirm(`Delete report ${filename}?`)) return;
    try {
        await ApiClient.delete(`/reports/delete/${encodeURIComponent(filename)}`);
        notificationManager?.show?.('Report deleted', 'success');
        loadReports();
    } catch (e) {
        notificationManager?.show?.('Failed to delete report', 'error');
    }
}

async function handleGenerateReport() {
    const batchSelect = document.getElementById('report-batch-id');
    const typeSelect = document.getElementById('report-type');
    const batchId = batchSelect?.value;
    if (!batchId) {
        notificationManager?.show?.('Select a batch', 'error');
        return;
    }
    try {
        loadingManager.show('Generating report...');
        // Fetch executions for batch
        const executions = await ApiClient.get(`/execution/batch/${encodeURIComponent(batchId)}/executions`);
        const executionIds = (executions||[]).map(e => e.id);
        if (executionIds.length === 0) {
            notificationManager?.show?.('No executions found for batch', 'warning');
            loadingManager.hide();
            return;
        }
        const format = typeSelect?.value === 'html' ? 'html' : 'html'; // currently html only or all
        const payload = { executionIds, reportFormat: format, reportType: 'Batch_'+batchId };
        const resp = await ApiClient.post('/reports/comprehensive/generate', payload);
        if (resp?.success) {
            notificationManager?.show?.('Report generated', 'success');
            // Refresh reports list
            await loadReports();
        } else {
            notificationManager?.show?.('Report generation failed', 'error');
        }
    } catch (e) {
        console.error('Report generation failed', e);
        notificationManager?.show?.('Report generation error', 'error');
    } finally {
        loadingManager.hide();
    }
}

// Utility ensure batch list is populated for report generation
async function populateReportBatchSelect() {
    const select = document.getElementById('report-batch-id');
    if (!select) return;
    try {
        const batches = await ApiClient.get('/execution/batches');
        select.innerHTML = '<option value="">Select batch...</option>' + (batches||[]).slice(0,100).map(b => `<option value="${b.batchId}">${b.batchName||b.batchId} (${b.status})</option>`).join('');
    } catch (e) {
        console.warn('Failed loading batches for report select');
    }
}

// Ensure reports section loads necessary data
async function initializeReportsSection() {
    await populateReportBatchSelect();
    await loadReports();
}

// Hook into section loading (if reports already active when script loads)
if (document.readyState === 'complete' || document.readyState === 'interactive') {
    setTimeout(() => {
        if (document.getElementById('reports-section')?.classList.contains('active')) {
            initializeReportsSection();
        }
    }, 500);
}

