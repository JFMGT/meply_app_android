<?php
require_once __DIR__ . '/../config.php'; // Nötig für Konstanten und debugLog

/**
 * Holt die aktuell gültige Versionsnummer der Datenschutzrichtlinie aus Strapi.
 * Verwendet den Admin-API-Token.
 *
 * @return string|null Die Versionsnummer (z.B. "1.2") bei Erfolg,
 * oder null bei jeglicher Art von Fehler.
 */
function getCurrentPrivacyVersion() {
    $url = STRAPI_API_BASE . "/legal-setting";

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt

    $response = curl_exec($ch);

    // 1. NEU: Robustheit - Auf Netzwerkfehler prüfen
    if (curl_errno($ch)) {
        debugLog("getCurrentPrivacyVersion cURL Error: " . curl_error($ch));
        curl_close($ch);
        return null;
    }

    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode === 200 || $httpCode === 201) {
        $data = json_decode($response, true);

        // HINWEIS: Strapi v4 Single Types nutzen oft $data['data']['attributes']['...']
        // Dein Code nutzt $data['data']['...'], was okay ist, wenn es v3 oder ein Custom Endpoint ist.
        if (isset($data['data']['privacy_policy_version'])) {
            return $data['data']['privacy_policy_version'];
        }
        
        // Loggen, wenn 200 OK, aber die Struktur unerwartet ist
        debugLog("getCurrentPrivacyVersion API Error: HTTP 200, aber 'privacy_policy_version' fehlt.");
        return null;
    }

    // 2. KORRIGIERT: Konsistentes Logging
    debugLog("getCurrentPrivacyVersion API Fehler: Code $httpCode | Response: $response");

    return null;
}