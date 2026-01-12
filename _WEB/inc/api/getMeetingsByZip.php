<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
header('Content-Type: application/json; charset=utf-8');

// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, getMeetingsByZip, requireLogin etc.

// 1. Authentifizierung erzwingen
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'error' => 'Nicht autorisiert',
        'content' => null,
        'count' => 0
    ]);
    exit;
}

// 2. Eingabe holen und validieren
$zip = $_GET['zip'] ?? null;
$distance = $_GET['distance'] ?? null;

// Stärkere Validierung
$distanceFloat = filter_var($distance, FILTER_VALIDATE_FLOAT);

// Deutsche PLZ-Prüfung (5 Ziffern)
if (!$zip || !preg_match('/^\d{5}$/', $zip)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => 'Ungültige PLZ. Bitte gib eine 5-stellige deutsche PLZ ein.',
        'content' => null,
        'count' => 0
    ]);
    exit;
}

if ($distanceFloat === false || $distanceFloat < 0 || $distanceFloat > 200) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => 'Ungültige Distanz. Bitte gib eine Zahl zwischen 0 und 200 ein.',
        'content' => null,
        'count' => 0
    ]);
    exit;
}

// 3. Daten laden
// ✅ getMeetingsByZip gibt jetzt strukturierte Array zurück
$result = getMeetingsByZip($zip, (float)$distanceFloat);

// 4. Response aufbauen
$response = [
    'success' => $result['success'],
    'error' => $result['error'],
    'content' => $result['html'],
    'count' => $result['count']
];

// 5. HTTP Status Code setzen
if (!$result['success']) {
    // Fehler-Cases
    if (strpos($result['error'], 'nicht gefundene PLZ') !== false) {
        http_response_code(404);
    } elseif (strpos($result['error'], 'Fehler beim Abrufen') !== false) {
        http_response_code(502); // Bad Gateway (Strapi-Problem)
    } else {
        http_response_code(400);
    }
} else {
    http_response_code(200);
}

// 6. JSON zurückgeben
echo json_encode($response, JSON_UNESCAPED_UNICODE);