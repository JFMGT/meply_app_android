<?php
// ARCHITEKTUR-HINWEIS: session_start() sollte global erfolgen.
session_start();
// Logging-Zeile beibehalten, wie im Original
// file_put_contents('test-log.txt', json_encode($_POST) . "\n", FILE_APPEND); // Optional
// ARCHITEKTUR-HINWEIS: config.php sollte global geladen werden.
require_once('../config.php');
require_once('../functions.php'); // Für debugLog

// CSRF-Token validieren
requireCsrfToken();
header('Content-Type: application/json');

// --- Logging Start ---
debugLog("--- delete-meeting.php (Minimal korrigiert + Auth): Skript gestartet ---");

// 1. Hole JWT aus der Session
$authToken = $_SESSION['jwt'] ?? null;
if (!$authToken) {
    http_response_code(401);
    debugLog("delete-meeting.php: FEHLER - Nicht authentifiziert (kein JWT).");
    echo json_encode(['success' => false, 'message' => 'Nicht authentifiziert']);
    exit;
}
// NEU: Hole auch die profileDocumentId für den Check
$currentUserDocumentId = $_SESSION['profile']['documentId'] ?? null;
if (!$currentUserDocumentId) {
    http_response_code(401); // Oder 500, da Session unvollständig
    debugLog("delete-meeting.php: FEHLER - profileDocumentId fehlt in Session.");
    echo json_encode(['success' => false, 'message' => 'Sitzungsinformationen unvollständig.']);
    exit;
}
debugLog("delete-meeting.php: JWT gefunden. Aktueller User: " . $currentUserDocumentId);


// 2. Hole documentId aus dem Request-Body
$input = json_decode(file_get_contents('php://input'), true);
$documentId = $input['documentId'] ?? null;
debugLog("delete-meeting.php: Roh-Input documentId: " . ($documentId ?? 'NICHT GESETZT'));
if (!$documentId) {
    http_response_code(400);
    debugLog("delete-meeting.php: FEHLER - documentId fehlt im Request-Body.");
    echo json_encode(['success' => false, 'message' => 'documentId fehlt']);
    exit;
}
debugLog("delete-meeting.php: Zu löschende documentId: " . $documentId);


// 3. Anfrage an Strapi: Suche Meeting INKLUSIVE AUTOR
// ARCHITEKTUR-HINWEIS: Dieser GET-Request ist redundant, wenn Strapi Policies sicher sind.
$strapiBase = STRAPI_API_BASE . '/meetings';
// NEU: populate=author hinzugefügt, um den Besitzer zu prüfen
$queryUrl = $strapiBase . '?filters[documentId][$eq]=' . urlencode($documentId) . '&populate=author';
debugLog("delete-meeting.php: GET-URL (mit Author): " . $queryUrl);

$ch_get = curl_init($queryUrl);
curl_setopt($ch_get, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch_get, CURLOPT_HTTPHEADER, ['Authorization: Bearer ' . $authToken]);
curl_setopt($ch_get, CURLOPT_TIMEOUT, 5);
$response = curl_exec($ch_get);
$httpCode_get = curl_getinfo($ch_get, CURLINFO_HTTP_CODE);
$err_get = curl_error($ch_get);
curl_close($ch_get);
debugLog("delete-meeting.php: GET Response: " . $response);
debugLog("delete-meeting.php: GET HTTP Code: " . $httpCode_get);
debugLog("delete-meeting.php: GET cURL Error: " . ($err_get ?: 'Kein Fehler'));


if ($err_get || $httpCode_get !== 200) {
    $errorCode = $httpCode_get >= 400 ? $httpCode_get : 500;
    http_response_code($errorCode);
    debugLog("delete-meeting.php: FEHLER - Fehler beim Abrufen des Meetings (GET).");
    echo json_encode(['success' => false, 'message' => 'Fehler beim Abrufen des Meetings (API).']);
    exit;
}

$data = json_decode($response, true);
$meeting = $data['data'][0] ?? null;

if (!$meeting || empty($meeting['documentId'])) {
    http_response_code(404);
    debugLog("delete-meeting.php: FEHLER - Meeting nicht gefunden (404).");
    echo json_encode(['success' => false, 'message' => 'Meeting nicht gefunden']);
    exit;
}
debugLog("delete-meeting.php: Meeting gefunden, ID: " . $meeting['documentId']);

// =======================================================================
// === NEU: EXPLIZITER AUTORISIERUNGS-CHECK ===
// =======================================================================
$authorDocumentId = $meeting['author']['documentId'] ?? null;
debugLog("delete-meeting.php: Autor des Meetings laut API: " . ($authorDocumentId ?? 'NICHT GEFUNDEN'));

if ($authorDocumentId !== $currentUserDocumentId) {
    http_response_code(403); // Forbidden
    debugLog("delete-meeting.php: FEHLER - Autorisierung fehlgeschlagen. User (" . $currentUserDocumentId . ") ist nicht Autor (" . ($authorDocumentId ?? 'N/A') . ").");
    echo json_encode(['success' => false, 'message' => 'Nicht berechtigt, dieses Meeting zu löschen.']);
    exit;
}
debugLog("delete-meeting.php: Autorisierung erfolgreich.");
// =======================================================================

// 4. DELETE-Request an Strapi senden
$meetingIdRaw = $meeting['documentId'];
// --- urlencode beibehalten, da es in der Version funktionierte ---
$deleteUrl = $strapiBase . '/' . urlencode($meetingIdRaw);
debugLog("delete-meeting.php: DELETE-URL: " . $deleteUrl);

$ch_delete = curl_init($deleteUrl);
curl_setopt($ch_delete, CURLOPT_CUSTOMREQUEST, 'DELETE');
curl_setopt($ch_delete, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch_delete, CURLOPT_HTTPHEADER, ['Authorization: Bearer ' . $authToken]);
curl_setopt($ch_delete, CURLOPT_TIMEOUT, 5);
$deleteResponse = curl_exec($ch_delete);
$httpCode_delete = curl_getinfo($ch_delete, CURLINFO_HTTP_CODE);
$err_delete = curl_error($ch_delete);
curl_close($ch_delete);
debugLog("delete-meeting.php: DELETE Response: " . $deleteResponse);
debugLog("delete-meeting.php: DELETE HTTP Code: " . $httpCode_delete);
debugLog("delete-meeting.php: DELETE cURL Error: " . ($err_delete ?: 'Kein Fehler'));

// 5. Antwort zurückgeben
if (!$err_delete && $httpCode_delete >= 200 && $httpCode_delete < 300) {
    debugLog("delete-meeting.php: Löschen erfolgreich (HTTP: " . $httpCode_delete . ").");
    echo json_encode(['success' => true, 'message' => 'Meeting erfolgreich gelöscht']); // Korrekte Erfolgsmeldung
} else {
    $errorCode = $httpCode_delete >= 400 ? $httpCode_delete : 500;
    http_response_code($errorCode);
    debugLog("delete-meeting.php: FEHLER - Löschen fehlgeschlagen (DELETE). HTTP: " . $httpCode_delete);
    echo json_encode(['success' => false, 'message' => 'Löschen fehlgeschlagen (API).']);
}
debugLog("--- delete-meeting.php (Minimal korrigiert + Auth): Skript beendet ---");