<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

/**
 * Holt die zum Verkauf stehenden Spiele eines bestimmten Profils.
 *
 * @param int|string $profileId Die ID (numerisch oder documentId) des Profils
 * @return array Eine Liste von Verkaufs-Arrays oder ein leeres Array bei Fehler/keine Treffer.
 */
function getUserSales($profileId) {
    
    // Validierung
    if (!$profileId) {
        debugLog("getUserSales: Keine profileId übergeben");
        return [];
    }

    // Custom Route mit Profil-ID
    $url = STRAPI_API_BASE . '/user-boardgames/sales/' . urlencode($profileId);

    // WICHTIG: useSystemToken=false, damit auch nicht-eingeloggte User zugreifen können
    // ODER: Prüfe ob JWT vorhanden, wenn nicht, nutze SystemToken
    if (empty($_SESSION['jwt'])) {
        // Nicht eingeloggt → System Token nutzen für öffentliche Daten
        $result = makeStrapiApiCall('GET', $url, null, true);
    } else {
        // Eingeloggt → User JWT nutzen
        $result = makeStrapiApiCall('GET', $url);
    }

    // Fehlerbehandlung
    if (!$result['success']) {
        debugLog("getUserSales API Error: HTTP " . $result['code'] . " - " . json_encode($result['response']));
        return [];
    }

    // Daten zurückgeben
    return $result['response']['sales'] ?? [];
}