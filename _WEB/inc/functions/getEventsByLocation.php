<?php
// Benötigte Abhängigkeiten (sollten idealerweise schon in functions.php geladen werden)
// 'makeStrapiApiCall', 'makeAdminStrapiApiCall' (falls vorhanden)
// 'renderLikeLink' (wird als existent angenommen)
require_once __DIR__ . '/../config.php'; // Nötig für Konstanten und debugLog
require_once __DIR__ . '/../functions.php'; // Nötig für Konstanten und debugLog
/**
 * ===================================================================
 * FUNKTION 1 (NEU): Die "DATEN"-Schicht
 * Holt alle Event-Daten, Like-Status und Like-Counts.
 * Behebt Performance- und Token-Fehler.
 * ===================================================================
 */
function get_events_by_location_data(string $documentId, int $limit = 25) {
    
    // ----------------------------------------
    // 1. Events abrufen (PERFORMANCE-FIX)
    // ----------------------------------------
    
    // PERFORMANCE-FIX: Datumsfilter und Limit direkt in die API-Query
    $today = date('Y-m-d');
    $query = http_build_query([
        'filters[location][documentId][$eq]' => $documentId,
        'filters[start_date][$gte]' => $today, // Nur zukünftige Events
        'sort' => 'start_date:asc',
        'pagination[pageSize]' => $limit // Nur so viele holen, wie gebraucht
    ]);
    $apiUrl = STRAPI_API_BASE . '/events?' . $query;

    // HINWEIS: Besser wäre makeAdminStrapiApiCall()
    $ch_events = curl_init($apiUrl);
    curl_setopt($ch_events, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch_events, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        "Accept: application/json"
    ]);
    curl_setopt($ch_events, CURLOPT_TIMEOUT, 5);

    $response_events = curl_exec($ch_events);
    $httpCode_events = curl_getinfo($ch_events, CURLINFO_HTTP_CODE);
    $err_events = curl_error($ch_events);
    curl_close($ch_events);

    if ($err_events || $httpCode_events !== 200) {
        debugLog("get_events_by_location_data (Events) Fehler: " . $err_events . " | HTTP: " . $httpCode_events);
        return ['events' => false]; // Fehler-Indikator
    }

    $eventsData = json_decode($response_events, true);
    $events = $eventsData['data'] ?? [];
    
    // PERFORMANCE-FIX: PHP-Filterung entfernt, da die API dies nun tut.
    
    if (empty($events)) {
        return ['events' => [], 'likedIds' => [], 'likeCounts' => []];
    }
    
    $documentIds = array_map(fn($e) => $e['documentId'] ?? '', $events);

    // ----------------------------------------
    // 2. Like-Status abrufen (TOKEN-FIX)
    // ----------------------------------------
    
    // TOKEN-FIX: 'has-liked' MUSS den User-JWT verwenden, nicht den Admin-Token.
    $user_jwt = $_SESSION['jwt'] ?? null;
    $likedIds = [];

    if ($user_jwt && count($documentIds) > 0) {
        $query = implode('&', array_map(fn($id) => 'documentIds[]=' . urlencode($id), $documentIds));
        $likeCheckUrl = STRAPI_API_BASE . "/likes/has-liked?$query";

        // HINWEIS: Besser wäre makeStrapiApiCall() (der User-Helfer)
        $ch_likes = curl_init($likeCheckUrl);
        curl_setopt($ch_likes, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch_likes, CURLOPT_HTTPHEADER, [
            "Authorization: Bearer $user_jwt", // <-- KORREKTER TOKEN
            "Accept: application/json"
        ]);
        curl_setopt($ch_likes, CURLOPT_TIMEOUT, 5);
        $likeResponse = curl_exec($ch_likes);
        curl_close($ch_likes);
        
        $likeData = json_decode($likeResponse, true);
        if (!empty($likeData['liked']) && is_array($likeData['liked'])) {
            $likedIds = $likeData['liked'];
        }
    }

    // ----------------------------------------
    // 3. Like-Counts abrufen
    // ----------------------------------------
    
    // HINWEIS: Besser wäre eine API-Route, die nur die Counts für $documentIds holt
    $likeCounts = [];
    $ch_counts = curl_init(STRAPI_API_BASE . '/like-counts?pagination[pageSize]=1000');
    curl_setopt($ch_counts, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch_counts, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . STRAPI_API_TOKEN, // Admin-Token ist hier OK
        'Content-Type: application/json',
    ]);
    curl_setopt($ch_counts, CURLOPT_TIMEOUT, 5);
    $response_counts = curl_exec($ch_counts);
    curl_close($ch_counts);

    $result_counts = json_decode($response_counts, true);
    if (!empty($result_counts['data'])) {
        foreach ($result_counts['data'] as $likeEntry) {
            if (isset($likeEntry['targetDocumentId']) && isset($likeEntry['count'])) {
                $likeCounts[$likeEntry['targetDocumentId']] = $likeEntry['count'];
            }
        }
    }

    // 4. Alle Daten gebündelt zurückgeben
    return [
        'events' => $events,
        'likedIds' => $likedIds,
        'likeCounts' => $likeCounts
    ];
}


/**
 * ===================================================================
 * FUNKTION 2 (NEU): Die "PRÄSENTATIONS"-Schicht
 * Baut das HTML auf. Enthält alle Sicherheits-Fixes.
 * ===================================================================
 */
function render_events_html($data): string {
    
    // Fall 1: API-Fehler
    if (!is_array($data) || $data['events'] === false) {
        return '<div class="card">Fehler beim Abrufen der Events.</div>';
    }

    $events = $data['events'];
    $likedIds = $data['likedIds'];
    $likeCounts = $data['likeCounts'];

    // Fall 2: Keine Events gefunden
    if (empty($events)) {
        return '<div class="card">Keine Events gefunden.</div>';
    }

    $html = '';
    foreach ($events as $event) {
        $e = $event;
        
        // ----------------------------------------
        // SICHERHEITS-FIX (XSS): Alle Ausgaben escapen
        // ----------------------------------------
        
        $title = htmlspecialchars($e['Title'] ?? 'Ohne Titel');
        $slug_raw = $e['slug'] ?? $e['documentId']; // Rohdaten
        $city = htmlspecialchars(trim($e['city'] ?? 'Unbekannter Ort'));
        $description = htmlspecialchars($e['description'] ?? 'Keine Beschreibung verfügbar.');
        $url_raw = $e['url'] ?? '#'; // Rohdaten
        $docId = htmlspecialchars($e['documentId'] ?? '');
        $likes = (int)($likeCounts[$e['documentId'] ?? ''] ?? 0); // (int) zur Sicherheit
        $liked = in_array($e['documentId'] ?? '', $likedIds);
        $meetings_raw = $e['meetingCount'] ?? 0; // Rohdaten

        // Bild-URL (Annahme: $image ist eine statische, sichere URL)
        $image = WEBSITE_BASE . "etc/images/pattern-7451714_1280-768x768.jpg";

        // Datumsformatierung (war in Ordnung)
        $start_date = $e['start_date'] ?? null;
        $end_date = $e['end_date'] ?? null;
        $formatted_date = 'Datum folgt';
        if ($start_date && $end_date) {
            $formatted_date = date("d.m.Y", strtotime($start_date)) . " bis " . date("d.m.Y", strtotime($end_date));
        } elseif ($start_date) {
            $formatted_date = date("d.m.Y", strtotime($start_date));
        }

        // SICHERHEITS-FIX (XSS): $slug und $meetings (count) escapen
        $safe_slug = htmlspecialchars($slug_raw, ENT_QUOTES, 'UTF-8');
        $safe_meetings_count = htmlspecialchars($meetings_raw);
        $safe_url = htmlspecialchars($url_raw, ENT_QUOTES, 'UTF-8');

        $html .= "<div class='event'>";
        $html .= "<h2>" . $title . "</h2>";
        
        // Annahme: renderLikeLink() ist sicher und escaped seine eigenen Daten
        $html .= renderLikeLink($docId, 'event', $likes, $liked); 
        
        $html .= "<p><strong><i class='fa-solid fa-location-dot'></i> " . $city . "</strong> ";
        $html .= "<strong><i class='fa-solid fa-calendar-days'></i> " . $formatted_date . "</strong></p>";
        $html .= "<p>" . $description . "</p>";

        if ($url_raw && $url_raw !== '#') {
            $html .= "<p><i class='fa-solid fa-up-right-from-square'></i> <a target='_blank' href='" . $safe_url . "'>zur Website</a></p>";
        }

        $html .= "<img src='{$image}' alt='Event Bild'>";
        $html .= "<p class='cta'><a class='compact' href='/events/{$safe_slug}'>"; // <-- FIX
        $html .= "<i class='fa-solid fa-users'></i><span> Info & Mitspieler <span class='count'>".$safe_meetings_count."</span></span></a></p>"; // <-- FIX
        $html .= "</div>";
    }

    return $html;
}


/**
 * ===================================================================
 * FUNKTION 3 (ANGEPASST): Die "CONTROLLER"-Schicht
 * Behält den alten Namen, sodass bestehende Aufrufe funktionieren.
 * ===================================================================
 */
function getEventsByLocation(string $documentId, int $limit = 25, ?string $jwt = null): string {
    if (!$documentId) {
        return '<div class="card">Keine Location-ID übergeben.</div>';
    }

    // 1. Daten holen
    $data = get_events_by_location_data($documentId, $limit);

    // 2. HTML rendern
    $html = render_events_html($data);

    // 3. HTML zurückgeben
    return $html;
}