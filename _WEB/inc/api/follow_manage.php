<?php
// ARCHITEKTUR: session_start() sollte global erfolgen.
session_start();
// ARCHITEKTUR: Abhängigkeiten ZENTRAL laden
require_once('../config.php');
require_once('../functions.php'); // Lädt debugLog, is_logged_in_explizit etc.

// CSRF-Token validieren
requireCsrfToken();

header('Content-Type: application/json');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
// Erzwingt, dass das JWT gültig ist (besser als nur die Existenz prüfen).
if (!is_logged_in_explizit()) { 
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Sitzung ungültig (Bitte neu einloggen)']);
    exit;
}

$jwt = $_SESSION['jwt']; 

// 2. Eingabe prüfen
$input = json_decode(file_get_contents('php://input'), true);
$followId = $input['id'] ?? null;
$action = $input['action'] ?? null;

$validActions = ['accepted', 'declined', 'remove'];

if (!$followId || !in_array($action, $validActions)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Ungültige Aktion oder fehlende ID']);
    exit;
}

// 3. API-Aufruf vorbereiten
// SICHERHEIT: ID für URL-Pfad encoden
$apiUrl = STRAPI_API_BASE . '/followers/manage/' . urlencode($followId);
$payload = json_encode(['action' => $action]);

// --- Manueller cURL-Block (wie gewünscht) ---
$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PATCH');
curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $jwt,
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);
// --- Ende Manueller cURL-Block ---

// 4. Konsistente Antwort und Fehlerbehandlung
if ($curlError) {
    // Fehlerfall (Netzwerkfehler)
    http_response_code(500);
    debugLog("Follow/Manage cURL Error: " . $curlError);
    echo json_encode(['success' => false, 'error' => 'Netzwerkfehler bei Anfrage an Strapi-API']);
    exit;
}

if ($httpCode >= 200 && $httpCode < 300) {
    // Erfolgreiche Weiterleitung der Strapi-Antwort
    http_response_code($httpCode);
    echo $response;
} else {
    // Fehlerfall (API-Fehler)
    http_response_code($httpCode);
    
    // Logge den Fehler
    debugLog("Follow/Manage API Error: HTTP {$httpCode}");

    // Versuche, die Fehlermeldung von Strapi durchzureichen
    $data = json_decode($response, true);
    $errorMsg = $data['message'] ?? $data['error']['message'] ?? 'Fehler bei der Anfrage';
    
    echo json_encode(['success' => false, 'error' => $errorMsg]);
}
?>