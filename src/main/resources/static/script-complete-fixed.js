// Enhanced Automation Testing Framework - Complete UI Integration with Professional UX
// Global Configuration
const API_BASE_URL = window.location.origin + '/api';
let currentSection = 'dashboard';
let refreshInterval = null;
let executionStatusPolling = new Map();
let parallelExecutionMonitor = null;
let retryAttempts = new Map();
let isOnline = navigator.onLine;

// Enhanced Error Handling and API Client with Retry Logic
class ApiClient {
    static async makeRequest(url, options = {}) {
        const requestId = Math.random().toString(36).substring(7);
        const maxRetries = options.retries || 3;
        const retryDelay = options.retryDelay || 1000;

        console.log(`[${requestId}] Making API request to:`, url);

        for (let attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), options.timeout || 10000);

                const response = await fetch(url, {
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Request-ID': requestId,
                        ...options.headers
                    },
                    signal: controller.signal,
                    ...options
                });

                clearTimeout(timeoutId);

                if (!response.ok) {
                    const errorData = await this.parseErrorResponse(response);
                    console.warn(`[${requestId}] API Error (${response.status}):`, errorData);

                    if (response.status >= 500 && attempt < maxRetries) {
                        console.log(`[${requestId}] Retrying in ${retryDelay}ms... (${attempt}/${maxRetries})`);
                        await this.delay(retryDelay * attempt);
                        continue;
                    }

                    throw new ApiError(errorData.message || response.statusText, response.status, errorData);
                }

                const result = await this.parseSuccessResponse(response);
                console.log(`[${requestId}] API Success:`, result);
                return result;

            } catch (error) {
                if (error.name === 'AbortError') {
                    throw new ApiError('Request timeout', 408);
                }

                if (!isOnline) {
                    throw new ApiError('No internet connection', 0);
                }

                if (attempt === maxRetries) {
                    console.error(`[${requestId}] Final attempt failed:`, error);
                    throw error instanceof ApiError ? error : new ApiError(error.message, 0);
                }

                console.warn(`[${requestId}] Attempt ${attempt} failed, retrying...`, error.message);
                await this.delay(retryDelay * attempt);
            }
        }
    }

    static async parseErrorResponse(response) {
        try {
            const contentType = response.headers.get('content-type');
            if (contentType?.includes('application/json')) {
                return await response.json();
            }
            return { message: await response.text() };
        } catch {
            return { message: `HTTP ${response.status} ${response.statusText}` };
        }
    }

    static async parseSuccessResponse(response) {
        const contentType = response.headers.get('content-type');
        if (contentType?.includes('application/json')) {
            return await response.json();
        } else if (contentType?.includes('text/')) {
            return await response.text();
        }
        return null;
    }

    static delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    static async get(endpoint, options = {}) {
        return this.makeRequest(`${API_BASE_URL}${endpoint}`, { method: 'GET', ...options });
    }

    static async post(endpoint, data, options = {}) {
        return this.makeRequest(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            body: JSON.stringify(data),
            ...options
        });
    }

    static async put(endpoint, data, options = {}) {
        return this.makeRequest(`${API_BASE_URL}${endpoint}`, {
            method: 'PUT',
            body: JSON.stringify(data),
            ...options
        });
    }

    static async delete(endpoint, options = {}) {
        return this.makeRequest(`${API_BASE_URL}${endpoint}`, { method: 'DELETE', ...options });
    }

    static async patch(endpoint, data, options = {}) {
        return this.makeRequest(`${API_BASE_URL}${endpoint}`, {
            method: 'PATCH',
            body: JSON.stringify(data),
            ...options
        });
    }
}

// Custom Error Classes
class ApiError extends Error {
    constructor(message, status, data = {}) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
    }
}

class ValidationError extends Error {
    constructor(message, field) {
        super(message);
        this.name = 'ValidationError';
        this.field = field;
    }
}

// Enhanced Notification System with Queue Management
class NotificationManager {
    constructor() {
        this.queue = [];
        this.maxVisible = 5;
        this.container = null;
    }

    show(message, type = 'info', options = {}) {
        const notification = {
            id: Math.random().toString(36).substring(7),
            message,
            type,
            duration: options.duration || 5000,
            persistent: options.persistent || false,
            actions: options.actions || []
        };

        this.queue.push(notification);
        this.processQueue();
        return notification.id;
    }

    processQueue() {
        const container = this.getContainer();
        const visible = container.querySelectorAll('.notification').length;

        if (visible >= this.maxVisible || this.queue.length === 0) {
            return;
        }

        const notification = this.queue.shift();
        this.renderNotification(notification);
    }

    renderNotification(notification) {
        const element = document.createElement('div');
        element.className = `notification notification-${notification.type} animate-slideIn`;
        element.dataset.id = notification.id;

        element.innerHTML = `
            <div class="notification-content">
                <i class="fas ${this.getIcon(notification.type)}"></i>
                <div class="notification-text">
                    <div class="notification-message">${notification.message}</div>
                    ${notification.actions.length > 0 ? this.renderActions(notification.actions) : ''}
                </div>
            </div>
            ${!notification.persistent ? `<button class="notification-close" onclick="notificationManager.close('${notification.id}')">
                <i class="fas fa-times"></i>
            </button>` : ''}
        `;

        this.getContainer().appendChild(element);

        if (!notification.persistent) {
            setTimeout(() => this.close(notification.id), notification.duration);
        }

        // Process next in queue
        setTimeout(() => this.processQueue(), 100);
    }

    renderActions(actions) {
        return `
            <div class="notification-actions">
                ${actions.map(action => `
                    <button class="btn btn-sm" onclick="${action.handler}">${action.label}</button>
                `).join('')}
            </div>
        `;
    }

    close(id) {
        const element = document.querySelector(`[data-id="${id}"]`);
        if (element) {
            element.classList.add('animate-fadeOut');
            setTimeout(() => {
                element.remove();
                this.processQueue();
            }, 300);
        }
    }

    closeAll() {
        const elements = this.getContainer().querySelectorAll('.notification');
        elements.forEach(el => {
            el.classList.add('animate-fadeOut');
            setTimeout(() => el.remove(), 300);
        });
        this.queue = [];
    }

    getIcon(type) {
        const icons = {
            'success': 'fa-check-circle',
            'error': 'fa-exclamation-triangle',
            'warning': 'fa-exclamation-circle',
            'info': 'fa-info-circle',
            'loading': 'fa-spinner fa-spin'
        };
        return icons[type] || icons.info;
    }

    getContainer() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'notification-container';
            this.container.className = 'notification-container';
            document.body.appendChild(this.container);
        }
        return this.container;
    }
}

// Initialize notification manager
const notificationManager = new NotificationManager();

// Enhanced Loading Manager
class LoadingManager {
    constructor() {
        this.activeLoaders = new Set();
        this.overlay = null;
    }

    show(message = 'Loading...', options = {}) {
        const id = Math.random().toString(36).substring(7);
        this.activeLoaders.add(id);

        const overlay = this.getOverlay();
        const textElement = overlay.querySelector('.loading-text');
        const progressElement = overlay.querySelector('.loading-progress-bar');

        if (textElement) textElement.textContent = message;
        if (progressElement && options.progress !== undefined) {
            progressElement.style.width = `${options.progress}%`;
        }

        overlay.classList.add('active');
        return id;
    }

    hide(id) {
        if (id) {
            this.activeLoaders.delete(id);
        } else {
            this.activeLoaders.clear();
        }

        if (this.activeLoaders.size === 0) {
            const overlay = this.getOverlay();
            overlay.classList.remove('active');
        }
    }

    updateProgress(progress, message) {
        const overlay = this.getOverlay();
        const progressElement = overlay.querySelector('.loading-progress-bar');
        const textElement = overlay.querySelector('.loading-text');

        if (progressElement) progressElement.style.width = `${progress}%`;
        if (textElement && message) textElement.textContent = message;
    }

    getOverlay() {
        if (!this.overlay) {
            this.overlay = document.createElement('div');
            this.overlay.className = 'loading-overlay';
            this.overlay.innerHTML = `
                <div class="loading-content">
                    <div class="loading-spinner"></div>
                    <div class="loading-text">Loading...</div>
                    <div class="loading-progress">
                        <div class="loading-progress-bar" style="width: 0%"></div>
                    </div>
                </div>
            `;
            document.body.appendChild(this.overlay);
        }
        return this.overlay;
    }
}

// Initialize loading manager
const loadingManager = new LoadingManager();

// Enhanced Form Validation
class FormValidator {
    static validateField(field, rules) {
        const errors = [];
        const value = field.value.trim();

        // Required validation
        if (rules.required && !value) {
            errors.push('This field is required');
        }

        // Skip other validations if field is empty and not required
        if (!value && !rules.required) {
            return errors;
        }

        // Length validations
        if (rules.minLength && value.length < rules.minLength) {
            errors.push(`Must be at least ${rules.minLength} characters`);
        }

        if (rules.maxLength && value.length > rules.maxLength) {
            errors.push(`Must be no more than ${rules.maxLength} characters`);
        }

        // Pattern validation
        if (rules.pattern && !rules.pattern.test(value)) {
            errors.push(rules.patternMessage || 'Invalid format');
        }

        // Email validation
        if (rules.email) {
            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(value)) {
                errors.push('Please enter a valid email address');
            }
        }

        // URL validation
        if (rules.url) {
            try {
                new URL(value);
            } catch {
                errors.push('Please enter a valid URL');
            }
        }

        // Number validation
        if (rules.number) {
            if (isNaN(Number(value))) {
                errors.push('Please enter a valid number');
            } else {
                const num = Number(value);
                if (rules.min !== undefined && num < rules.min) {
                    errors.push(`Must be at least ${rules.min}`);
                }
                if (rules.max !== undefined && num > rules.max) {
                    errors.push(`Must be no more than ${rules.max}`);
                }
            }
        }

        // Custom validation
        if (rules.custom && typeof rules.custom === 'function') {
            const customResult = rules.custom(value, field);
            if (customResult !== true && customResult) {
                errors.push(customResult);
            }
        }

        return errors;
    }

    static validateForm(form, validationRules) {
        const errors = {};
        let isValid = true;

        // Clear previous errors
        form.querySelectorAll('.field-error').forEach(el => el.remove());
        form.querySelectorAll('.form-input').forEach(el => {
            el.classList.remove('error', 'success');
        });

        // Validate each field
        Object.keys(validationRules).forEach(fieldName => {
            const field = form.querySelector(`[name="${fieldName}"]`);
            if (!field) return;

            const fieldErrors = this.validateField(field, validationRules[fieldName]);

            if (fieldErrors.length > 0) {
                errors[fieldName] = fieldErrors;
                isValid = false;
                this.showFieldError(field, fieldErrors[0]);
            } else {
                this.showFieldSuccess(field);
            }
        });

        return { isValid, errors };
    }

    static showFieldError(field, message) {
        field.classList.add('error');
        field.classList.remove('success');

        const errorElement = document.createElement('div');
        errorElement.className = 'field-error';
        errorElement.innerHTML = `<i class="fas fa-exclamation-circle"></i> ${message}`;

        field.parentNode.appendChild(errorElement);
    }

    static showFieldSuccess(field) {
        field.classList.add('success');
        field.classList.remove('error');

        const successElement = document.createElement('div');
        successElement.className = 'field-success';
        successElement.innerHTML = `<i class="fas fa-check-circle"></i> Valid`;

        field.parentNode.appendChild(successElement);
    }
}

// Enhanced Connection Manager
class ConnectionManager {
    constructor() {
        this.isOnline = navigator.onLine;
        this.listeners = [];
        this.setupEventListeners();
        this.startHealthCheck();
    }

    setupEventListeners() {
        window.addEventListener('online', () => {
            this.isOnline = true;
            this.notifyListeners('online');
            notificationManager.show('Connection restored', 'success');
        });

        window.addEventListener('offline', () => {
            this.isOnline = false;
            this.notifyListeners('offline');
            notificationManager.show('Connection lost - Working offline', 'warning', { persistent: true });
        });
    }

    startHealthCheck() {
        setInterval(async () => {
            try {
                await fetch('/health', { method: 'HEAD', cache: 'no-store' });
                if (!this.isOnline) {
                    this.isOnline = true;
                    this.notifyListeners('online');
                }
            } catch {
                if (this.isOnline) {
                    this.isOnline = false;
                    this.notifyListeners('offline');
                }
            }
        }, 30000); // Check every 30 seconds
    }

    onStatusChange(callback) {
        this.listeners.push(callback);
    }

    notifyListeners(status) {
        this.listeners.forEach(callback => callback(status, this.isOnline));
    }
}

// Initialize connection manager
const connectionManager = new ConnectionManager();

// Enhanced Application Initialization
document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM Content Loaded - Initializing enhanced application...');

    // Show initial loading
    const loadingId = loadingManager.show('Initializing application...');

    try {
        // Initialize core components
        initializeApplication();

        // Hide loading after short delay to show smooth transition
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
    console.log('Starting enhanced application initialization...');

    // Initialize mobile detection and responsive behavior
    initializeMobileSupport();

    // Initialize navigation system
    initializeNavigation();

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

    // Try to load initial data (non-blocking)
    loadInitialData().catch(error => {
        console.warn('Initial data loading failed:', error);
        notificationManager.show('Some data may not be available', 'warning');
    });

    console.log('Enhanced application initialization completed');
}

// Mobile Support Enhancement
function initializeMobileSupport() {
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    const isSmallScreen = window.innerWidth < 1024;

    if (isMobile || isSmallScreen) {
        document.body.classList.add('mobile-device');
        initializeMobileMenu();
        initializeTouchGestures();
    }

    // Handle orientation changes
    window.addEventListener('orientationchange', () => {
        setTimeout(() => {
            // Recalculate layouts after orientation change
            window.dispatchEvent(new Event('resize'));
        }, 100);
    });
}

function initializeMobileMenu() {
    const mobileToggle = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');

    if (mobileToggle && sidebar) {
        mobileToggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            mobileToggle.querySelector('i').classList.toggle('fa-bars');
            mobileToggle.querySelector('i').classList.toggle('fa-times');
        });

        // Close mobile menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target)) {
                sidebar.classList.remove('open');
                mobileToggle.querySelector('i').classList.add('fa-bars');
                mobileToggle.querySelector('i').classList.remove('fa-times');
            }
        });
    }
}

// Keyboard Shortcuts
function initializeKeyboardShortcuts() {
    const shortcuts = {
        'ctrl+/': () => showKeyboardShortcuts(),
        'ctrl+r': (e) => { e.preventDefault(); refreshCurrentSection(); },
        'ctrl+n': (e) => { e.preventDefault(); createNewItem(); },
        'esc': () => closeModalsAndNotifications(),
        'ctrl+1': () => switchToSection('dashboard'),
        'ctrl+2': () => switchToSection('testcases'),
        'ctrl+3': () => switchToSection('execution'),
        'ctrl+4': () => switchToSection('reports')
    };

    document.addEventListener('keydown', (e) => {
        const key = `${e.ctrlKey ? 'ctrl+' : ''}${e.shiftKey ? 'shift+' : ''}${e.altKey ? 'alt+' : ''}${e.key.toLowerCase()}`;

        if (shortcuts[key]) {
            shortcuts[key](e);
        }
    });
}

// Accessibility Features
function initializeAccessibility() {
    // Enhanced focus management
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Tab') {
            document.body.classList.add('keyboard-navigation');
        }
    });

    document.addEventListener('mousedown', () => {
        document.body.classList.remove('keyboard-navigation');
    });

    // Screen reader announcements
    const announcer = document.createElement('div');
    announcer.setAttribute('aria-live', 'polite');
    announcer.setAttribute('aria-atomic', 'true');
    announcer.className = 'sr-only';
    announcer.id = 'screen-reader-announcer';
    document.body.appendChild(announcer);

    window.announceToScreenReader = (message) => {
        announcer.textContent = message;
    };
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

// Enhanced Navigation Management
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
        const targetSection = document.getElementById(sectionName);
        if (targetSection) targetSection.classList.add('active');

        // Update page title
        const titles = {
            'dashboard': 'Dashboard',
            'testcases': 'Test Cases',
            'execution': 'Test Execution',
            'schedules': 'Schedules',
            'reports': 'Reports',
            'analytics': 'Analytics'
        };
        const titleElement = document.getElementById('page-title');
        if (titleElement) titleElement.textContent = titles[sectionName];

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
        case 'schedules':
            await loadSchedules();
            break;
        case 'reports':
            await loadReports();
            break;
        case 'analytics':
            // Analytics loads on demand
            break;
    }
}

// Data Loading Functions
async function loadAllData() {
    try {
        await Promise.all([
            loadDashboardData(),
            loadTestCases(),
            loadExecutions(),
            loadSchedules(),
            loadReports()
        ]);
    } catch (error) {
        console.error('Error loading data:', error);
        notificationManager.show('Some data failed to load', 'warning');
    }
}

async function loadDashboardData() {
    try {
        const [testCases, executions, schedules, batches] = await Promise.all([
            ApiClient.get('/testcases'),
            ApiClient.get('/execution/executions'),
            ApiClient.get('/schedules'),
            ApiClient.get('/execution/batches')
        ]);

        updateDashboardStats(testCases, executions, schedules);
        updateRecentBatches(batches);

    } catch (error) {
        console.error('Error loading dashboard data:', error);
        notificationManager.show('Failed to load dashboard data', 'error');
    }
}

function updateDashboardStats(testCases, executions, schedules) {
    const totalTestCasesEl = document.getElementById('total-testcases');
    const totalExecutionsEl = document.getElementById('total-executions');
    const activeSchedulesEl = document.getElementById('active-schedules');
    const successRateEl = document.getElementById('success-rate');

    if (totalTestCasesEl) totalTestCasesEl.textContent = testCases.length || 0;
    if (totalExecutionsEl) totalExecutionsEl.textContent = executions.length || 0;
    if (activeSchedulesEl) activeSchedulesEl.textContent = schedules.filter(s => s.enabled).length || 0;

    // Calculate success rate
    const successfulExecutions = executions.filter(e => e.status === 'PASSED').length;
    const successRate = executions.length > 0 ? Math.round((successfulExecutions / executions.length) * 100) : 0;
    if (successRateEl) successRateEl.textContent = `${successRate}%`;
}

function updateRecentBatches(batches) {
    const tbody = document.getElementById('recent-batches-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!batches || batches.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-gray-400 py-8">No recent batches found</td></tr>';
        return;
    }

    // Show latest 10 batches
    batches.slice(0, 10).forEach(batch => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="font-mono text-sm">${batch.batchId}</td>
            <td><span class="status-badge status-${batch.status.toLowerCase()}">${batch.status}</span></td>
            <td><span class="env-badge env-${batch.environment}">${batch.environment}</span></td>
            <td>${formatDateTime(batch.startTime)}</td>
            <td>${formatDuration(batch.duration)}</td>
            <td class="action-buttons">
                <button class="action-btn" onclick="viewBatchDetails('${batch.batchId}')" title="View Details">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="action-btn" onclick="generateBatchReport('${batch.batchId}')" title="Generate Report">
                    <i class="fas fa-file-alt"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Test Cases Management
async function loadTestCases() {
    try {
        const testCases = await ApiClient.get('/testcases');
        updateTestCasesTable(testCases);
    } catch (error) {
        console.error('Error loading test cases:', error);
        notificationManager.show('Failed to load test cases', 'error');
    }
}

function updateTestCasesTable(testCases) {
    const tbody = document.getElementById('testcases-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!testCases || testCases.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-gray-400 py-8">No test cases found. <a href="#" onclick="showCreateTestCaseModal()" class="text-cyan-400 hover:underline">Create your first test case</a></td></tr>';
        return;
    }

    testCases.forEach(testCase => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="font-mono">${testCase.id}</td>
            <td class="font-medium">${testCase.name}</td>
            <td><span class="type-badge type-${testCase.testType?.toLowerCase() || 'unknown'}">${testCase.testType || 'N/A'}</span></td>
            <td><span class="priority-badge priority-${testCase.priority?.toLowerCase() || 'medium'}">${testCase.priority || 'MEDIUM'}</span></td>
            <td><span class="env-badge env-${testCase.environment || 'dev'}">${testCase.environment || 'dev'}</span></td>
            <td>${testCase.testSuite || 'default'}</td>
            <td><span class="status-badge status-${testCase.isActive ? 'active' : 'inactive'}">${testCase.isActive ? 'Active' : 'Inactive'}</span></td>
            <td class="action-buttons">
                <button class="action-btn" onclick="executeTestCase(${testCase.id})" title="Execute Test">
                    <i class="fas fa-play"></i>
                </button>
                <button class="action-btn" onclick="editTestCase(${testCase.id})" title="Edit Test">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn action-btn-danger" onclick="deleteTestCase(${testCase.id})" title="Delete Test">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Test Execution Management
async function loadExecutions() {
    try {
        const executions = await ApiClient.get('/execution/executions');
        updateExecutionsTable(executions);
    } catch (error) {
        console.error('Error loading executions:', error);
        notificationManager.show('Failed to load executions', 'error');
    }
}

function updateExecutionsTable(executions) {
    const tbody = document.getElementById('executions-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!executions || executions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-gray-400 py-8">No executions found. Execute a test to see results here.</td></tr>';
        return;
    }

    // Show latest 50 executions
    executions.slice(0, 50).forEach(execution => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="font-mono">${execution.id}</td>
            <td class="font-medium">${execution.testCase?.name || 'N/A'}</td>
            <td><span class="status-badge status-${execution.status?.toLowerCase() || 'unknown'}">${execution.status || 'UNKNOWN'}</span></td>
            <td><span class="env-badge env-${execution.environment || 'dev'}">${execution.environment || 'dev'}</span></td>
            <td>${formatDateTime(execution.startTime)}</td>
            <td>${formatDuration(execution.executionDuration)}</td>
            <td class="action-buttons">
                <button class="action-btn" onclick="viewExecutionDetails(${execution.id})" title="View Details">
                    <i class="fas fa-eye"></i>
                </button>
                ${execution.status === 'FAILED' ? `
                    <button class="action-btn" onclick="downloadExecutionLogs(${execution.id})" title="Download Logs">
                        <i class="fas fa-download"></i>
                    </button>
                ` : ''}
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Schedule Management
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
    const tbody = document.getElementById('schedules-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!schedules || schedules.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-gray-400 py-8">No schedules found. <a href="#" onclick="showCreateScheduleModal()" class="text-cyan-400 hover:underline">Create your first schedule</a></td></tr>';
        return;
    }

    schedules.forEach(schedule => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="font-mono">${schedule.id}</td>
            <td class="font-medium">${schedule.scheduleName}</td>
            <td class="font-mono text-sm">${schedule.cronExpression}</td>
            <td>${schedule.testSuite}</td>
            <td><span class="env-badge env-${schedule.environment}">${schedule.environment}</span></td>
            <td><span class="status-badge status-${schedule.enabled ? 'active' : 'inactive'}">${schedule.enabled ? 'Active' : 'Inactive'}</span></td>
            <td>${formatDateTime(schedule.nextExecutionTime)}</td>
            <td class="action-buttons">
                <button class="action-btn" onclick="toggleSchedule(${schedule.id}, ${!schedule.enabled})" title="${schedule.enabled ? 'Disable' : 'Enable'} Schedule">
                    <i class="fas fa-${schedule.enabled ? 'pause' : 'play'}"></i>
                </button>
                <button class="action-btn" onclick="executeScheduleNow(${schedule.id})" title="Execute Now">
                    <i class="fas fa-bolt"></i>
                </button>
                <button class="action-btn action-btn-danger" onclick="deleteSchedule(${schedule.id})" title="Delete Schedule">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Reports Management
async function loadReports() {
    try {
        const reports = await ApiClient.get('/reports/list');
        updateReportsTable(reports);
    } catch (error) {
        console.error('Error loading reports:', error);
        updateReportsTable([]);
    }
}

function updateReportsTable(reports) {
    const tbody = document.getElementById('reports-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (!reports || reports.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-gray-400 py-8">No reports generated yet. Execute some tests and generate reports to see them here.</td></tr>';
        return;
    }

    reports.forEach(report => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="font-mono text-sm">${report.filename}</td>
            <td class="font-mono text-sm">${report.batchId}</td>
            <td><span class="type-badge type-${report.type?.toLowerCase() || 'html'}">${report.type || 'HTML'}</span></td>
            <td>${formatFileSize(report.size)}</td>
            <td>${formatDateTime(report.createdAt)}</td>
            <td class="action-buttons">
                <button class="action-btn" onclick="downloadReport('${report.filename}')" title="Download Report">
                    <i class="fas fa-download"></i>
                </button>
                <button class="action-btn" onclick="previewReport('${report.filename}')" title="Preview Report">
                    <i class="fas fa-eye"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Form Handlers
function initializeFormHandlers() {
    // Test Case Creation Form
    const createTestCaseForm = document.getElementById('create-testcase-form');
    if (createTestCaseForm) {
        createTestCaseForm.addEventListener('submit', handleCreateTestCase);
    }

    // Schedule Creation Form
    const createScheduleForm = document.getElementById('create-schedule-form');
    if (createScheduleForm) {
        createScheduleForm.addEventListener('submit', handleCreateSchedule);
    }

    // Batch Execution Form
    const batchExecutionForm = document.getElementById('batch-execution-form');
    if (batchExecutionForm) {
        batchExecutionForm.addEventListener('submit', handleBatchExecution);
    }

    // Single Test Execution Form
    const singleExecutionForm = document.getElementById('single-execution-form');
    if (singleExecutionForm) {
        singleExecutionForm.addEventListener('submit', handleSingleExecution);
    }

    // Search and Filter Handlers
    setupSearchAndFilters();
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

    const filterSelects = document.querySelectorAll('#testcase-filter-type, #testcase-filter-env');
    filterSelects.forEach(select => {
        select.addEventListener('change', filterTestCases);
    });
}

// Form Submit Handlers
async function handleCreateTestCase(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const testCaseData = {};

    for (let [key, value] of formData.entries()) {
        if (key === 'testData') {
            // Validate JSON format but keep it as string for backend
            if (value && value.trim()) {
                try {
                    JSON.parse(value); // Validate JSON format
                    testCaseData[key] = value; // Send as string
                } catch (e) {
                    notificationManager.show('Invalid JSON in test data field', 'error');
                    return;
                }
            } else {
                testCaseData[key] = '{}'; // Default empty JSON object as string
            }
        } else if (key === 'testSteps') {
            // Handle testSteps similarly - validate JSON but send as string
            if (value && value.trim()) {
                try {
                    JSON.parse(value); // Validate JSON format
                    testCaseData[key] = value; // Send as string
                } catch (e) {
                    notificationManager.show('Invalid JSON in test steps field', 'error');
                    return;
                }
            } else {
                testCaseData[key] = '[]'; // Default empty array as string
            }
        } else if (key === 'isActive') {
            testCaseData[key] = value === 'true';
        } else {
            testCaseData[key] = value;
        }
    }

    try {
        loadingManager.show('Creating test case...');
        await ApiClient.post('/testcases', testCaseData);

        notificationManager.show('✅ Test case created successfully!', 'success');
        closeModal('testcase-modal');
        event.target.reset();

        await loadTestCases();
        await loadDashboardData();

    } catch (error) {
        console.error('Error creating test case:', error);
        notificationManager.show('❌ Failed to create test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleCreateSchedule(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const scheduleData = {};

    for (let [key, value] of formData.entries()) {
        if (key === 'parallelThreads') {
            scheduleData[key] = parseInt(value);
        } else if (key === 'enabled') {
            scheduleData[key] = value === 'true';
        } else {
            scheduleData[key] = value;
        }
    }

    scheduleData.enabled = true;

    try {
        loadingManager.show('Creating schedule...');
        await ApiClient.post('/schedules', scheduleData);

        notificationManager.show('✅ Schedule created successfully!', 'success');
        closeModal('schedule-modal');
        event.target.reset();

        await loadSchedules();
        await loadDashboardData();

    } catch (error) {
        console.error('Error creating schedule:', error);
        notificationManager.show('❌ Failed to create schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleBatchExecution(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const executionData = {
        testSuite: formData.get('testSuite'),
        environment: formData.get('environment'),
        parallelThreads: parseInt(formData.get('parallelThreads'))
    };

    try {
        loadingManager.show('Executing test batch...');
        const result = await ApiClient.post('/execution/batch', executionData);

        notificationManager.show(`🚀 Batch execution started! Batch ID: ${result.batchId}`, 'success');

        // Start polling for execution status
        startExecutionStatusPolling(result.batchId);

        // Refresh executions
        setTimeout(() => {
            loadExecutions();
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing batch:', error);
        notificationManager.show('❌ Failed to execute batch', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function handleSingleExecution(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const testCaseId = formData.get('testCaseId');
    const environment = formData.get('environment');

    try {
        loadingManager.show('Executing test case...');
        const result = await ApiClient.post(`/execution/single/${testCaseId}?environment=${environment}`, {});

        notificationManager.show(`🚀 Test execution started! Test Case ID: ${testCaseId}`, 'success');

        setTimeout(() => {
            loadExecutions();
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing test case:', error);
        notificationManager.show('❌ Failed to execute test case', 'error');
    } finally {
        loadingManager.hide();
    }
}

// Action Functions
async function executeTestCase(testCaseId) {
    const environment = prompt('Enter environment (dev/staging/production):', 'dev');
    if (!environment) return;

    try {
        loadingManager.show('Executing test case...');
        const result = await ApiClient.post(`/execution/single/${testCaseId}?environment=${environment}`, {});

        notificationManager.show(`🚀 Test execution started! Test Case ID: ${testCaseId}`, 'success');

        setTimeout(() => {
            loadExecutions();
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
    if (!confirm('Are you sure you want to delete this test case? This action cannot be undone.')) {
        return;
    }

    try {
        loadingManager.show('Deleting test case...');
        await ApiClient.delete(`/testcases/${testCaseId}`);

        notificationManager.show('✅ Test case deleted successfully!', 'success');

        await loadTestCases();
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
        await ApiClient.patch(`/schedules/${scheduleId}/status`, { enabled });
        notificationManager.show(`✅ Schedule ${enabled ? 'enabled' : 'disabled'} successfully!`, 'success');
        await loadSchedules();
        await loadDashboardData();

    } catch (error) {
        console.error('Error toggling schedule:', error);
        notificationManager.show('❌ Failed to update schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function deleteSchedule(scheduleId) {
    if (!confirm('Are you sure you want to delete this schedule? This action cannot be undone.')) {
        return;
    }

    try {
        loadingManager.show('Deleting schedule...');
        await ApiClient.delete(`/schedules/${scheduleId}`);

        notificationManager.show('✅ Schedule deleted successfully!', 'success');

        await loadSchedules();
        await loadDashboardData();

    } catch (error) {
        console.error('Error deleting schedule:', error);
        notificationManager.show('❌ Failed to delete schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function executeScheduleNow(scheduleId) {
    try {
        loadingManager.show('Executing schedule...');
        const result = await ApiClient.post(`/schedules/${scheduleId}/execute`);

        notificationManager.show(`🚀 Schedule executed! Batch ID: ${result.batchId}`, 'success');

        setTimeout(() => {
            loadExecutions();
            loadDashboardData();
        }, 2000);

    } catch (error) {
        console.error('Error executing schedule:', error);
        notificationManager.show('❌ Failed to execute schedule', 'error');
    } finally {
        loadingManager.hide();
    }
}

async function generateBatchReport(batchId) {
    try {
        loadingManager.show('Generating batch report...');
        await ApiClient.post(`/reports/html/${batchId}`);

        notificationManager.show('✅ Batch report generated successfully!', 'success');

        await loadReports();

    } catch (error) {
        console.error('Error generating batch report:', error);
        notificationManager.show('❌ Failed to generate batch report', 'error');
    } finally {
        loadingManager.hide();
    }
}

function viewBatchDetails(batchId) {
    notificationManager.show(`Viewing details for batch: ${batchId}`, 'info');
}

function viewExecutionDetails(executionId) {
    notificationManager.show(`Viewing details for execution: ${executionId}`, 'info');
}

function downloadExecutionLogs(executionId) {
    notificationManager.show(`Downloading logs for execution: ${executionId}`, 'info');
}

function editTestCase(testCaseId) {
    notificationManager.show(`Edit functionality not implemented for test case: ${testCaseId}`, 'warning');
}

function downloadReport(filename) {
    notificationManager.show(`Download functionality not implemented for: ${filename}`, 'warning');
}

function previewReport(filename) {
    notificationManager.show(`Preview functionality not implemented for: ${filename}`, 'warning');
}

// Utility Functions
function filterTestCases() {
    const searchTerm = document.getElementById('testcase-search')?.value?.toLowerCase() || '';
    const typeFilter = document.getElementById('testcase-filter-type')?.value || '';
    const envFilter = document.getElementById('testcase-filter-env')?.value || '';

    const rows = document.querySelectorAll('#testcases-body tr');

    rows.forEach(row => {
        const cells = row.querySelectorAll('td');
        if (cells.length < 5) return;

        const name = cells[1]?.textContent?.toLowerCase() || '';
        const type = cells[2]?.textContent?.toLowerCase() || '';
        const env = cells[4]?.textContent?.toLowerCase() || '';

        const matchesSearch = !searchTerm || name.includes(searchTerm);
        const matchesType = !typeFilter || type.includes(typeFilter.toLowerCase());
        const matchesEnv = !envFilter || env.includes(envFilter.toLowerCase());

        row.style.display = matchesSearch && matchesType && matchesEnv ? '' : 'none';
    });
}

function formatDateTime(timestamp) {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString();
}

function formatDuration(milliseconds) {
    if (!milliseconds || milliseconds === 0) return '0s';

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
}

function formatFileSize(bytes) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function setDefaultFormValues() {
    const now = new Date();
    const oneWeekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    const fromDateInput = document.getElementById('trend-from-date');
    const toDateInput = document.getElementById('trend-to-date');

    if (fromDateInput) fromDateInput.value = oneWeekAgo.toISOString().slice(0, 16);
    if (toDateInput) toDateInput.value = now.toISOString().slice(0, 16);
}

// Modal Functions
function showCreateTestCaseModal() {
    const modal = document.getElementById('testcase-modal');
    if (modal) modal.style.display = 'flex';
}

function showCreateScheduleModal() {
    const modal = document.getElementById('schedule-modal');
    if (modal) modal.style.display = 'flex';
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.style.display = 'none';
}

// API Status Check
async function checkApiStatus() {
    const statusIndicator = document.getElementById('api-status');
    const statusText = document.getElementById('status-text');

    try {
        await ApiClient.get('/validation/health');
        if (statusIndicator) statusIndicator.className = 'status-indicator status-online';
        if (statusText) {
            statusText.textContent = 'API Online';
            statusText.className = 'text-sm font-medium text-green-400';
        }
    } catch (error) {
        if (statusIndicator) statusIndicator.className = 'status-indicator status-offline';
        if (statusText) {
            statusText.textContent = 'API Offline';
            statusText.className = 'text-sm font-medium text-red-400';
        }
    }
}

// Enhanced Auto-refresh Setup
function setupSmartRefresh() {
    const refreshRate = {
        'dashboard': 30000, // 30 seconds
        'execution': 15000, // 15 seconds
        'default': 60000 // 1 minute
    };

    refreshInterval = setInterval(async () => {
        try {
            if (currentSection === 'dashboard') {
                await loadDashboardData();
            } else if (currentSection === 'execution') {
                await loadExecutions();
            }

            await checkApiStatus();
        } catch (error) {
            console.debug('Auto-refresh error:', error);
        }
    }, refreshRate[currentSection] || refreshRate['default']);
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
                    loadExecutions();
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

// Click outside modal to close
window.addEventListener('click', (event) => {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
});

// Additional utility functions that might be missing
function showElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) element.style.display = 'block';
}

function hideElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) element.style.display = 'none';
}

function toggleElement(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.style.display = element.style.display === 'none' ? 'block' : 'none';
    }
}

// Handle API errors gracefully
function handleApiError(error, context = 'operation') {
    console.error(`API Error in ${context}:`, error);

    if (error.message && error.message.includes('fetch')) {
        notificationManager.show(`Connection failed during ${context}. Please check your network.`, 'error');
    } else {
        notificationManager.show(`Failed to perform ${context}. Please try again.`, 'error');
    }
}

// Populate test case dropdown for single execution
async function populateTestCaseDropdown() {
    try {
        const testCases = await ApiClient.get('/testcases');
        const dropdown = document.getElementById('single-testcase-id');

        if (dropdown && testCases) {
            dropdown.innerHTML = '<option value="">Select a test case</option>';
            testCases.forEach(testCase => {
                const option = document.createElement('option');
                option.value = testCase.id;
                option.textContent = `${testCase.name} (${testCase.testType})`;
                dropdown.appendChild(option);
            });
        }
    } catch (error) {
        console.warn('Failed to populate test case dropdown:', error);
    }
}

// Initialize tooltips and other UI enhancements
function initializeUIEnhancements() {
    // Add loading states to buttons
    const buttons = document.querySelectorAll('.btn-modern, .action-btn');
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            this.classList.add('loading');
            setTimeout(() => {
                this.classList.remove('loading');
            }, 2000);
        });
    });
}

// Ensure all referenced functions exist
window.loadDashboardData = loadDashboardData;
window.showCreateTestCaseModal = showCreateTestCaseModal;
window.showCreateScheduleModal = showCreateScheduleModal;
window.closeModal = closeModal;
window.executeTestCase = executeTestCase;
window.editTestCase = editTestCase;
window.deleteTestCase = deleteTestCase;
window.viewBatchDetails = viewBatchDetails;
window.generateBatchReport = generateBatchReport;
window.viewExecutionDetails = viewExecutionDetails;
window.downloadExecutionLogs = downloadExecutionLogs;
window.toggleSchedule = toggleSchedule;
window.executeScheduleNow = executeScheduleNow;
window.deleteSchedule = deleteSchedule;
window.downloadReport = downloadReport;
window.previewReport = previewReport;

