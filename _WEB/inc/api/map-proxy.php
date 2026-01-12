<?php
// ARCHITEKTUR: Session ZENTRAL starten (falls für Helfer benötigt)
// session_start();

// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, debugLog etc.

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// Nur GET zulassen
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success' => false, 'error' => 'Method not allowed']);
    exit;
}

// --- 1. SICHERHEIT: Whitelisting erlaubter Parameter ---
$allowedParams = [
    'filters',    // Erlaube Filter (z.B. nach Typ)
    'populate',   // Populate relations
    'fields',     // Select specific fields
    'sort',       // Sortierung
    'pagination', // Pagination
];

$params = [];
foreach ($_GET as $key => $value) {
    // Einfache Prüfung: Ist der Parameter (oder sein Hauptteil vor '[') erlaubt?
    $baseKey = strtok($key, '[');
    if (in_array($baseKey, $allowedParams)) {
        // WICHTIG: Hier findet KEINE tiefgreifende Validierung des *Wertes* statt.
        // Die Sicherheit hängt davon ab, dass Strapi die Filter sicher verarbeitet.
        $params[$key] = $value;
    } else {
        // Logge den Versuch, einen unerlaubten Parameter zu verwenden
        debugLog("API Proxy Warning: Disallowed parameter attempted: " . $key);
        // Ignoriere den unerlaubten Parameter
    }
}

// --- 2. API-Aufruf mit PUBLIC Route ---
// WICHTIG: Nutze die neue /locations/public Route!
// Diese Route benötigt KEINE Authentifizierung und gibt nur Published Locations zurück
$apiUrl = STRAPI_API_BASE . '/locations/public?' . http_build_query($params);
debugLog("API Proxy URL: " . $apiUrl);

// Verwende PUBLIC API Call (kein Token nötig)
$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_errno($ch);
curl_close($ch);

// --- 3. Ergebnis verarbeiten ---
if ($curlError) {
    // cURL Fehler
    http_response_code(500);
    debugLog("API Proxy cURL Error: " . $curlError);
    echo json_encode(['success' => false, 'error' => 'API nicht erreichbar']);
    exit;
}

if ($httpCode >= 200 && $httpCode < 300) {
    // Erfolg: Gib die Strapi-Antwort direkt durch
    http_response_code($httpCode);
    echo $response; // Gibt data + meta zurück
} else {
    // Fehlerfall (4xx, 5xx von Strapi)
    http_response_code($httpCode);
    debugLog("API Proxy Error: HTTP {$httpCode} - {$response}");
    
    $responseData = json_decode($response, true);
    $errorMsg = 'Fehler bei der API-Anfrage';
    
    if (is_array($responseData) && isset($responseData['error']['message'])) {
        $errorMsg = $responseData['error']['message'];
    }
    
    echo json_encode(['success' => false, 'error' => $errorMsg]);
}