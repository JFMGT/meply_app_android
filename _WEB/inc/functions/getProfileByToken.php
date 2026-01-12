<?php
// NEU: config.php wird für Konstanten und debugLog benötigt
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

/**
 * Holt ein Profil anhand eines Bestätigungs-Tokens.
 * Verwendet den Admin-API-Token.
 *
 * @param string $token Der zu suchende Bestätigungs-Token.
 * @return array|null Das Profil-Daten-Array bei Erfolg, sonst null.
 */
function getProfileByToken($token) {
    
    // 1. SICHERHEITS-FIX (Query Injection):
    // Die Parameter MÜSSEN mit http_build_query() erstellt werden.
    $query = http_build_query([
        'filters[confirmationToken][$eq]' => $token,
        'pagination[limit]' => 1 // Wir brauchen nur einen Treffer
    ]);
    
    // Alte, unsichere URL:
    // $url = STRAPI_API_BASE . "/profiles?filters[confirmationToken][\$eq]=$token";
    
    // Neue, sichere URL:
    $url = STRAPI_API_BASE . "/profiles?" . $query;

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // NEU: Timeout

    $response = curl_exec($ch);

    // 2. ROBUSTHEIT: Komplette Fehlerbehandlung hinzugefügt
    if (curl_errno($ch)) {
        debugLog("getProfileByToken cURL Error: " . curl_error($ch));
        curl_close($ch);
        return null; // Netzwerkfehler
    }

    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode !== 200) {
        debugLog("getProfileByToken API Error: HTTP $httpCode | Response: $response");
        return null; // API-Fehler (4xx, 5xx)
    }

    $data = json_decode($response, true);
    
    // 3. Antwort-Logik (unverändert, aber jetzt sicher)
    return $data['data'][0] ?? null;
}