<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.
// include('../functions/getProfileData.php'); // Wird bereits von functions.php geladen

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
// Stellt sicher, dass das JWT nicht nur existiert, sondern auch gültig ist.
if (!is_logged_in_explizit()) {
    http_response_code(401); // Unauthorized
    // Gib die ursprüngliche Fallback-Fehlerstruktur zurück
    echo json_encode([
        'data' => [
            [
                'id' => 0,
                'attributes' => [
                    'type' => 'error',
                    'message' => 'Nicht autorisiert (Sitzung ungültig)', // Klarere Meldung
                    'seen' => false,
                    'archived' => false,
                ]
            ]
        ]
    ]);
    exit;
}
// JWT ist gültig

// Hole User-ID direkt aus der Session (effizienter als getProfileData())
$profileId = $_SESSION['profile']['documentId'] ?? null;
// $token wird für manuelles cURL benötigt
$token = $_SESSION['jwt'];

// Zusätzliche Prüfung, falls Session nach Login unvollständig ist
if (!$profileId) {
     http_response_code(401); // Session unvollständig
     // debugLog("Get Notifications Error: profileDocumentId missing in session."); // Optional loggen
      echo json_encode([
          'data' => [ /* ... Fallback-Fehlerstruktur ... */ ]
      ]);
     exit;
}


// --- 2. Strapi-API-Call (Manuelles cURL beibehalten) ---
// SICHERHEIT: urlencode war bereits korrekt
$apiUrl = STRAPI_API_BASE . "/notifications?populate=*&filters[seen][\$eq]=false&filters[receiver][documentId][\$eq]=" . urlencode($profileId);
// debugLog("Get Notifications API URL: " . $apiUrl); // Optional für Debugging

$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $token"
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt
$response = curl_exec($ch);

// --- 3. ROBUSTHEIT: Grundlegende Fehlerbehandlung (wie im Original, aber mit curl_errno) ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch); // NEU: Netzwerkfehler prüfen
curl_close($ch);

// Fehlerhafte oder leere Antwort → Dummy-Nachricht (Logik beibehalten)
// NEU: Auch $curlError prüfen
if ($curlError || !$response || $httpCode >= 400) {
    // Optional: Logge den spezifischen Fehler serverseitig
    // debugLog("Get Notifications Error: HTTP {$httpCode}, cURL: {$curlError}");

    http_response_code(200); // Behalte 200 bei, um JS nicht zu brechen
    echo json_encode([
        'data' => [
            [
                'id' => 0,
                'attributes' => [
                    'type' => 'error',
                    'message' => 'Fehler beim Abrufen der Benachrichtigungen', // Behalte generische Meldung bei
                    'seen' => false,
                    'archived' => false,
                ]
            ]
        ]
    ]);
    exit;
}

// --- 4. Erfolgreich → Originaldaten zurückgeben (wie im Original) ---
// Header wurde bereits oben gesetzt
echo $response;