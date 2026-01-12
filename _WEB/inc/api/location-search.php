<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// 1. SICHERHEIT: Explizite Login-Prüfung
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode([]); // Leeres Array bei Fehler
    exit;
}
// JWT ist gültig, Helfer nutzen Admin-Token

// Hole User-ID für Filter
$userDocumentId = $_SESSION['profile']['documentId'] ?? null;
if (!$userDocumentId) {
     http_response_code(401); // Session unvollständig nach Login
     debugLog("Location Search Error: profileDocumentId missing in session.");
     echo json_encode([]);
     exit;
}

// 2. Eingabe holen und validieren
$q = $_GET['q'] ?? '';
// Mindestlänge für Suche beibehalten
if (strlen($q) < 2) {
    echo json_encode([]); // Leeres Array, wenn Suchbegriff zu kurz
    exit;
}

// 3. API-Parameter SICHER bauen
$params = [
    // SICHERHEIT: Filter korrekt in Array-Struktur für http_build_query
    'filters[$or][0][author][documentId]' => $userDocumentId, // Filter auf eigenen User
    'filters[$or][1][allow_user_events]' => true,          // Oder öffentliche Locations
    'filters[Titel][$containsi]' => $q,                     // Suchbegriff (containsi = case-insensitive)
    // Optional: Felder explizit auswählen statt '*' für Performance
    // 'fields[0]' => 'Titel',
    // 'fields[1]' => 'Strasse',
    // ...
    'pagination[limit]' => 5
];

$apiUrl = STRAPI_API_BASE . '/locations?' . http_build_query($params);
debugLog("Location Search API URL: " . $apiUrl);

// 4. API-Aufruf mit Admin-Helfer (da Admin-Token verwendet wird)
$result = makeAdminStrapiApiCall('GET', $apiUrl);

// 5. Robuste Fehlerbehandlung
if (!$result['success']) {
    $errorCode = $result['code'] ?: 500;
    http_response_code($errorCode);
    debugLog("Location Search API Error: HTTP {$result['code']} - " . json_encode($result));
    echo json_encode(['error' => 'Fehler bei der Locationsuche.', 'details' => $result['response']]); // Gib Fehlerdetails zurück
    exit;
}

// 6. Datenverarbeitung (Logik war gut, Felder anpassen)
$results = [];
$responseData = $result['response']['data'] ?? [];

foreach ($responseData as $item) {
    // Passe Feldnamen an deine Strapi v4 'attributes' Struktur an
    $attributes = $item; // Annahme: Helfer gibt direkt attributes zurück oder passe an
    $results[] = [
        'id'            => $attributes['documentId'] ?? $item['id'] ?? '', // ID/documentId je nach Bedarf
        'title'         => $attributes['Titel'] ?? '', // Prüfe Groß/Kleinschreibung
        'street'        => $attributes['Strasse'] ?? '',
        'street_number' => $attributes['Hausnummer'] ?? '',
        'zip'           => $attributes['PLZ'] ?? '',
        'city'          => $attributes['Ort'] ?? '',
    ];
}

// 7. Erfolg zurückgeben
echo json_encode($results); // Nur das Array der Ergebnisse
?>