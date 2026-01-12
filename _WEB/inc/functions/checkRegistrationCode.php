<?php
require_once __DIR__ . '/../config.php';

/**
 * Prüft, ob ein Registrierungscode gültig und unbenutzt ist.
 *
 * @param string $code Der vom Benutzer eingegebene Code.
 * @return array|false Gibt das Code-Objekt (array) bei Erfolg zurück,
 * ansonsten false (bei Fehler, Ungültigkeit oder "Nicht gefunden").
 */
function checkRegistrationCode($code)
{
    // 1. SICHERHEIT: Query Injection durch http_build_query behoben
    $query = [
        'filters[code][$eq]' => $code,
        'filters[valid][$eq]' => 'true',
        'filters[$or][0][used][$eq]' => 'false',
        'filters[$or][1][used][$null]' => 'true',
        'pagination[limit]' => 1 // Wir brauchen nur einen Treffer
    ];

    $url = STRAPI_API_BASE . "/registration-codes?" . http_build_query($query);

    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Guter Stil: Timeout setzen

    $response = curl_exec($ch);

    // 2. QUALITÄT: Alle 'echo' entfernt, Fehler werden geloggt
    if (curl_errno($ch)) {
        debugLog("CURL Fehler bei checkRegistrationCode: " . curl_error($ch));
        curl_close($ch);
        return false;
    }

    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode !== 200) {
        // 3. SICHERHEIT: Keine Info-Preisgabe. Fehler wird nur geloggt.
        debugLog("HTTP Fehler bei checkRegistrationCode: " . $httpCode . " | Response: " . $response);
        return false;
    }

    $data = json_decode($response, true);

    // 4. QUALITÄT: 'echo' entfernt.
    if (!isset($data['data']) || empty($data['data'])) {
        // Code wurde nicht gefunden oder ist nicht gültig -> kein Fehler, einfach 'false'
        return false;
    }

    // Erfolg!
    return $data['data'][0];
}