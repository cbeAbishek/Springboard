// Complete Navigation System for Springboard Framework
console.log('Loading Springboard Navigation System...');

// Global variables
let currentSection = 'dashboard';
let isNavigating = false;

// Navigation system initialization
function initializeNavigation() {
    console.log('Initializing navigation system...');

    // Find all navigation links
    const navLinks = document.querySelectorAll('.nav-link');
    console.log(`Found ${navLinks.length} navigation links`);

    if (navLinks.length === 0) {
        console.error('No navigation links found!');
        return;
    }

    // Add click handlers to navigation links
    navLinks.forEach(link => {
        const section = link.getAttribute('data-section');
        console.log(`Setting up navigation for section: ${section}`);

        link.addEventListener('click', async function(e) {
            e.preventDefault();
            e.stopPropagation();

            const targetSection = this.getAttribute('data-section');
            console.log(`Navigation clicked: ${targetSection}`);

            if (isNavigating) {
                console.log('Navigation already in progress, ignoring click');
                return;
            }

            await switchToSection(targetSection);
        });
    });

    // Initialize mobile menu
    setupMobileMenu();

    // Set initial section
    console.log('Setting initial section to dashboard...');
    switchToSection('dashboard');

    console.log('Navigation system initialized successfully');
}

// Main section switching function
async function switchToSection(sectionName) {
    if (currentSection === sectionName && !isNavigating) {
        console.log(`Already in section: ${sectionName}`);
        return;
    }

    console.log(`Switching to section: ${sectionName}`);
    isNavigating = true;

    try {
        // Update navigation UI
        updateNavigationState(sectionName);

        // Hide all sections
        hideAllSections();

        // Show target section
        showTargetSection(sectionName);

        // Update page title
        updatePageTitle(sectionName);

        // Load section data
        await loadSectionContent(sectionName);

        // Update current section
        currentSection = sectionName;

        console.log(`Successfully switched to: ${sectionName}`);

    } catch (error) {
        console.error(`Error switching to section ${sectionName}:`, error);
        showNotification(`Failed to load ${sectionName} section`, 'error');
    } finally {
        isNavigating = false;
    }
}

// Update navigation visual state
function updateNavigationState(sectionName) {
    console.log(`Updating navigation state for: ${sectionName}`);

    // Remove active class from all nav links
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
        link.removeAttribute('aria-current');
    });

    // Add active class to target nav link
    const targetLink = document.querySelector(`[data-section="${sectionName}"]`);
    if (targetLink) {
        targetLink.classList.add('active');
        targetLink.setAttribute('aria-current', 'page');
        console.log(`Activated navigation link for: ${sectionName}`);
    } else {
        console.warn(`Navigation link not found for section: ${sectionName}`);
    }
}

// Hide all content sections
function hideAllSections() {
    console.log('Hiding all sections...');
    const sections = document.querySelectorAll('.content-section');
    console.log(`Found ${sections.length} content sections`);

    sections.forEach(section => {
        section.classList.remove('active');
        section.style.display = 'none';
    });
}

// Show target section
function showTargetSection(sectionName) {
    console.log(`Showing section: ${sectionName}`);

    // Try multiple section ID formats
    const possibleIds = [
        `${sectionName}-section`,
        sectionName,
        `section-${sectionName}`
    ];

    let targetSection = null;
    for (const id of possibleIds) {
        targetSection = document.getElementById(id);
        if (targetSection) {
            console.log(`Found section with ID: ${id}`);
            break;
        }
    }

    if (targetSection) {
        targetSection.style.display = 'block';
        targetSection.classList.add('active');

        // Add animation
        targetSection.style.opacity = '0';
        targetSection.style.transform = 'translateY(10px)';

        // Trigger animation
        setTimeout(() => {
            targetSection.style.transition = 'all 0.3s ease-in-out';
            targetSection.style.opacity = '1';
            targetSection.style.transform = 'translateY(0)';
        }, 10);

        console.log(`Successfully showed section: ${sectionName}`);
    } else {
        console.error(`Section not found: ${sectionName}. Tried IDs: ${possibleIds.join(', ')}`);
        showNotification(`Section "${sectionName}" not found`, 'error');
    }
}

// Update page title based on section
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

// Load section-specific content
async function loadSectionContent(sectionName) {
    console.log(`Loading content for section: ${sectionName}`);

    try {
        switch (sectionName) {
            case 'dashboard':
                await loadDashboardContent();
                break;
            case 'testcases':
                await loadTestCasesContent();
                break;
            case 'execution':
                await loadExecutionContent();
                break;
            case 'batches':
                await loadBatchesContent();
                break;
            case 'schedules':
                await loadSchedulesContent();
                break;
            case 'reports':
                await loadReportsContent();
                break;
            case 'analytics':
                await loadAnalyticsContent();
                break;
            case 'validation':
                await loadValidationContent();
                break;
            case 'demo':
                await loadDemoContent();
                break;
            case 'docs':
                await loadDocsContent();
                break;
            default:
                console.warn(`No content loader for section: ${sectionName}`);
        }
    } catch (error) {
        console.error(`Error loading content for ${sectionName}:`, error);
    }
}

// Content loading functions
async function loadDashboardContent() {
    console.log('Loading dashboard content...');

    // Update metrics
    const metrics = {
        totalTestCases: 12,
        totalBatches: 8,
        totalExecutions: 45,
        activeSchedules: 3
    };

    // Animate counters
    animateCounter('total-test-cases', 0, metrics.totalTestCases);
    animateCounter('total-batches', 0, metrics.totalBatches);
    animateCounter('total-executions', 0, metrics.totalExecutions);
    animateCounter('active-schedules', 0, metrics.activeSchedules);

    // Update progress bars
    setTimeout(() => {
        updateProgressBar('test-cases-progress', 75);
        updateProgressBar('batches-progress', 60);
        updateProgressBar('executions-progress', 85);
        updateProgressBar('schedules-progress', 40);
    }, 500);
}

async function loadTestCasesContent() {
    console.log('Loading test cases content...');

    const tbody = document.getElementById('testcases-table-body');
    if (tbody) {
        tbody.innerHTML = `
            <tr class="hover:bg-white/5">
                <td class="px-6 py-4"><input type="checkbox" class="custom-checkbox"></td>
                <td class="px-6 py-4 text-white font-medium">Sample Login Test</td>
                <td class="px-6 py-4 text-gray-300">Authentication</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full">WEB_UI</span></td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded-full">dev</span></td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-yellow-100 text-yellow-800 rounded-full">MEDIUM</span></td>
                <td class="px-6 py-4 text-gray-400 text-xs">2 hours ago</td>
                <td class="px-6 py-4">
                    <button class="text-green-400 hover:text-green-300 mr-2"><i class="fas fa-play"></i></button>
                    <button class="text-blue-400 hover:text-blue-300 mr-2"><i class="fas fa-edit"></i></button>
                    <button class="text-red-400 hover:text-red-300"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
            <tr class="hover:bg-white/5">
                <td class="px-6 py-4"><input type="checkbox" class="custom-checkbox"></td>
                <td class="px-6 py-4 text-white font-medium">API Health Check</td>
                <td class="px-6 py-4 text-gray-300">Health</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full">API</span></td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded-full">staging</span></td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-red-100 text-red-800 rounded-full">HIGH</span></td>
                <td class="px-6 py-4 text-gray-400 text-xs">1 hour ago</td>
                <td class="px-6 py-4">
                    <button class="text-green-400 hover:text-green-300 mr-2"><i class="fas fa-play"></i></button>
                    <button class="text-blue-400 hover:text-blue-300 mr-2"><i class="fas fa-edit"></i></button>
                    <button class="text-red-400 hover:text-red-300"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
        `;
    }

    // Update test case count
    const countElement = document.getElementById('testcase-count');
    if (countElement) {
        countElement.textContent = '2';
    }
}

async function loadExecutionContent() {
    console.log('Loading execution content...');

    // Update execution stats
    animateCounter('executions-running', 0, 1);
    animateCounter('executions-queued', 0, 3);
    animateCounter('executions-completed', 0, 25);
    animateCounter('executions-failed', 0, 2);

    // Update current executions
    const executionsContainer = document.getElementById('current-executions');
    if (executionsContainer) {
        executionsContainer.innerHTML = `
            <div class="flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10 mb-3">
                <div>
                    <div class="font-medium text-white">Sample Login Test</div>
                    <div class="text-sm text-gray-400">ID: exec_001 | dev environment</div>
                </div>
                <div class="text-right">
                    <span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full">RUNNING</span>
                    <div class="text-xs text-gray-400 mt-1">Started 2 min ago</div>
                </div>
            </div>
            <div class="flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10">
                <div>
                    <div class="font-medium text-white">API Health Check</div>
                    <div class="text-sm text-gray-400">ID: exec_002 | staging environment</div>
                </div>
                <div class="text-right">
                    <span class="px-2 py-1 text-xs bg-yellow-100 text-yellow-800 rounded-full">QUEUED</span>
                    <div class="text-xs text-gray-400 mt-1">Waiting...</div>
                </div>
            </div>
        `;
    }
}

async function loadBatchesContent() {
    console.log('Loading batches content...');

    // Update batch stats
    animateCounter('active-batches-count', 0, 2);
    animateCounter('recent-batches-count', 0, 8);
    document.getElementById('batch-success-rate').textContent = '85%';

    // Update batches table
    const tbody = document.getElementById('batches-table-body');
    if (tbody) {
        tbody.innerHTML = `
            <tr class="hover:bg-white/5">
                <td class="px-6 py-4 font-mono text-sm text-white">batch_001</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full">COMPLETED</span></td>
                <td class="px-6 py-4 text-gray-300">Smoke Tests</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded-full">dev</span></td>
                <td class="px-6 py-4 text-gray-300">2 hours ago</td>
                <td class="px-6 py-4">
                    <button class="text-blue-400 hover:text-blue-300 mr-2"><i class="fas fa-eye"></i></button>
                    <button class="text-green-400 hover:text-green-300"><i class="fas fa-file-alt"></i></button>
                </td>
            </tr>
            <tr class="hover:bg-white/5">
                <td class="px-6 py-4 font-mono text-sm text-white">batch_002</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full">RUNNING</span></td>
                <td class="px-6 py-4 text-gray-300">Regression Suite</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded-full">staging</span></td>
                <td class="px-6 py-4 text-gray-300">30 min ago</td>
                <td class="px-6 py-4">
                    <button class="text-blue-400 hover:text-blue-300 mr-2"><i class="fas fa-eye"></i></button>
                    <button class="text-gray-400"><i class="fas fa-file-alt"></i></button>
                </td>
            </tr>
        `;
    }
}

async function loadSchedulesContent() {
    console.log('Loading schedules content...');

    const tbody = document.getElementById('schedules-table-body');
    if (tbody) {
        tbody.innerHTML = `
            <tr class="hover:bg-white/5">
                <td class="px-6 py-4 text-white font-medium">Daily Smoke Tests</td>
                <td class="px-6 py-4 text-gray-300">smoke-suite</td>
                <td class="px-6 py-4 font-mono text-sm text-gray-300">0 9 * * *</td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full">Active</span></td>
                <td class="px-6 py-4"><span class="px-2 py-1 text-xs bg-gray-100 text-gray-800 rounded-full">dev</span></td>
                <td class="px-6 py-4">
                    <button class="text-yellow-400 hover:text-yellow-300 mr-2"><i class="fas fa-pause"></i></button>
                    <button class="text-blue-400 hover:text-blue-300 mr-2"><i class="fas fa-bolt"></i></button>
                    <button class="text-red-400 hover:text-red-300"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
        `;
    }
}

async function loadReportsContent() {
    console.log('Loading reports content...');

    const reportsList = document.getElementById('reports-list');
    if (reportsList) {
        reportsList.innerHTML = `
            <div class="flex items-center justify-between p-4 glass-effect rounded-lg border border-white/10 mb-3">
                <div class="flex items-center space-x-3">
                    <div class="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center">
                        <i class="fas fa-file-alt text-green-400"></i>
                    </div>
                    <div>
                        <div class="font-medium text-white">Report for batch_001</div>
                        <div class="text-sm text-gray-400">Generated: 2 hours ago</div>
                    </div>
                </div>
                <div class="flex space-x-2">
                    <button class="btn btn-sm btn-primary"><i class="fas fa-file-alt mr-1"></i>Generate</button>
                    <button class="btn btn-sm btn-secondary"><i class="fas fa-download mr-1"></i>Download</button>
                </div>
            </div>
        `;
    }
}

async function loadAnalyticsContent() {
    console.log('Loading analytics content...');
    // Analytics content loads on demand
}

async function loadValidationContent() {
    console.log('Loading validation content...');
    // Validation content loads on demand
}

async function loadDemoContent() {
    console.log('Loading demo content...');
    // Demo content loads on demand
}

async function loadDocsContent() {
    console.log('Loading docs content...');

    const container = document.getElementById('api-docs-content');
    if (container) {
        container.innerHTML = `
            <div class="space-y-6">
                <div class="border border-white/10 rounded-lg p-4">
                    <h3 class="text-lg font-semibold text-white mb-4">Test Cases Endpoints</h3>
                    <div class="space-y-3">
                        <div class="bg-gray-800 rounded p-3">
                            <div class="flex items-center space-x-3 mb-2">
                                <span class="px-2 py-1 text-xs bg-green-100 text-green-800 rounded">GET</span>
                                <code class="text-cyan-400 text-sm">/api/testcases</code>
                            </div>
                            <p class="text-gray-300 text-sm">Get all test cases</p>
                        </div>
                        <div class="bg-gray-800 rounded p-3">
                            <div class="flex items-center space-x-3 mb-2">
                                <span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">POST</span>
                                <code class="text-cyan-400 text-sm">/api/testcases</code>
                            </div>
                            <p class="text-gray-300 text-sm">Create a new test case</p>
                        </div>
                    </div>
                </div>
                <div class="border border-white/10 rounded-lg p-4">
                    <h3 class="text-lg font-semibold text-white mb-4">Execution Endpoints</h3>
                    <div class="space-y-3">
                        <div class="bg-gray-800 rounded p-3">
                            <div class="flex items-center space-x-3 mb-2">
                                <span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">POST</span>
                                <code class="text-cyan-400 text-sm">/api/execution/single</code>
                            </div>
                            <p class="text-gray-300 text-sm">Execute a single test case</p>
                        </div>
                        <div class="bg-gray-800 rounded p-3">
                            <div class="flex items-center space-x-3 mb-2">
                                <span class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded">POST</span>
                                <code class="text-cyan-400 text-sm">/api/execution/batch</code>
                            </div>
                            <p class="text-gray-300 text-sm">Execute a batch of tests</p>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
}

// Utility functions
function animateCounter(elementId, start, end, duration = 1000) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const startTime = performance.now();
    const difference = end - start;

    function updateCounter(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const currentValue = Math.floor(start + (difference * progress));

        element.textContent = currentValue;

        if (progress < 1) {
            requestAnimationFrame(updateCounter);
        } else {
            element.textContent = end;
        }
    }

    requestAnimationFrame(updateCounter);
}

function updateProgressBar(elementId, percentage) {
    const element = document.getElementById(elementId);
    if (element) {
        element.style.width = `${percentage}%`;
    }
}

function setupMobileMenu() {
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

function showNotification(message, type = 'info') {
    console.log(`Notification: ${message} (${type})`);

    // Create notification element
    const notification = document.createElement('div');
    notification.className = `fixed top-4 right-4 z-50 p-4 rounded-lg shadow-lg transition-all duration-300 transform translate-x-full`;

    // Set notification style based on type
    const styles = {
        'success': 'bg-green-600 text-white',
        'error': 'bg-red-600 text-white',
        'warning': 'bg-yellow-600 text-white',
        'info': 'bg-blue-600 text-white'
    };

    notification.className += ` ${styles[type] || styles.info}`;
    notification.innerHTML = `
        <div class="flex items-center space-x-2">
            <i class="fas fa-${type === 'success' ? 'check' : type === 'error' ? 'exclamation-triangle' : type === 'warning' ? 'exclamation-circle' : 'info-circle'}"></i>
            <span>${message}</span>
        </div>
    `;

    document.body.appendChild(notification);

    // Animate in
    setTimeout(() => {
        notification.classList.remove('translate-x-full');
    }, 10);

    // Remove after 3 seconds
    setTimeout(() => {
        notification.classList.add('translate-x-full');
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, 3000);
}

// Initialize when DOM is loaded
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeNavigation);
} else {
    initializeNavigation();
}

// Export functions for global access
window.switchToSection = switchToSection;
window.initializeNavigation = initializeNavigation;

console.log('Springboard Navigation System loaded successfully');
