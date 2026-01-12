<?php
require_once __DIR__ . '/../config.php'; // Wird für STRAPI_API_TOKEN und debugLog benötigt

/**
 * Erstellt ein neues Profil in Strapi und verknüpft es mit einem Benutzer.
 * Verwendet den Admin-API-Token.
 *
 * @param int $userId Die ID des Strapi-Benutzers.
 * @param string $userDocumentId Die documentId des Benutzers.
 * @param string $username Der gewählte Benutzername.
 * @param string $confirmationToken Ein Token für die E-Mail-Bestätigung.
 * @return bool True bei Erfolg (HTTP 200 oder 201), ansonsten false.
 */
function createProfile($userId, $userDocumentId, $username, $confirmationToken) {
    $url = STRAPI_API_BASE . "/profiles";

    $currentPrivacyVersion = '1.2';

    // Log-Eintrag erzeugen
    $privacyLog = [
        [
            'version' => $currentPrivacyVersion,
            'accepted_at' => date('c') // ISO-8601 Zeitformat
        ]
    ];

    $payload = [
    'data' => [
        'userDocumentId' => $userDocumentId,
        'username' => $username,
        'confirmationToken' => $confirmationToken,
        'confirmed' => false,
        'privacy_acceptance_log' => $privacyLog,
        'user' => [
            'connect' => [
                'id' => $userId
                ]
            ]
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
        debugLog("createProfile cURL Error: " . curl_error($ch));
        curl_close($ch);
        return false;
    }

    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    // 2. NEU: Konsistenz - debugLog() statt file_put_contents() verwenden
    if ($httpCode !== 200 && $httpCode !== 201) {
        debugLog("createProfile API Fehler: Code $httpCode | Response: $response");
    }

    // Die Rückgabe-Logik war bereits korrekt
    return $httpCode === 200 || $httpCode === 201;
}