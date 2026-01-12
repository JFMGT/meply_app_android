<?php
// VERBESSERTE register.php
// Nutzt jetzt den neuen Strapi /auth/register-with-code Endpoint

require_once '../../inc/functions.php';

header('Content-Type: application/json; charset=utf-8');

// ========================================
// Helfer-Funktion für JSON-Antworten
// ========================================

function respond($success, $message, $data = []) {
    echo json_encode(array_merge([
        'success' => $success,
        'message' => $message
    ], $data));
    exit;
}

// ========================================
// SECURITY: CSRF-Token Validierung
// ========================================

session_start();

// Nutze zentrale CSRF-Validierung
requireCsrfToken();

// ========================================
// SECURITY: Rate Limit Check
// ========================================

// Skip rate limiting in DEV if SECLOGIN_SECRET is not configured
if (defined('SECLOGIN_SECRET') && !empty(SECLOGIN_SECRET)) {
    $limitResult = checkRateLimit('register', 5, 3600); // Max 5 Registrierungen pro Stunde

    if (!$limitResult['success']) {
        debugLog("Rate Limit Check Failed: " . json_encode($limitResult));

        // Only block if it's a real rate limit issue (429), not a config error (500)
        if ($limitResult['code'] === 429) {
            respond(false, "Zu viele Registrierungsversuche. Bitte versuche es später.");
        }
        // For other errors (500, network), log but allow registration in DEV
        debugLog("Rate Limit skipped due to error code: " . $limitResult['code']);
    } else {
        $data = $limitResult['response'];
        if (($data['action'] ?? '') === 'block') {
            $reason = $data['reason'] ?? 'unknown';
            $until = $data['until'] ?? time() + 300;
            $minuten = ceil(($until - time()) / 60);

            if ($reason === 'geo') {
                respond(false, "Registrierung aus deiner Region derzeit nicht möglich.");
            } else {
                respond(false, "Zu viele Versuche. Bitte warte ca. $minuten Minute(n).");
            }
        }
    }
} else {
    // DEV Mode: SECLOGIN_SECRET not configured, skip rate limiting
    debugLog("Rate Limit Check skipped: SECLOGIN_SECRET not configured (DEV mode)");
}

// ========================================
// SECURITY: Input Validierung (Basis-Check)
// Detaillierte Validierung erfolgt in Strapi
// ========================================

$username = trim($_POST['username'] ?? '');
$email = trim($_POST['email'] ?? '');
$password = $_POST['password'] ?? '';
$password_confirm = $_POST['password_confirm'] ?? '';
$registration_key = trim($_POST['registration_key'] ?? '');
$privacy_confirm = $_POST['privacy_confirm'] ?? null;

// Basis-Validierung
if (empty($privacy_confirm)) {
    respond(false, "Bitte bestätige die Datenschutzerklärung.");
}

if (empty($username) || empty($email) || empty($password) || empty($registration_key)) {
    respond(false, "Bitte alle Pflichtfelder ausfüllen.");
}

if ($password !== $password_confirm) {
    respond(false, "Die Passwörter stimmen nicht überein.");
}

// ========================================
// Registrierung über Strapi Custom Endpoint
// ========================================

$url = STRAPI_API_BASE . "/auth/register-with-code";

$payload = [
    'username' => $username,
    'email' => $email,
    'password' => $password,
    'registrationCode' => $registration_key
];

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Content-Type: application/json"
    // KEIN Authorization Header - der Endpoint ist öffentlich
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 15);

$response = curl_exec($ch);

if (curl_errno($ch)) {
    debugLog("CURL Error in register.php: " . curl_error($ch));
    curl_close($ch);
    respond(false, "Verbindungsfehler. Bitte versuche es später erneut.");
}

$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

$responseData = json_decode($response, true);

// ========================================
// SECURITY: Logging (für Fehleranalyse)
// ========================================

if ($httpCode !== 200) {
    debugLog("Registration failed: HTTP $httpCode | Response: $response");
}

// ========================================
// Fehlerbehandlung
// ========================================

if ($httpCode !== 200 || !$responseData['success']) {
    // SECURITY: Generische Fehlermeldung
    // (Details nur im Log, nicht an User)
    $errorMsg = "Registrierung fehlgeschlagen. Bitte überprüfe deine Angaben.";
    
    // Falls Strapi eine spezifische (sichere) Nachricht mitgibt, verwende diese
    if (isset($responseData['message']) && !empty($responseData['message'])) {
        $errorMsg = $responseData['message'];
    }
    
    respond(false, $errorMsg);
}

// ========================================
// Erfolg - Bestätigungs-Email senden
// ========================================

$user = $responseData['user'];
$confirmationToken = $user['confirmationToken'];

// Email-Versand (bleibt vorerst in PHP)
if (!sendConfirmationMail($user['email'], $confirmationToken)) {
    debugLog("CRITICAL: Email sending failed for user {$user['id']} ({$user['email']})");
    respond(false, "Registrierung erfolgreich, aber die Bestätigungs-E-Mail konnte nicht gesendet werden. Bitte kontaktiere den Support.");
}

// ========================================
// Erfolgreiche Registrierung
// ========================================

debugLog("User registered successfully: {$user['id']} ({$user['email']})");

respond(
    true, 
    "Registrierung erfolgreich! Bitte überprüfe dein E-Mail-Postfach (auch den Spam-Ordner), um deinen Account zu bestätigen.",
    [
        'userId' => $user['id'] // Optional: Für Tracking
    ]
);