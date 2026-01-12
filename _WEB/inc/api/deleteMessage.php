<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt debugLog etc.
header('Content-Type: application/json');

// --- CSRF-Token validieren ---
requireCsrfToken();

// --- Nur Fehler loggen ---

// 1. JWT aus Session prüfen
$authToken = $_SESSION['jwt'] ?? null;
if (!$authToken) {
    http_response_code(401);
    debugLog("deleteMessage.php: FEHLER - Nicht authentifiziert (kein JWT)."); // Fehler-Log
    echo json_encode(['success' => false, 'message' => 'Nicht authentifiziert']);
    exit;
}

// 2. documentId aus Request holen und validieren
$input = json_decode(file_get_contents('php://input'), true);
$documentId = $input['documentId'] ?? null;

if (empty($documentId)) {
    http_response_code(400);
    debugLog("deleteMessage.php: FEHLER - documentId fehlt im Request."); // Fehler-Log
    echo json_encode(['success' => false, 'message' => 'documentId fehlt']);
    exit;
}

// SICHERHEIT: ID für URL-Pfad vorbereiten
$safeDocumentIdEncoded = urlencode($documentId);

// 3. POST-Request an die Custom Route (Manuelles cURL beibehalten)
$strapiUrl = STRAPI_API_BASE . '/messages/' . $safeDocumentIdEncoded . '/mark-as-deleted';

$ch = curl_init($strapiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $authToken,
    'Content-Type: application/json'
]);
curl_setopt($ch, CURLOPT_POSTFIELDS, '{}');
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
    // SICHERHEIT: Keine internen Details nach außen
    debugLog("deleteMessage.php: FEHLER - cURL-Fehler: " . $err);
    echo json_encode(['success' => false, 'message' => 'Verbindungsfehler zum Backend']);
    exit;
}

// Prüfe HTTP-Statuscode
if ($httpCode >= 200 && $httpCode < 300) {
    // Erfolg
    echo json_encode(['success' => true]);
} else {
    // Fehlerhafte Antwort vom Backend
    http_response_code($httpCode);
    debugLog("deleteMessage.php: FEHLER - Backend-Fehler (HTTP $httpCode): " . $response);
    echo json_encode([
        'success' => false,
        'message' => 'Fehler beim Aufruf der API-Route',
        'details' => $response
    ]);
}
?>