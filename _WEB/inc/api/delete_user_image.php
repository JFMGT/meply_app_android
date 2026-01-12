<?php
declare(strict_types=1);
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
header('Content-Type: application/json; charset=utf-8');

// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php'; // Lädt esc(), debugLog etc.

// CSRF-Token validieren
requireCsrfToken();

/**
 * Hilfsfunktion für JSON-Antworten (unverändert)
 */
function respond(array $data, int $status = 200): void {
    http_response_code($status);
    // Stelle sicher, dass JSON_UNESCAPED_UNICODE etc. verfügbar sind (PHP >= 5.4)
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

// --- Logging Start ---
debugLog("--- delete_user_image.php (Minimal korrigiert): Skript gestartet ---");

// Login prüfen (ggf. requireLogin() nutzen)
$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
    debugLog("delete_user_image.php: FEHLER - Nicht authentifiziert (kein JWT).");
    respond(['success' => false, 'error' => 'Nicht authentifiziert'], 401);
}
debugLog("delete_user_image.php: JWT gefunden.");

// Eingabe validieren
$idInput = $_GET['id'] ?? null;
$id = 0;
if (is_numeric($idInput) && $idInput > 0) {
    $id = (int)$idInput;
}
debugLog("delete_user_image.php: Roh-Input ID: " . ($idInput ?? 'NICHT GESETZT'));

if ($id <= 0) {
    debugLog("delete_user_image.php: FEHLER - Ungültige oder fehlende ID.");
    respond(['success' => false, 'error' => 'Ungültige oder fehlende ID'], 400);
}
debugLog("delete_user_image.php: Zu löschende ID: " . $id);

// Basis-URL ermitteln
$STRAPI_BASE = defined('STRAPI_BASE') ? STRAPI_BASE : (defined('STRAPI_API_BASE') ? STRAPI_API_BASE : '');
if (!$STRAPI_BASE) {
    debugLog("delete_user_image.php: FEHLER - STRAPI_BASE nicht konfiguriert.");
    respond(['success' => false, 'error' => 'API-Basis-URL ist nicht konfiguriert'], 500);
}
$base = rtrim($STRAPI_BASE, '/');

// --- Strapi DELETE aufrufen (Manuelles cURL beibehalten) ---
// SICHERHEIT: ID für URL-Pfad encoden
$url = "{$base}/user-uploads/own/" . urlencode((string)$id);
debugLog("delete_user_image.php: DELETE-URL: " . $url);

$ch = curl_init($url);
curl_setopt_array($ch, [
    CURLOPT_CUSTOMREQUEST  => 'DELETE',
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT        => 15,
    CURLOPT_HTTPHEADER     => [
        'Authorization: Bearer ' . $jwt,
        'Accept: application/json',
    ],
]);

$res  = curl_exec($ch);
$err  = curl_error($ch); // NEU: Fehler holen
$code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

debugLog("delete_user_image.php: DELETE Response: " . $res);
debugLog("delete_user_image.php: DELETE HTTP Code: " . $code);
debugLog("delete_user_image.php: DELETE cURL Error: " . ($err ?: 'Kein Fehler'));


// --- Ergebnis verarbeiten (mit Fehlerbehandlung) ---

// NEU: Netzwerkfehler prüfen
if ($res === false) {
    // SICHERHEIT: Logge detaillierten Fehler, gib generische Meldung zurück
    debugLog("delete_user_image.php: FEHLER - Netzwerkfehler: " . $err);
    respond(['success' => false, 'error' => 'Netzwerkfehler beim Löschen'], 502); // Bad Gateway
}

// Strapi kann 200/204 senden; 403/401/404 etc. behandeln
if ($code === 204) { // 204 No Content ist auch Erfolg
    debugLog("delete_user_image.php: Löschen erfolgreich (HTTP 204).");
    respond(['success' => true, 'data' => ['id' => $id]], 200);
}

$payload = json_decode($res, true);
if ($code >= 200 && $code < 300) {
    debugLog("delete_user_image.php: Löschen erfolgreich (HTTP {$code}).");
    // Prüfe optional, ob die erwartete Antwortstruktur vom Custom Endpoint kommt
     if (is_array($payload) && ($payload['success'] ?? false)) {
         respond($payload, 200);
     }
    // Fallback: Generischer Erfolg
    respond(['success' => true, 'data' => ['id' => $id]], 200);
}

// Fehlerfall aus Strapi sauber durchreichen
$errorMsg = 'Unbekannter Fehler'; // Standard
if (is_array($payload)) {
      $errorMsg = $payload['error']['message']
               ?? $payload['error']
               ?? $payload['message']
               ?? $errorMsg;
}
debugLog("delete_user_image.php: FEHLER - API-Fehler (HTTP {$code}): " . $errorMsg);
respond(['success' => false, 'error' => $errorMsg], $code ?: 500);

debugLog("--- delete_user_image.php (Minimal korrigiert): Skript beendet ---");