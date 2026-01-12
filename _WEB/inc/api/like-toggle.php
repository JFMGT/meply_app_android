<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
header('Content-Type: application/json; charset=utf-8');

// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit etc.

// CSRF-Token validieren
requireCsrfToken();

// 1. SICHERHEIT: Explizite Login-Prüfung
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode([
        'success' => false, // Konsistentes Format
        'error'   => 'Nicht eingeloggt',
        'message' => 'Du musst angemeldet sein, um liken zu können.'
    ]);
    exit;
}
// JWT ist gültig und wird vom Helfer genutzt

// 2. Eingabe validieren
$input = json_decode(file_get_contents('php://input'), true);
$targetDocumentId = $input['targetDocumentId'] ?? null;
$targetType = $input['targetType'] ?? null; // z.B. 'post', 'event'

// Stärkere Validierung (Beispiel)
if (empty($targetDocumentId) || empty($targetType) || !preg_match('/^[a-z0-9]+$/i', $targetDocumentId) || !preg_match('/^[a-z]+$/i', $targetType)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error'   => 'Fehlende oder ungültige Eingaben',
        'message' => 'Bitte Inhaltstyp und gültige documentId übergeben.'
    ]);
    exit;
}

// 3. API-Aufruf mit Helfer (DRY & Robustheit)
$strapiEndpoint = STRAPI_API_BASE . '/likes/toggle';
$payload = [
    'targetDocumentId' => $targetDocumentId,
    'targetType' => $targetType,
];

$result = makeStrapiApiCall('POST', $strapiEndpoint, $payload); // Helfer holt JWT

// 4. Konsistente Antwort
if ($result['success']) {
    // Erfolgreiche Weiterleitung der Strapi-Antwort
    http_response_code($result['code']); // Gibt 200 oder 201 zurück
    echo json_encode($result['response']); // Gibt die decodierte JSON-Antwort zurück
} else {
    // Fehlerfall (Netzwerk, 4xx, 5xx)
    $errorCode = $result['code'] ?: 500;
    http_response_code($errorCode);

    debugLog("Like/Toggle API Error: HTTP {$result['code']} - " . json_encode($result));

    // Gib die Fehlermeldung von Strapi durch
    $errorMsg = $result['response']['message'] ?? $result['response']['error']['message'] ?? 'Fehler bei der Like-Anfrage';

    echo json_encode([
        'success' => false,
        'error'   => $errorMsg
    ]);
}