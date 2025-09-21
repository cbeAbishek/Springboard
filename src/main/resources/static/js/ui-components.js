// UI Components and Notification System Module

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
            this.overlay = document.getElementById('loading-overlay') || this.createOverlay();
        }
        return this.overlay;
    }

    createOverlay() {
        const overlay = document.createElement('div');
        overlay.id = 'loading-overlay';
        overlay.className = 'loading-overlay';
        overlay.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <div class="loading-text">Loading...</div>
                <div class="loading-progress">
                    <div class="loading-progress-bar" style="width: 0%"></div>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
        return overlay;
    }
}

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

// Modal Functions
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'flex';
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('modal-open');
    }
}

function closeModalsAndNotifications() {
    // Close all modals
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.style.display = 'none';
        modal.setAttribute('aria-hidden', 'true');
    });
    document.body.classList.remove('modal-open');

    // Close all notifications
    if (window.notificationManager) {
        notificationManager.closeAll();
    }
}

// Mobile Support Functions
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
            const icon = mobileToggle.querySelector('i');
            icon.classList.toggle('fa-bars');
            icon.classList.toggle('fa-times');
        });

        // Close mobile menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target)) {
                sidebar.classList.remove('open');
                const icon = mobileToggle.querySelector('i');
                icon.classList.add('fa-bars');
                icon.classList.remove('fa-times');
            }
        });
    }
}

function initializeTouchGestures() {
    // Add touch gesture support for mobile devices
    let startX = 0;
    let startY = 0;

    document.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
    });

    document.addEventListener('touchend', (e) => {
        const endX = e.changedTouches[0].clientX;
        const endY = e.changedTouches[0].clientY;
        const deltaX = endX - startX;
        const deltaY = endY - startY;

        // Detect swipe gestures
        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 50) {
            if (deltaX > 0) {
                // Swipe right - open sidebar
                const sidebar = document.querySelector('.sidebar');
                if (sidebar) sidebar.classList.add('open');
            } else {
                // Swipe left - close sidebar
                const sidebar = document.querySelector('.sidebar');
                if (sidebar) sidebar.classList.remove('open');
            }
        }
    });
}

// Accessibility Functions
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

// Initialize managers
const notificationManager = new NotificationManager();
const loadingManager = new LoadingManager();

// Export for global access
window.notificationManager = notificationManager;
window.loadingManager = loadingManager;
window.FormValidator = FormValidator;
window.showModal = showModal;
window.closeModal = closeModal;
window.closeModalsAndNotifications = closeModalsAndNotifications;
window.initializeMobileSupport = initializeMobileSupport;
window.initializeAccessibility = initializeAccessibility;
