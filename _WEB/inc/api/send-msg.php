<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// --- 1. SICHERHEIT: Nur POST erlauben (war bereits gut) ---
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405); // Method Not Allowed
    header('Allow: POST');
    echo json_encode(['success' => false, 'error' => 'Nur POST erlaubt']);
    exit;
}

// --- 1.5 SICHERHEIT: CSRF-Token validieren ---
requireCsrfToken();

// --- 2. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Nicht autorisiert']);
    exit;
}
$jwt = $_SESSION['jwt']; // JWT ist gültig

// --- 3. Eingabe holen und 'dates'-Logik (Logik war gut) ---
$input = json_decode(file_get_contents('php://input'), true);

// Debug-Log-Zeilen entfernt (waren nicht nötig)

// Prüfen, ob Nachricht übergeben wurde
if (empty($input['message'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Nachricht fehlt']);
    exit;
}

// Entscheidung: Neue Konversation oder bestehende? (Logik war gut)
$conversationId = $input['conversation'] ?? $input['conversationId'] ?? null;

if (!empty($conversationId)) {
    // Antwort auf bestehende Konversation
    $payload = [
        'conversationId' => $conversationId,
        'message' => $input['message'],
    ];
    $apiUrl =  STRAPI_API_BASE . '/messages';
} else {
    // Neue Konversation → Empfänger ist Pflicht
    if (empty($input['recipient'])) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'Empfänger fehlt']);
        exit;
    }
    $payload = [
        'recipient' => $input['recipient'],
        'message' => $input['message'],
    ];
    if (!empty($input['reference'])) {
        $payload['reference'] = $input['reference'];
    }
    $apiUrl =  STRAPI_API_BASE . '/conversations/create';
}

// --- 4. Manuelles cURL (Beibehalten) mit Fehlerbehandlung ---
debugLog("Send Message API URL: " . $apiUrl); // Optional
$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $jwt
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 10); // Timeout hinzugefügt
$response = curl_exec($ch);

// --- 5. ROBUSTHEIT: Fehlerbehandlung hinzugefügt ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

debugLog("Send Message Response Code: " . $httpCode);
debugLog("Send Message cURL Error: " . ($curlError ?: 'Kein Fehler'));

if ($curlError) {
    http_response_code(502); // Bad Gateway
    debugLog("Send Message cURL Error: " . $curlError);
    echo json_encode(['success' => false, 'error' => 'Netzwerkfehler zur API.']);
    exit;
}

// --- 6. Antwort zurückgeben (Stil beibehalten, aber sicherer) ---
http_response_code($httpCode); // Setze den von Strapi erhaltenen Code

if ($httpCode >= 200 && $httpCode < 300) {
    echo $response; // Erfolg: Roh-Antwort (wie im Original)
} else {
    // Fehlerfall
    debugLog("Send Message API Error: HTTP {$httpCode} - " . $response);
    // SICHERHEITS-FIX: Info Disclosure verhindern
    $errorData = json_decode($response, true);
    $errorMsg = $errorData['error']['message'] ?? 'Fehler beim Senden der Nachricht.';
    echo json_encode(['success' => false, 'error' => $errorMsg]); // Nur saubere Fehlermeldung
}