<?php
// ARCHITEKTUR: Die Session wird in functions.php/config.php global gestartet.
require_once('../functions.php'); // Lädt Helfer, requireLogin, config.php
requireLogin(); // Erzwingt gültigen Login

// ARCHITEKTUR: Header zuerst senden und unnötige Includes entfernen
header('Content-Type: application/json');
// include("../../inc/header_auth.php"); // KRITISCH: ENTFERNT, da es HTML/Text ausgeben könnte!

// --- Logging Start ---
debugLog("--- follow_status.php: Skript gestartet ---");

// 1. Session-Daten holen (Sicher nach requireLogin)
$targetDocumentId = $_GET['target'] ?? null;

// Die Prüfung auf $token und $currentDocumentId ist nun durch requireLogin() abgedeckt.

// 2. Eingabe prüfen
if (!$targetDocumentId) {
    http_response_code(400);
    debugLog("follow_status.php: FEHLER - Ziel-userDocumentId fehlt.");
    echo json_encode(['success' => false, 'error' => 'Ziel-userDocumentId fehlt']);
    exit;
}

// 3. API-Aufruf mit Helfer (DRY & Robustheit)
// HINWEIS: Nutzt den /followers/followedby/{userA}/{userB} Endpunkt.
// 'current' wird im Backend durch das JWT des Users ersetzt.
$apiUrl = STRAPI_API_BASE . "/followers/followedby/current/" . urlencode($targetDocumentId);

$result = makeStrapiApiCall('GET', $apiUrl); // Helfer holt JWT

// 4. Konsistente Antwort
if ($result['success']) {
    // Erfolgreiche Weiterleitung der Strapi-Antwort
    http_response_code($result['code']);
    echo json_encode($result['response']); // Gibt die decodierte JSON-Antwort zurück
} else {
    // Fehlerfall (Netzwerkfehler, 401, 403, 404, 500)
    $errorCode = $result['code'] ?: 500; // Wenn 0 (cURL), setze 500
    http_response_code($errorCode);
    
    // Logge den Fehler
    debugLog("follow_status.php API Error: HTTP {$result['code']} - " . json_encode($result['response']));

    // Gib eine generische Fehlermeldung aus
    echo json_encode(['success' => false, 'error' => 'Fehler beim Abrufen des Follow-Status.']);
}
?>