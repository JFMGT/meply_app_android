<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, esc etc.

// ARCHITEKTUR: Header hier setzen, da HTML ausgegeben wird
header('Content-Type: text/html; charset=utf-8');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    http_response_code(401); // Unauthorized
    exit('<div class="message error-message">' . esc('Nicht autorisiert. Bitte einloggen.') . '</div>');
}

// Session-Daten holen (nur noch für myProfileId benötigt)
$jwt = $_SESSION['jwt']; // Gültig nach Prüfung
$myProfileId = $_SESSION['profile']['documentId'] ?? null; // Benötigt für 'isMine'-Vergleich

// --- 2. Eingabe holen und validieren ---
$convoIdRaw = $_GET['id'] ?? null;
if (empty($convoIdRaw)) {
    http_response_code(400); // Bad Request
    exit('<div class="message error-message">' . esc('Fehler: Konversations-ID fehlt.') . '</div>');
}
// SICHERHEIT: ID für URL-Pfad encoden
$convoIdEncoded = urlencode($convoIdRaw);

// --- 3. API-Abfrage an Strapi (Manuelles cURL beibehalten) ---
$apiUrl = STRAPI_API_BASE . "/conversations/{$convoIdEncoded}/messages";
// debugLog("Load Messages API URL: " . $apiUrl);

$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [ "Authorization: Bearer $jwt", "Accept: application/json" ]);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

// --- 4. ROBUSTHEIT: Fehlerbehandlung für API-Call ---
if ($curlError || $httpCode !== 200 || !$response) {
    // Fehler loggen
    debugLog("Load Messages Error: HTTP {$httpCode}, cURL: {$curlError}");
    // HTTP-Code setzen (wichtig für res.ok im JS)
    http_response_code($httpCode ?: 500); // Setze API-Code oder 500
    // HTML-Fehlermeldung ausgeben
    exit('<div class="message error-message">' . esc('Fehler beim Laden der Nachrichten (Code: ' . ($httpCode ?: 'Netzwerk') . ').') . '</div>');
}

// --- 5. JSON-Daten auswerten ---
$data = json_decode($response, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    http_response_code(500);
    debugLog("Load Messages JSON Decode Error: " . json_last_error_msg());
    exit('<div class="message error-message">' . esc('Fehler beim Verarbeiten der API-Antwort.') . '</div>');
}

// --- 6. SICHERHEIT: Redundanter Autorisierungs-Check ENTFERNT ---
// $participants = $data['participants'] ?? []; // Wird nicht mehr benötigt
// if (!$isParticipant) { ... } // ENTFERNT

$messages = $data['messages'] ?? [];
$conversationClosed = $data['conversationClosed'] ?? false;

// --- 7. Nachrichten ausgeben (HTML-Rendering, mit XSS-Fixes) ---
if (empty($messages) && !$conversationClosed) {
     echo "<div class='message info-message'>Noch keine Nachrichten in dieser Unterhaltung.</div>";
}

foreach ($messages as $msg) {
    // Prüfen, ob $myProfileId gesetzt ist (sollte nach Login der Fall sein)
    $isMine = $myProfileId && ($msg['author']['documentId'] ?? '') === $myProfileId;
    $cssClass = $isMine ? 'sent' : 'received';
    $text = nl2br(esc($msg['content'] ?? ''));
    $timestamp = 'unbekannt';
    if (!empty($msg['createdAt'])) { try { $timestamp = date('d.m.Y H:i', strtotime($msg['createdAt'])); } catch (Exception $e) {} }
    $msgDocumentId = esc($msg['documentId'] ?? ''); // XSS-Fix

    echo "<div class='message {$cssClass}'>";
    echo "<div class='msg-text'>{$text}</div>";
    echo "<div class='msg-time'>{$timestamp}";
    if ($isMine && empty($msg['deletedByUser'])) {
        echo "<span class='delete-message' data-id='{$msgDocumentId}'><i class='fa-solid fa-trash'></i></span>"; // XSS-Fix
    }
    echo "</div>"; // msg-time
    echo "</div>"; // message
}

if ($conversationClosed) {
    echo "<div class='message received no-reply-warning'>" . esc('Dein Gegenüber hat die Unterhaltung verlassen.') . "</div>";
    echo "<style>.pm-actions { display: none !important; }</style>";
}