// Core API Client and Utilities Module
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

// Utility Functions
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

// Connection Manager
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
            if (window.notificationManager) {
                notificationManager.show('Connection restored', 'success');
            }
        });

        window.addEventListener('offline', () => {
            this.isOnline = false;
            this.notifyListeners('offline');
            if (window.notificationManager) {
                notificationManager.show('Connection lost - Working offline', 'warning', { persistent: true });
            }
        });
    }

    startHealthCheck() {
        setInterval(async () => {
            try {
                await fetch('/api/validation/health', { method: 'HEAD', cache: 'no-store' });
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

// API Status Check
async function checkApiStatus() {
    const statusIndicator = document.getElementById('system-health-indicator');
    const statusText = document.getElementById('api-status-text');

    try {
        await ApiClient.get('/validation/health');
        if (statusIndicator) {
            const dot = statusIndicator.querySelector('.w-2.h-2');
            const text = statusIndicator.querySelector('.text-xs');
            if (dot) {
                dot.className = 'w-2 h-2 bg-green-400 rounded-full animate-pulse';
            }
            if (text) {
                text.textContent = 'Online';
                text.className = 'text-xs text-green-400';
            }
        }
        if (statusText) {
            statusText.textContent = 'Connected';
            statusText.className = 'text-green-400';
        }
    } catch (error) {
        if (statusIndicator) {
            const dot = statusIndicator.querySelector('.w-2.h-2');
            const text = statusIndicator.querySelector('.text-xs');
            if (dot) {
                dot.className = 'w-2 h-2 bg-red-400 rounded-full';
            }
            if (text) {
                text.textContent = 'Offline';
                text.className = 'text-xs text-red-400';
            }
        }
        if (statusText) {
            statusText.textContent = 'Disconnected';
            statusText.className = 'text-red-400';
        }
    }
}

// Export for global access
window.ApiClient = ApiClient;
window.ApiError = ApiError;
window.ValidationError = ValidationError;
window.connectionManager = connectionManager;
window.formatDateTime = formatDateTime;
window.formatDate = formatDate;
window.formatDuration = formatDuration;
window.formatFileSize = formatFileSize;
window.checkApiStatus = checkApiStatus;
