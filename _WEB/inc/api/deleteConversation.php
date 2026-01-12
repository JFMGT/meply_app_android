<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt debugLog etc.

// CSRF-Token validieren
requireCsrfToken();
header('Content-Type: application/json');

// --- Nur Fehler loggen ---

// 1. JWT aus Session prüfen
$authToken = $_SESSION['jwt'] ?? null;
$currentUserDocumentId = $_SESSION['profile']['documentId'] ?? null; // Nötig für Auth-Check

if (!$authToken || !$currentUserDocumentId) {
    http_response_code(401);
    // Fehler-Log
    debugLog("delete-conversation.php: FEHLER - Nicht authentifiziert oder Session unvollständig.");
    echo json_encode(['success' => false, 'message' => 'Nicht authentifiziert oder Session unvollständig.']);
    exit;
}

// 2. conversationId aus Request holen und validieren
$input = json_decode(file_get_contents('php://input'), true);
$convoId = $input['convoId'] ?? null;

if (empty($convoId)) {
    http_response_code(400);
    // Fehler-Log
    debugLog("delete-conversation.php: FEHLER - convoId fehlt im Request.");
    echo json_encode(['success' => false, 'message' => 'conversationId fehlt']);
    exit;
}
// SICHERHEIT: ID für URL-Pfad vorbereiten
$safeConvoIdEncoded = urlencode($convoId);

// =======================================================================
// === PLATZHALTER: EXPLIZITER AUTORISIERUNGS-CHECK (UNBEDINGT NÖTIG!) ===
// =======================================================================
// BEVOR du löschst, MUSST du prüfen, ob der $currentUserDocumentId
// Teilnehmer dieser Konversation ($safeConvoIdEncoded) ist!
// Das erfordert wahrscheinlich einen zusätzlichen GET-Request an Strapi.
// Wenn dieser Check fehlschlägt:
/*
    http_response_code(403); // Forbidden
    debugLog("delete-conversation.php: FEHLER - Autorisierung fehlgeschlagen. User ist kein Teilnehmer."); // Fehler-Log
    echo json_encode(['success' => false, 'message' => 'Nicht berechtigt, diese Konversation zu löschen.']);
    exit;
*/
// =======================================================================


// 3. DELETE-Request an Strapi senden (Manuelles cURL beibehalten)
$deleteUrl = STRAPI_API_BASE . '/conversations/' . $safeConvoIdEncoded;

$ch = curl_init($deleteUrl);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $authToken,
    'Content-Type: application/json',
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
// --- Fehlerbehandlung ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$err = curl_error($ch);
curl_close($ch);

// 4. Antwort auswerten
// Prüfe zuerst Netzwerkfehler
if ($err) {
    http_response_code(502);
    // SICHERHEIT & Fehler-Log: Logge detaillierten Fehler, gib generische Meldung
    debugLog("delete-conversation.php: FEHLER - Netzwerkfehler: " . $err); // Fehler-Log
    echo json_encode(['success' => false, 'message' => 'Netzwerkfehler zur API']);
    exit;
}

// Prüfe dann HTTP-Erfolgscode
if ($httpCode >= 200 && $httpCode < 300) {
    // Erfolgreich - Kein Log
    echo json_encode(['success' => true]);
} else {
    // API-Fehler (4xx, 5xx)
    $errorCode = $httpCode >= 400 ? $httpCode : 500;
    http_response_code($errorCode);
    // SICHERHEIT & Fehler-Log: Logge detaillierten Fehler, gib generische Meldung
    debugLog("delete-conversation.php: FEHLER - API-Fehler (HTTP: " . $httpCode . "): " . $response); // Fehler-Log
    echo json_encode([
        'success' => false,
        'message' => 'Löschen fehlgeschlagen'
        // 'details' entfernt
    ]);
}
?>