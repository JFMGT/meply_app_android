<?php
// Annahme: config.php und der HELFER 'makeAdminStrapiApiCall'
// sind bereits durch die zentrale functions.php geladen.
// require_once __DIR__ . '/../config.php'; // HIER ENTFERNT

/**
 * Markiert einen Registrierungscode als verwendet in Strapi.
 * Verwendet den Admin-API-Token.
 *
 * @param string|int $codeId Die ID des zu aktualisierenden Codes.
 * @return bool True bei Erfolg (HTTP 200), sonst false.
 */
function markCodeAsUsed($codeId): bool {
    
    // 1. SICHERHEIT: URL-Pfad-Parameter encoden
    $url = STRAPI_API_BASE . "/registration-codes/" . urlencode($codeId);

    // 2. Payload (war bereits gut)
    $payload = [
        'data' => [
            'used' => true,
            'usedAt' => date('c') // ISO-8601 format
        ]
    ];

    // 3. Admin-Helfer verwenden (DRY, Robustheit)
    $result = makeAdminStrapiApiCall('PUT', $url, $payload);

    // 4. Erfolg prüfen
    if (!$result['success']) {
        // KONSISTENZ: debugLog verwenden
        debugLog("markCodeAsUsed API Fehler: Code " . $result['code'] . " | Response: " . json_encode($result['response']));
    }

    // Gibt true zurück, wenn der Helfer 'success: true' meldet (HTTP 2xx)
    return $result['success'];
}