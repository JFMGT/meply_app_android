/**
 * CSRF Protection - Automatic Token Handling
 *
 * Automatically adds CSRF token to all fetch() requests
 * Reads token from <meta name="csrf-token"> in document head
 *
 * @author Claude
 * @version 1.0
 * @date 2025-12-29
 */

(function() {
    'use strict';

    /**
     * Get CSRF token from meta tag
     * @returns {string|null} The CSRF token or null if not found
     */
    function getCsrfToken() {
        const meta = document.querySelector('meta[name="csrf-token"]');
        return meta ? meta.getAttribute('content') : null;
    }

    /**
     * Check if URL is a same-origin request
     * @param {string} url - The URL to check
     * @returns {boolean} True if same origin
     */
    function isSameOrigin(url) {
        // Relative URLs are always same-origin
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            return true;
        }

        // Compare origins
        try {
            const urlObj = new URL(url, window.location.origin);
            return urlObj.origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    /**
     * Override native fetch to add CSRF token automatically
     */
    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
        // Only add CSRF token for same-origin POST/PUT/DELETE/PATCH requests
        const method = (options.method || 'GET').toUpperCase();
        const needsCsrf = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method);

        if (needsCsrf && isSameOrigin(url)) {
            const token = getCsrfToken();

            if (token) {
                // Initialize headers if not exists
                options.headers = options.headers || {};

                // Handle different header types
                if (options.headers instanceof Headers) {
                    options.headers.append('X-CSRF-Token', token);
                } else if (typeof options.headers === 'object') {
                    options.headers['X-CSRF-Token'] = token;
                }

                // For JSON requests, also add to body if it's an object
                if (options.body && typeof options.body === 'string') {
                    try {
                        const bodyObj = JSON.parse(options.body);
                        bodyObj.csrf_token = token;
                        options.body = JSON.stringify(bodyObj);
                    } catch (e) {
                        // Body is not JSON, skip
                    }
                }

                // For FormData, add token field
                if (options.body instanceof FormData) {
                    options.body.append('csrf_token', token);
                }
            } else {
                console.warn('[CSRF] Token not found in meta tag. Ensure csrfTokenMeta() is called in header.');
            }
        }

        return originalFetch(url, options);
    };

    /**
     * Global helper function to get CSRF token (for manual use)
     */
    window.getCsrfToken = getCsrfToken;

    /**
     * Handle 403 CSRF errors globally
     */
    window.addEventListener('unhandledrejection', function(event) {
        if (event.reason && event.reason.status === 403) {
            console.error('[CSRF] Request blocked. Token may be invalid or missing.');
        }
    });

    console.log('[CSRF Protection] Initialized - All fetch() requests will include CSRF token');

})();
