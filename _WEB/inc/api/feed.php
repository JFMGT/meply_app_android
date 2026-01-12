<?php
// ARCHITEKTUR: Zentral laden
session_start();
require_once('../config.php');
require_once('../functions.php');

// JSON Header KONSISTENT
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('Expires: 0');

// ===== SICHERHEIT: Authentifizierung =====
$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Nicht authentifiziert']);
    exit;
}

// ===== SICHERHEIT: Input-Validierung =====
$authorId = $_GET['author'] ?? null;
$limit    = max(1, min((int)($_GET['limit'] ?? 10), 50)); // Max 50 zur Last-Kontrolle
$reset    = isset($_GET['reset']) ? (int)$_GET['reset'] : 0;

// SICHERHEIT: authorId validieren (falls gesetzt)
if ($authorId !== null) {
    // Prüfen ob numerisch oder UUID-Format (je nach Strapi-Setup)
    if (!is_numeric($authorId) && !preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $authorId)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'Ungültige Author-ID']);
        exit;
    }
}

// Feed-Key & Reset
$feedKeyParts = ['author' => $authorId ?: 'all'];
$feedKey = 'feed_' . md5(json_encode($feedKeyParts));

if ($reset) {
    unset($_SESSION['feed_cursors'][$feedKey]);
}

// Cursor Logik
$sessionCursor = $_SESSION['feed_cursors'][$feedKey] ?? [];
$before = $_GET['before'] ?? ($sessionCursor['oldest'] ?? null);
$since = $_GET['since'] ?? null;

// SICHERHEIT: Cursor-Timestamp validieren (ISO 8601 Format)
if ($before && !preg_match('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/', $before)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Ungültiges Cursor-Format']);
    exit;
}

// Anfrage-Parameter
$query = ['limit' => $limit];
if ($authorId) $query['author'] = $authorId;
if (!empty($before)) $query['before'] = $before;
if (!empty($since))  $query['since']  = $since;

$strapiUrl = STRAPI_API_BASE . '/feed?' . http_build_query($query);

// Strapi Call
$result = makeStrapiApiCall('GET', $strapiUrl);

// Fehlerbehandlung
if (!$result['success']) {
    $errorCode = $result['code'] >= 400 ? $result['code'] : 500;
    http_response_code($errorCode);
    debugLog("Feed API Error: HTTP " . $result['code'] . " - " . json_encode($result['response']));
    echo json_encode(['success' => false, 'error' => 'Fehler beim Laden des Feeds.']);
    exit;
}

// Daten extrahieren
$feed   = $result['response']['feed'] ?? [];
$cursor = $result['response']['cursor'] ?? null;

// Session-Cursor aktualisieren
if (is_array($cursor)) {
    if (empty($sessionCursor['newest']) && !empty($cursor['newestCreatedAt'])) {
        $sessionCursor['newest'] = $cursor['newestCreatedAt'];
    }
    if (!empty($cursor['oldestCreatedAt'])) {
        $sessionCursor['oldest'] = $cursor['oldestCreatedAt'];
    }
    $sessionCursor['hasMore'] = !empty($cursor['hasMore']);
    $_SESSION['feed_cursors'][$feedKey] = $sessionCursor;
}

// HTML rendern (mit Output Buffering)
ob_start();
if (!empty($feed)) {
    renderPosts($feed);
}
$html = ob_get_clean();

// ===== KONSISTENTE JSON-ANTWORT =====
$response = [
    'success' => true,
    'html' => $html,
    'cursor' => $cursor['oldestCreatedAt'] ?? null,
    'hasMore' => $cursor['hasMore'] ?? false
];

echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
exit;