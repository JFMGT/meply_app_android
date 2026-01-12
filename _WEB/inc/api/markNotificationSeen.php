<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit etc.

// CSRF-Token validieren
requireCsrfToken();

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// 1. SICHERHEIT: Explizite Login-Prüfung
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Nicht autorisiert']);
    exit;
}
// JWT ist gültig und wird vom Helfer genutzt

// 2. Eingabe holen und validieren
$input = json_decode(file_get_contents('php://input'), true);
$notificationIdRaw = $input['id'] ?? null;

// Stärkere Validierung (Beispiel: Format prüfen)
if (empty($notificationIdRaw) /* || !isDocumentId($notificationIdRaw) */ ) { // isDocumentId() ggf. hinzufügen
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Keine oder ungültige ID übergeben']);
    exit;
}
// SICHERHEIT: ID für URL encoden
$notificationIdEncoded = urlencode($notificationIdRaw);

// 3. API-Aufruf mit Helfer (DRY & Robustheit)
$url = STRAPI_API_BASE . "/notifications/" . $notificationIdEncoded;
$payload = [
    'data' => [
        'seen' => true,
    ]
];

// Helfer nutzen ('PUT' oder 'PATCH' je nach API-Design)
$result = makeStrapiApiCall('PUT', $url, $payload);

// 4. Konsistente Antwort
if ($result['success']) {
    // Erfolg (Strapi gibt 200 OK zurück)
    echo json_encode(['success' => true]);
} else {
    // Fehlerfall (Netzwerk, 401, 403, 404, 500 etc.)
    $errorCode = $result['code'] ?: 500;
    http_response_code($errorCode);
    debugLog("Mark Notification Seen Error: ID {$notificationIdRaw}, HTTP {$result['code']} - " . json_encode($result));
    $errorMsg = 'Strapi-Aktualisierung fehlgeschlagen'; // Generische Meldung
     if (is_array($result['response'])) { // Spezifischere Meldung falls verfügbar
         $errorMsg = $result['response']['error']['message'] ?? $errorMsg;
     }
    echo json_encode(['success' => false, 'error' => $errorMsg]);
}

?> // Kein schließendes ?>