<?php
declare(strict_types=1);
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

/**
 * Hilfsfunktion für JSON-Antworten (unverändert, aber global laden wäre besser)
 */
function respond(array $data, int $status = 200): void {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    respond(['success' => false, 'error' => 'Nicht autorisiert'], 401);
}
// JWT ist gültig und wird vom Helfer genutzt

// --- 2. Query-Parameter validieren (Logik war gut) ---
$page     = isset($_GET['page'])     ? max(1, (int)$_GET['page'])     : 1;
$pageSize = isset($_GET['pageSize']) ? (int)$_GET['pageSize']       : 24;
if ($pageSize < 1)  $pageSize = 24;
if ($pageSize > 100) $pageSize = 100; // Limit pageSize

// --- 3. Strapi-Request vorbereiten (mit Helfer) ---
$query = http_build_query(['page' => $page, 'pageSize' => $pageSize]);
// Annahme: STRAPI_API_BASE ist korrekt in config.php definiert
$url   = rtrim(STRAPI_API_BASE, '/') . '/user-uploads/me?' . $query;

// API-Aufruf mit Helfer
$result = makeStrapiApiCall('GET', $url);

// --- 4. Ergebnis verarbeiten ---
if (!$result['success']) {
    // Fehlerfall (Netzwerk, 4xx, 5xx)
    $errorCode = $result['code'] ?: 500;
    // SICHERHEIT: Logge detaillierten Fehler, gib generische Meldung
    debugLog("List User Images Error: HTTP {$result['code']} - " . json_encode($result));
    $errorMsg = ($errorCode === 0) ? 'Netzwerkfehler' : 'Fehler beim Laden der Bilder';
    respond(['success' => false, 'error' => $errorMsg], $errorCode);
}

// Erfolg - Daten extrahieren und transformieren
$payload = $result['response']; // Helfer hat bereits json_decode gemacht
$data = $payload['data'] ?? [];
$pag  = $payload['meta']['pagination'] ?? ['page' => $page, 'pageSize' => $pageSize, 'pageCount' => 1, 'total' => 0];

// Transformation (Logik war gut)
$results = array_map(function ($row) {
    $f = $row['file'] ?? null;
    $thumb   = $f['formats']['thumbnail']['url'] ?? ($f['url'] ?? null);
    $preview = $f['formats']['medium']['url']    ?? ($f['formats']['small']['url'] ?? ($f['url'] ?? null));
    // Füge STRAPI_IMAGE_BASE hinzu für vollständige URLs
    $imageBase = defined('STRAPI_IMAGE_BASE') ? rtrim(STRAPI_IMAGE_BASE, '/') : '';
    return [
        'id'        => $row['id']        ?? null,
        'reason'    => $row['reason']    ?? null,
        'createdAt' => $row['createdAt'] ?? null,
        'file' => $f ? [
            'id'           => $f['id']   ?? null,
            'url'          => $f['url']  ? $imageBase . $f['url'] : null,
            'thumbnailUrl' => $thumb    ? $imageBase . $thumb    : null,
            'previewUrl'   => $preview  ? $imageBase . $preview  : null,
            'name'         => $f['name'] ?? null,
            'ext'          => $f['ext']  ?? null,
            'mime'         => $f['mime'] ?? null,
            'size'         => $f['size'] ?? null,
            'width'        => $f['width']?? null,
            'height'       => $f['height']?? null,
        ] : null,
    ];
}, is_array($data) ? $data : []);

// --- 5. Erfolgsantwort senden ---
respond([
    'success'    => true,
    'results'    => $results,
    'pagination' => [
        'page'      => (int)($pag['page']      ?? $page),
        'pageSize'  => (int)($pag['pageSize']  ?? $pageSize),
        'pageCount' => (int)($pag['pageCount'] ?? 1),
        'total'     => (int)($pag['total']     ?? 0),
    ],
], 200); // Immer 200 OK bei Erfolg
?>