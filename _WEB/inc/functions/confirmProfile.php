<?php
// 1. NEU: Konfiguration laden
require_once __DIR__ . '/../config.php';

/**
 * Bestätigt ein Profil in Strapi (setzt 'confirmed' auf true).
 * Nutzt den Admin-API-Token.
 *
 * @param string|int $profileId Die ID des zu bestätigenden Profils.
 * @return bool True bei Erfolg (HTTP 200), ansonsten false.
 */
function confirmProfile($profileId) {
    
    // 2. SICHERHEIT: Eingabe für die URL kodieren
    $url = STRAPI_API_BASE . "/profiles/" . urlencode($profileId);

    $payload = [
        'data' => [
            'confirmed' => true,
        ]
    ];

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PUT");
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt

    $response = curl_exec($ch);

    // 3. ROBUSTHEIT: Zuerst auf Netzwerkfehler prüfen
    if (curl_errno($ch)) {
        debugLog("CURL Fehler bei confirmProfile: " . curl_error($ch));
        curl_close($ch);
        return false;
    }

    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode !== 200) {
        debugLog("HTTP Fehler bei confirmProfile: " . $httpCode . " | Response: " . $response);
    }
    
    // Gibt true zurück, wenn HTTP 200, ansonsten false
    return $httpCode === 200;
}