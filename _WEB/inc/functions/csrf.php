<?php
/**
 * CSRF Protection Helper Functions
 *
 * Provides centralized CSRF token generation and validation
 * to protect against Cross-Site Request Forgery attacks.
 *
 * @author Claude
 * @version 1.0
 * @date 2025-12-29
 */

/**
 * Generates a CSRF token and stores it in the session
 *
 * @return string The generated CSRF token
 */
function generateCsrfToken() {
    // Ensure session is started
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    // Generate token if not exists
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }

    return $_SESSION['csrf_token'];
}

/**
 * Retrieves the current CSRF token from session
 *
 * @return string|null The CSRF token or null if not set
 */
function getCsrfToken() {
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    return $_SESSION['csrf_token'] ?? null;
}

/**
 * Validates a CSRF token against the session token
 *
 * Uses timing-safe comparison to prevent timing attacks
 *
 * @param string $token The token to validate
 * @return bool True if valid, false otherwise
 */
function validateCsrfToken($token) {
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    // Check if session token exists
    if (empty($_SESSION['csrf_token'])) {
        return false;
    }

    // Timing-safe comparison
    return hash_equals($_SESSION['csrf_token'], $token);
}

/**
 * Middleware function to validate CSRF token from request
 *
 * Checks for token in:
 * 1. HTTP Header (X-CSRF-Token)
 * 2. POST data (csrf_token)
 * 3. JSON body (csrf_token)
 *
 * Sends 403 response and exits if validation fails
 *
 * @param bool $exit_on_failure Whether to exit on validation failure (default: true)
 * @return bool True if valid, false otherwise
 */
function requireCsrfToken($exit_on_failure = true) {
    $token = null;
    $source = 'none';

    // 1. Check HTTP Header (for AJAX/Fetch requests)
    if (isset($_SERVER['HTTP_X_CSRF_TOKEN'])) {
        $token = $_SERVER['HTTP_X_CSRF_TOKEN'];
        $source = 'header';
    }

    // 2. Check POST data (for traditional forms)
    elseif (isset($_POST['csrf_token'])) {
        $token = $_POST['csrf_token'];
        $source = 'post';
    }

    // 3. Check JSON body (for JSON API requests)
    else {
        $input = file_get_contents('php://input');
        if ($input) {
            $data = json_decode($input, true);
            if (isset($data['csrf_token'])) {
                $token = $data['csrf_token'];
                $source = 'json';
            }
        }
    }

    // DEBUG logging
    if (function_exists('debugLog')) {
        debugLog("CSRF Check - Source: $source, Token present: " . ($token ? 'yes' : 'no') .
                 ", Session token present: " . (isset($_SESSION['csrf_token']) ? 'yes' : 'no'));
    }

    // Validate token
    $isValid = $token && validateCsrfToken($token);

    if (!$isValid && $exit_on_failure) {
        if (function_exists('debugLog')) {
            debugLog("CSRF Validation FAILED - Token: " . substr($token ?? 'null', 0, 10) .
                     "..., Session: " . substr($_SESSION['csrf_token'] ?? 'null', 0, 10) . "...");
        }

        http_response_code(403);
        header('Content-Type: application/json');
        echo json_encode([
            'success' => false,
            'error' => 'CSRF-Token ungültig oder fehlt',
            'error_code' => 'CSRF_VALIDATION_FAILED'
        ]);
        exit;
    }

    return $isValid;
}

/**
 * Outputs a hidden input field with CSRF token
 *
 * For use in HTML forms
 *
 * @return void
 */
function csrfTokenField() {
    $token = generateCsrfToken();
    echo '<input type="hidden" name="csrf_token" value="' . htmlspecialchars($token, ENT_QUOTES, 'UTF-8') . '">';
}

/**
 * Returns a meta tag with CSRF token
 *
 * For use in HTML head section, accessible via JavaScript
 *
 * @return string The meta tag HTML
 */
function csrfTokenMeta() {
    $token = generateCsrfToken();
    return '<meta name="csrf-token" content="' . htmlspecialchars($token, ENT_QUOTES, 'UTF-8') . '">';
}
