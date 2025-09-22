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
        const loadingId = loadingManager.show(`Loading ${sectionName}...`);

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

        // Show target section - check for both formats
        let targetSection = document.getElementById(sectionName + '-section');
        if (!targetSection) {
            targetSection = document.getElementById(sectionName);
        }

        if (targetSection) {
            targetSection.classList.add('active');
        } else {
            console.warn(`Section not found: ${sectionName}`);
            // Create a placeholder section if it doesn't exist
            createPlaceholderSection(sectionName);
        }

        currentSection = sectionName;

        // Stop any ongoing executions in the previous section
        if (executionStatusPolling.has(currentSection)) {
            clearInterval(executionStatusPolling.get(currentSection));
            executionStatusPolling.delete(currentSection);
        }

        // Start execution status polling for the new section, if applicable
        if (currentSection === 'execution') {
            startExecutionStatusPolling();
        }

        loadingManager.hide(loadingId);

    } catch (error) {
        console.error('Error switching section:', error);
        notificationManager.show('Failed to load section', 'error');
        loadingManager.hide(loadingId);
    }
}

function createPlaceholderSection(sectionName) {
    const placeholder = document.createElement('div');
    placeholder.className = 'content-section placeholder';
    placeholder.id = `placeholder-${sectionName}`;
    placeholder.innerHTML = `<p>No content available for ${sectionName}. Please check back later.</p>`;
    document.querySelector('.content-container').appendChild(placeholder);
}

// Smart Refresh Logic
function setupSmartRefresh() {
    const refreshConfig = {
        'dashboard': 30000,
        'testcases': 30000,
        'execution': 5000,
        'reports': 30000
    };

    function getRefreshInterval(section) {
        return refreshConfig[section] || 30000;
    }

    async function refreshData() {
        if (!currentSection || !refreshConfig[currentSection]) return;

        try {
            const interval = getRefreshInterval(currentSection);
            console.log(`Refreshing data for ${currentSection}...`);

            // Add your data fetching and refreshing logic here
            await fetchDataForSection(currentSection);

            // Schedule next refresh
            refreshInterval = setTimeout(refreshData, interval);
        } catch (error) {
            console.error('Error refreshing data:', error);
            notificationManager.show('Error refreshing data', 'error');
        }
    }

    // Start the initial refresh
    refreshData();
}

// Placeholder for actual data fetching logic
async function fetchDataForSection(section) {
    // Implement your data fetching logic here
    // Example:
    // if (section === 'dashboard') {
    //     await ApiClient.get('/dashboard/data');
    // } else if (section === 'testcases') {
    //     await ApiClient.get('/testcases/data');
    // }
}

// Execution Status Polling
function startExecutionStatusPolling() {
    const section = 'execution';
    const interval = 5000;

    if (parallelExecutionMonitor) {
        clearInterval(parallelExecutionMonitor);
    }

    parallelExecutionMonitor = setInterval(async () => {
        try {
            console.log('Polling execution status...');
            // Fetch and update execution status
            await updateExecutionStatus();
        } catch (error) {
            console.error('Error polling execution status:', error);
            notificationManager.show('Error polling execution status', 'error');
        }
    }, interval);
}

async function updateExecutionStatus() {
    // Implement your execution status updating logic here
    // Example:
    // const status = await ApiClient.get('/execution/status');
    // document.getElementById('execution-status').textContent = status;
}

// Enhanced Error Boundary
function setupErrorBoundary() {
    window.addEventListener('error', (event) => {
        event.preventDefault();
        notificationManager.show('An unexpected error occurred. Please try again later.', 'error');
        return true;
    });

    window.addEventListener('unhandledrejection', (event) => {
        event.preventDefault();
        notificationManager.show('A background task failed. Please try again later.', 'error');
        return true;
    });
}

// Initialize application enhancements
function initializeEnhancements() {
    // Add any additional initialization logic for enhancements here
    console.log('Initializing application enhancements...');
}

// Call this function to initialize all enhancements
function initializeAll() {
    initializeEnhancements();
    setupErrorBoundary();
}

// Start the application
initializeAll();
