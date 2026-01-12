<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode([]); // Leeres Array bei Auth-Fehler (wie im Original)
    exit;
}
$jwt = $_SESSION['jwt']; // JWT ist gültig

// --- 2. Eingabe holen und validieren (war gut) ---
$query = isset($_GET['q']) ? trim($_GET['q']) : '';
if (strlen($query) < 2) {
    echo json_encode([]); // Leeres Array, wenn Suchbegriff zu kurz
    exit;
}

// --- 3. API-Abfrage (Manuelles cURL beibehalten) ---

// KORREKTUR (Wartung): Konstante STRAPI_API_BASE verwenden
$url = STRAPI_API_BASE . '/boardgames?filters[title][$containsi]=' . urlencode($query) . '&pagination[limit]=10';
// debugLog("Boardgame Search URL: " . $url); // Optional

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt"
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt
$response = curl_exec($ch);

// --- 4. ROBUSTHEIT: Fehlerbehandlung hinzugefügt ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

// Fall 1: Netzwerkfehler
if ($curlError) {
    http_response_code(502); // Bad Gateway
    debugLog("Boardgame Search cURL Error: " . $curlError);
    echo json_encode(['error' => 'Netzwerkfehler zur API']); // Fehler statt leeres Array
    exit;
}

// Fall 2: API-Fehler (4xx, 5xx)
if ($httpCode !== 200) {
    http_response_code($httpCode);
    debugLog("Boardgame Search API Error: HTTP " . $httpCode);
    echo json_encode(['error' => 'API-Fehler beim Suchen']); // Fehler statt leeres Array
    exit;
}
// --- Ende Fehlerbehandlung ---

// --- 5. Datenverarbeitung (Logik war gut) ---
$data = json_decode($response, true);
$games = [];
if (isset($data['data'])) {
    foreach ($data['data'] as $entry) {
        // HINWEIS: Strapi v4 nutzt oft $entry['attributes']['title']
        // Passe dies ggf. an deine Strapi-Version an.
        $title = $entry['title'] ?? ($entry['attributes']['title'] ?? 'Unbekannt');
        $id = $entry['id'] ?? null;
        
        $games[] = [
            'id' => $id,
            'title' => $title
        ];
    }
}

// --- 6. Erfolg zurückgeben (war gut) ---
// Header wurde bereits oben gesetzt
echo json_encode($games);