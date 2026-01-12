<?php
// Benötigt: config.php, makeStrapiApiCall, makeAdminStrapiApiCall (oder public), debugLog
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Holt Event-Daten über den /events/nearby Endpunkt und filtert nach Datum im PHP.
 * HOLT AUCH Like-Status und Like-Counts.
 *
 * @param string $zip Postleitzahl.
 * @param float $radius Suchradius in km.
 * @param int $limit Maximale Anzahl der Events.
 * @param string|null $jwt JWT des Benutzers (für Like-Status).
 * @param string|null $profileDocumentId DocumentId des Benutzers.
 * @return array Ein Array mit ['events' => array|false, 'likedIds' => array, 'likeCounts' => array]
 * oder ['error' => string] bei Fehlern.
 */
function get_nearby_events_data(string $zip, float $radius, int $limit, ?string $jwt, ?string $profileDocumentId): array {
debugLog($zip . " " . $radius);
    
    $targetZip = trim($zip);
    $targetRadius = $radius;


    // --- 1. Events über /nearby abrufen ---
    // API-Query OHNE Datumsfilter und Limit (API soll Location filtern)
    $eventQuery = http_build_query([
        'zip' => $zip,
        'radius' => $radius,
        'sort' => 'start_date:asc',
        'populate' => '*', // Hole alles, da wir Likes etc. brauchen
        'pagination[pageSize]' => 250 // Hole vorsichtshalber mehr, falls API unzuverlässig limitiert
    ]);

    $eventApiUrl = STRAPI_API_BASE . '/events/nearby?' . $eventQuery;
    debugLog("get_nearby_events_data: API URL: " . $eventApiUrl);

    // ANNAHME: /events/nearby ist öffentlich oder braucht Admin-Token?
    // Hier wird makeAdminStrapiApiCall genutzt, anpassen falls nötig!
    $eventResult = makeAdminStrapiApiCall('GET', $eventApiUrl); // ODER makePublicApiCall

    if (!$eventResult['success']) {
    $errorMsg = 'Fehler beim Abrufen der Events.';
    
    // Strapi wirft bei badRequest: { error: { message: "..." } }
    if ($eventResult['code'] === 400) {
        // Prüfe verschiedene mögliche Error-Strukturen
        if (isset($eventResult['response']['error']['message'])) {
            $errorMsg = $eventResult['response']['error']['message'];
        } elseif (isset($eventResult['response']['message'])) {
            $errorMsg = $eventResult['response']['message'];
        } elseif (is_string($eventResult['response'])) {
            $errorMsg = $eventResult['response'];
        }
    }
    
    debugLog("get_nearby_events_data (Events) API Fehler: HTTP " . $eventResult['code'] . " - " . json_encode($eventResult['response']));
    return ['error' => $errorMsg];
}

    $eventsFromApi = $eventResult['response']['data'] ?? [];
    debugLog("get_nearby_events_data: Anzahl Events von API (/nearby): " . count($eventsFromApi));


    // --- 2. Nach Datum im PHP filtern (wie im Original) ---
    // ARCHITEKTUR-HINWEIS: Diese Filterung sollte idealerweise
    // direkt in der API-Query erfolgen ('filters[start_date][$gte]=...'),
    // um das Übertragen unnötiger Daten zu vermeiden.
    $today = strtotime(date('Y-m-d'));
    $filteredEvents = array_filter($eventsFromApi, function ($event) use ($today) {
         // Annahme Strapi v4 Struktur
         $attr = $event['attributes'] ?? $event; // Fallback
         $start = isset($attr['start_date']) ? strtotime($attr['start_date']) : null;
         $end   = isset($attr['end_date']) ? strtotime($attr['end_date']) : null;
         // Dein Original hatte hier noch 'placeholder', falls das relevant ist:
         // $placeholder = trim($attr['date_placeholder'] ?? '');
         // return ($start && $start >= $today) || ($end && $end >= $today) || !empty($placeholder);
         return ($start && $start >= $today) || ($end && $end >= $today); // Nur Datum prüfen
    });
    debugLog("get_nearby_events_data: Anzahl Events nach Datumsfilter: " . count($filteredEvents));


    // --- 3. Im PHP limitieren (wie im Original) ---
    // ARCHITEKTUR-HINWEIS: Das Limit sollte idealerweise direkt in der
    // API-Query ('pagination[pageSize]') berücksichtigt werden.
    $limitedEvents = array_slice(array_values($filteredEvents), 0, $limit); // array_values() für korrekte Indizes
    debugLog("get_nearby_events_data: Anzahl Events nach Limitierung: " . count($limitedEvents));


    if (empty($limitedEvents)) {
        return ['events' => [], 'likedIds' => [], 'likeCounts' => []];
    }

    $documentIds = array_filter(array_map(fn($e) => $e['documentId'] ?? ($e['attributes']['documentId'] ?? null), $limitedEvents)); // IDs sammeln

    // --- 4. Like-Status UND Counts holen (wie in der vorherigen Version) ---
    // ARCHITEKTUR-HINWEIS: Diese zwei zusätzlichen API-Aufrufe sind ineffizient (N+2).
    // Sie sollten idealerweise durch EINEN Call ersetzt oder die Daten direkt
    // mit dem Event-Aufruf über 'populate' geholt werden.
    $likedIds = [];
    $likeCounts = [];
    if ($jwt && !empty($documentIds)) {
        // a) Like-Status holen (User-JWT)
        $likeStatusQuery = http_build_query(['documentIds' => $documentIds]);
        $likeCheckUrl = STRAPI_API_BASE . "/likes/has-liked?" . $likeStatusQuery;
        $likeStatusResult = makeStrapiApiCall('GET', $likeCheckUrl); // User-JWT Helfer!
        if ($likeStatusResult['success'] && !empty($likeStatusResult['response']['liked'])) {
            $likedIds = $likeStatusResult['response']['liked'];
        } else if (!$likeStatusResult['success']) {
             debugLog("get_nearby_events_data (Like Status) API Fehler: HTTP " . $likeStatusResult['code']);
        }

        // b) Like-Counts holen (Optimierte Abfrage)
        $countQuery = http_build_query([
            'filters[targetContentType][$eq]' => 'event', // Muss zum Modelnamen passen
            'filters[targetDocumentId][$in]' => $documentIds,
            'pagination[pageSize]' => count($documentIds)
        ]);
        $countUrl = STRAPI_API_BASE . '/like-counts?' . $countQuery;
        // Welchen Token braucht /like-counts? Annahme: Admin
        $countResult = makeAdminStrapiApiCall('GET', $countUrl);
        if ($countResult['success'] && !empty($countResult['response']['data'])) {
            foreach ($countResult['response']['data'] as $likeEntry) {
                 if (isset($likeEntry['targetDocumentId']) && isset($likeEntry['count'])) {
                     $likeCounts[$likeEntry['targetDocumentId']] = $likeEntry['count'];
                 }
            }
        } else if (!$countResult['success']) {
            debugLog("get_nearby_events_data (Like Counts) API Fehler: HTTP " . $countResult['code']);
        }
    }

    return [
        'events' => $limitedEvents, // Gib die gefilterte und limitierte Liste zurück
        'likedIds' => $likedIds,
        'likeCounts' => $likeCounts
    ];
}