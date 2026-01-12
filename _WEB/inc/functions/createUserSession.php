<?php
require_once __DIR__ . '/../config.php'; // Wird für STRAPI_API_TOKEN und debugLog benötigt

/**
 * Erstellt eine "user-session" Entität in Strapi (vermutlich für Logging).
 * Verwendet den Admin-API-Token.
 *
 * @param int $userId Die ID des Benutzers, für den die Session geloggt wird.
 * @return bool True bei Erfolg (HTTP 200 oder 201), ansonsten false.
 */
function createUserSession($userId) {
    $url = STRAPI_API_BASE . "/user-sessions";

    $payload = [
        'data' => [
            'user' => $userId
        ]
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10); // Timeout hinzugefügt

    $response = curl_exec($ch);

    // 1. NEU: Robustheit - Zuerst auf Netzwerkfehler prüfen
    if (curl_errno($ch)) {
        debugLog("createUserSession cURL Error: " . curl_error($ch));
        curl_close($ch);
        return false;
    }
    
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    // 2. KORRIGIERT: Konsistentes Logging (debugLog) und korrekter Name
    if ($httpCode !== 200 && $httpCode !== 201) {
        debugLog("createUserSession API Fehler: Code $httpCode | Response: $response");
    }

    // Rückgabe-Logik war bereits korrekt
    return $httpCode === 200 || $httpCode === 201;
}