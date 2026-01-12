<?php
// Benötigte Abhängigkeiten (sollten idealerweise schon in functions.php geladen sein)
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

/**
 * ===================================================================
 * FUNKTION 1 (Refactored): getZipcodeData
 * Nutzt den 'makeStrapiApiCall' Helfer für Robustheit.
 * ===================================================================
 */
function getZipcodeData(string $zip): ?array {
    // Whitelist (war bereits gut)
    $zip = preg_replace('/\D/', '', $zip);
    if (empty($zip)) return null;
    
    $url = STRAPI_API_BASE . '/zipcodes?filters[zip][$eq]=' . urlencode($zip) . '&populate=*';
    
    // Nutzt den robusten Helfer
    $result = makeStrapiApiCall('GET', $url);

    if (!$result['success']) {
        debugLog("getZipcodeData API Fehler: " . ($result['response'] ?? 'Fehler'));
        return null;
    }
    
    return $result['response']['data'][0] ?? null;
}

/**
 * ===================================================================
 * FUNKTION 2 (Unverändert): haversineDistance
 * Diese Funktion ist sicher und performant.
 * ===================================================================
 */
function haversineDistance($lat1, $lon1, $lat2, $lon2): float {
    $earthRadius = 6371; // km
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat / 2) * sin($dLat / 2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon / 2) * sin($dLon / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    return $earthRadius * $c;
}

/**
 * ===================================================================
 * FUNKTION 3 (NEU): Die "DATEN"-Schicht
 * Holt die Meeting-Rohdaten basierend auf dem PLZ-Präfilter.
 * ===================================================================
 */
function get_meetings_by_zip_data(string $zip, float $distance, array $zipData) {
    $targetLat = $zipData['coordinates']['lat'];
    $targetLng = $zipData['coordinates']['lng'];
    
    // PLZ-Mapping Logik (unverändert)
    $mappingFile = null;
    if ($distance <= 15) $mappingFile = __DIR__ . '/plz_json/plz_mapping_15.json';
    elseif ($distance <= 30) $mappingFile = __DIR__ . '/plz_json/plz_mapping_30.json';
    elseif ($distance <= 60) $mappingFile = __DIR__ . '/plz_json/plz_mapping_60.json';
    elseif ($distance <= 100) $mappingFile = __DIR__ . '/plz_json/plz_mapping_100.json';

    $filterZipPrefixes = [];
    if ($mappingFile && file_exists($mappingFile)) {
        $mapping = json_decode(file_get_contents($mappingFile), true);
        $zipPrefix = substr($zip, 0, 2);
        $filterZipPrefixes = $mapping[$zipPrefix] ?? [];
    }

    if (empty($filterZipPrefixes)) {
        return ['error' => 'Keine Nachbarregionen für diese PLZ im gewählten Umkreis gefunden.'];
    }

    $zipFilters = [];
    foreach ($filterZipPrefixes as $prefix) {
        $zipFilters[] = ['zip' => ['$startsWith' => $prefix]];
    }

    // API-Abfrage mit Filter
    $params = [
        'populate' => ['zipcode', 'author', 'location', 'event'],
        'sort' => 'date:asc',
        'filters' => ['zipcode' => ['$or' => $zipFilters]],
        'pagination[pageSize]' => 250 // Hohes Limit für lokales Filtern
    ];

    $query = http_build_query($params);
    $baseUrl = STRAPI_API_BASE . '/meetings?' . $query;
    
    $result = makeStrapiApiCall('GET', $baseUrl);

    if (!$result['success']) {
        return ['error' => 'Fehler beim Abrufen der Spieltreffen.', 'httpCode' => $result['code']];
    }

    $meetings = $result['response']['data'] ?? [];

    // Weitere Filterung per Distanz (unverändert)
    $filteredMeetings = [];
    foreach ($meetings as $meeting) {
        $coords = $meeting['zipcode']['coordinates'] ?? null;
        if (!$coords || !isset($coords['lat'], $coords['lng'])) {
            continue;
        }

        $distanceToTarget = haversineDistance($targetLat, $targetLng, $coords['lat'], $coords['lng']);
        if ($distanceToTarget <= $distance) {
            $filteredMeetings[] = $meeting;
        }
    }
    
    return $filteredMeetings;
}


/**
 * ===================================================================
 * FUNKTION 4 (KOMPLETT REFACTORED): Strukturierte Response
 * Gibt Array zurück: ['success' => bool, 'html' => string, 'error' => string, 'count' => int]
 * ===================================================================
 */
function getMeetingsByZip(string $zip, float $distance): array {
    
    // 1. Session-Prüfung
    if (empty($_SESSION['jwt'])) {
        return [
            'success' => false,
            'error' => 'Du musst eingeloggt sein, um Spieltreffen zu sehen.',
            'html' => '<div class="card"><p>Du musst eingeloggt sein, um Spieltreffen zu sehen.</p></div>',
            'count' => 0
        ];
    }

    // 2. PLZ-Daten holen
    $zipData = getZipcodeData($zip);

    if (!$zipData || empty($zipData['coordinates']) || empty($zipData['zip'])) {
        return [
            'success' => false,
            'error' => 'Ungültige oder nicht gefundene PLZ.',
            'html' => '<div class="card"><p>Ungültige oder nicht gefundene PLZ.</p></div>',
            'count' => 0
        ];
    }

    // 3. Distanz-Limit
    $overHundred = false;
    if ($distance > 100) {
        $distance = 100;
        $overHundred = true;
    }
    
    // 4. Meeting-Daten holen
    $meetingsData = get_meetings_by_zip_data($zip, $distance, $zipData);

    // 5. Fehler von der Daten-Funktion behandeln
    if (isset($meetingsData['error'])) {
        $errorMsg = $meetingsData['error'];
        $httpCode = $meetingsData['httpCode'] ?? null;
        
        if ($httpCode) {
            $errorMsg .= ' (Status ' . esc((string)$httpCode) . ')';
        }
        
        return [
            'success' => false,
            'error' => $errorMsg,
            'html' => '<div class="card"><p>' . esc($errorMsg) . '</p></div>',
            'count' => 0
        ];
    }
    
    if (empty($meetingsData)) {
        return [
            'success' => true, // Technisch erfolgreich, nur keine Ergebnisse
            'error' => null,
            'html' => '<div class="card"><p>Keine Spieltreffen im angegebenen Umkreis gefunden.</p></div>',
            'count' => 0
        ];
    }

    // 6. PERFORMANCE-FIX: Benutzer-ID EINMAL vor der Schleife holen
    $currentUserDocumentId = getProfileData(true);

    $html = '';
    if ($overHundred) {
        $html .= "<div class='card info'><p>Der Suchradius ist auf 100KM limitiert und wurde entsprechend angepasst.</p></div>";
    }

    // 7. HTML generieren
    foreach ($meetingsData as $meeting) {
        $html .= renderMeetingCard($meeting, $currentUserDocumentId);
    }

    return [
        'success' => true,
        'error' => null,
        'html' => $html,
        'count' => count($meetingsData)
    ];
}