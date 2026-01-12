<?php
// ARCHITEKTUR: session_start() sollte zentral sein (wird hier beibehalten)
session_start();
// ARCHITEKTUR: Abhängigkeiten ZENTRAL laden
require_once('../config.php');
require_once('../functions.php'); // Lädt Helfer, is_logged_in_explizit etc.

header('Content-Type: application/json');

// --- CSRF-Token validieren ---
requireCsrfToken();

// --- 1. SICHERHEIT: Explizite Login-Prüfung (ersetzt den einfachen JWT-Check) ---
// Nutzen Sie is_logged_in_explizit() (oder requireLogin()), um die JWT-Gültigkeit zu erzwingen.
if (!is_logged_in_explizit()) { 
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Sitzung ungültig (Bitte neu einloggen)']);
    exit;
}
$jwt = $_SESSION['jwt']; // JWT ist nun als gültig angenommen

// 2. Eingabe validieren
$input = json_decode(file_get_contents('php://input'), true);
$documentId = $input['documentId'] ?? null;

if (!$documentId) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'documentId fehlt']);
    exit;
}

// 3. API-Aufruf mit Helfer (DRY & Robustheit)
$apiUrl = STRAPI_API_BASE . '/followers/toggle';
$payload = ['documentId' => $documentId]; // Payload ist sicher

$result = makeStrapiApiCall('POST', $apiUrl, $payload); // Helfer holt JWT

// 4. Konsistente Fehlerbehandlung
if ($result['success']) {
    // Erfolgreiche Weiterleitung der Strapi-Antwort
    http_response_code($result['code']);
    // Gib die ursprüngliche (decodierte) Antwort als JSON aus
    echo json_encode($result['response']);
} else {
    // Fehlerfall (Netzwerkfehler, 403, 404, 500)
    $errorCode = $result['code'] ?: 500; // Wenn 0 (cURL), setze 500
    http_response_code($errorCode);
    
    // Logge den Fehler
    debugLog("Follow/Toggle API Error: HTTP {$result['code']} - " . json_encode($result));

    // Gib die Fehlermeldung von Strapi durch (sauberer)
    $errorMsg = $result['response']['error']['message'] ?? $result['response']['message'] ?? 'Fehler bei Anfrage an Strapi-API';

    echo json_encode([
        'success' => false,
        'error' => $errorMsg
    ]);
}
?>