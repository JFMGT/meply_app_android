<?php
/**
 * Ruft den Match-Score für zwei Profile von einem Custom-API-Endpunkt ab.
 *
 * @param string $profileA Die documentId von Profil A.
 * @param string $profileB Die documentId von Profil B.
 * @return array Das standardisierte Antwort-Array vom Helfer.
 */
function getMatchScore(string $profileA, string $profileB): array {
    
    // Validierung (unverändert)
    if (!preg_match('/^[a-zA-Z0-9\-]+$/', $profileA) || !preg_match('/^[a-zA-Z0-9\-]+$/', $profileB)) {
        return [
            'success' => false,
            'code' => 400,
            'response' => 'Ungültige Profil-IDs übergeben.'
        ];
    }

    // -----------------------------------------------------------------
    // KORREKTUR HIER:
    // Das "/api" wurde entfernt, da es bereits in STRAPI_API_BASE enthalten ist.
    // -----------------------------------------------------------------
    $endpoint = "/match/" . urlencode($profileA) . "/" . urlencode($profileB);
    
    // Baut die URL jetzt korrekt zusammen:
    // .../api (aus Konstante) + /match/A/B (aus $endpoint)
    $url = STRAPI_API_BASE . $endpoint;

    // Aufruf mit dem Helfer (unverändert)
    return makeStrapiApiCall('GET', $url);
}