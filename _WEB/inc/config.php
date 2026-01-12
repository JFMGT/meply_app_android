<?php
require_once(__DIR__ . '/../../strapi_secrets.php');
const STRAPI_API_BASE = 'https://admin.meeplemates.de/api';
const STRAPI_IMAGE_BASE = 'https://admin.meeplemates.de';
const WEBSITE_BASE = 'https://dev.meply.de/';
const DEBUG_LOG_FILE = __DIR__ . '/../logs/errorlog.txt';
const INCLUDE_DIR = __DIR__ . '/';
define('WEBSITE_BASE', 'https://dev.meply.de');
define('MAIL_FROM', 'Meply <no-reply@meply.de>');
define('MAIL_REPLY_TO', 'support@meply.de');


ini_set('session.cookie_secure', 1);      // Nur über HTTPS senden
ini_set('session.cookie_httponly', 1);    // Kein JavaScript-Zugriff
ini_set('session.use_strict_mode', 1);    // Schutz vor Session-Fixation
ini_set('session.cookie_samesite', 'Strict'); // CSRF-Schutz via SameSite-Cookie

function debugLog($message) {
    $timestamp = date('Y-m-d H:i:s');
    $fullMessage = "[$timestamp] $message\n";
    error_log($fullMessage, 3, DEBUG_LOG_FILE);
}