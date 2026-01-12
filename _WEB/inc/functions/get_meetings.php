<?php
// Benötigte Abhängigkeiten (sollten idealerweise schon in functions.php geladen werden)
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
// 'makeStrapiApiCall' wird als bereits geladen angenommen (via functions.php)


/**
 * ===================================================================
 * FUNKTION 1 (NEU): Die "DATEN"-Schicht
 * Holt nur die Daten von der API, enthält kein HTML.
 * ===================================================================
 *
 * @param string|null $type
 * @param string|null $documentId
 * @param bool $onlyUnlinked
 * @return array|false Ein Array mit Meeting-Daten bei Erfolg, sonst false.
 */
function get_meetings_data(?string $type = null, ?string $documentId = null, bool $onlyUnlinked = false) {
    
    // Basisparameter
    $params = [
        'sort' => 'date:asc',
        'populate' => '*', // populate=* ist oft langsam, sei spezifisch, wenn du kannst
    ];

    // Filterlogik je nach Typ
    if ($onlyUnlinked) {
        $params['filters']['$and'][0]['event']['null'] = true;
        $params['filters']['$and'][1]['location']['null'] = true;
    } elseif ($type === 'single' && $documentId) {
        $params['filters']['documentId']['$eq'] = $documentId;
    } elseif ($type && $documentId) {
        $params['filters'][$type]['documentId']['$eq'] = $documentId;
    }

    // Filter: Nur Events mit leerem oder zukünftigem Datum
    $today = (new DateTime())->format('Y-m-d');

    // Dein "Magic Date" Workaround (unverändert)
    if ($type == 'author' && isset($_SESSION['profileDocumentId']) && $documentId == $_SESSION['profileDocumentId']) {
        $today = '1984-05-05';
    }

    $params['filters']['$and'][] = [
        '$or' => [
            ['date' => ['$null' => true]],
            ['date' => ['$gte' => $today]],
        ]
    ];
    
    // URL aufbauen (sicher, da http_build_query)
    $url = STRAPI_API_BASE . '/meetings?' . http_build_query($params);

    // API-Aufruf über den ROBUSTEN HELFER (löst cURL-Probleme)
    $result = makeStrapiApiCall('GET', $url);

    if (!$result['success']) {
        // Logge den Fehler, aber gib kein HTML aus
        debugLog("get_meetings_data API Fehler: " . ($result['response'] ?? 'Unbekannt'));
        return false;
    }

    return $result['response']['data'] ?? [];
}


/**
 * ===================================================================
 * FUNKTION 2 (NEU): Die "PRÄSENTATIONS"-Schicht
 * Baut das HTML auf. Enthält alle Sicherheits- und Performance-Fixes.
 * ===================================================================
 *
 * @param array|false $meetings Die Daten von get_meetings_data()
 * @param string|null $type Kontext für das $dataAttr
 * @param string|null $documentId Kontext für das $dataAttr
 * @return string Das gerenderte HTML.
 */
function render_meetings_html($meetings, ?string $type = null, ?string $documentId = null): string {

    // 1. SICHERHEITS-FIX (XSS): $documentId hier escapen
    $dataAttr = '';
    if ($type == "location" && $documentId) {
        $dataAttr = "data-location='" . htmlspecialchars($documentId, ENT_QUOTES, 'UTF-8') . "'";
    }
    if ($type == "event" && $documentId) {
        $dataAttr = "data-event='" . htmlspecialchars($documentId, ENT_QUOTES, 'UTF-8') . "'";
    }
    
    $html = '<div class="hold-meetings" ' . $dataAttr . '>';

    // Fehler- oder Leer-Fall
    if (empty($meetings)) {
        // $meetings kann 'false' sein (API-Fehler) oder ein leeres Array (keine Treffer)
        $message = ($meetings === false) 
            ? 'Fehler beim Abrufen der Spieltreffen.' 
            : 'Es wurden noch keine Spieltreffen gefunden.';
        return $html . '<div class="card"><p>' . $message . '</p></div></div>';
    }

    // 2. PERFORMANCE-FIX (N+1): Aktuellen Benutzer VOR der Schleife holen
    $currentUserDocumentId = getProfileData(true); // Annahme: getProfileData(true) gibt ID zurück

    foreach ($meetings as $meeting) {
        $m = $meeting;
        
        // 3. SICHERHEITS-FIX (XSS): Alle data-Attribute escapen
        $meetingId = htmlspecialchars($m['id'] ?? '', ENT_QUOTES, 'UTF-8');
        $meetingDocumentId = htmlspecialchars($m['documentId'] ?? '', ENT_QUOTES, 'UTF-8');
        
        $attributes = $m ?? [];
        
        // 4. SICHERHEITS-FIX (XSS): Alle HTML-Ausgaben mit esc() (oder htmlspecialchars)
        $title = htmlspecialchars($attributes['title'] ?? 'Ohne Titel');
        $description = nl2br(htmlspecialchars($attributes['description'] ?? ''));
        
        // ... (Deine Datums-Logik - die ist sicher und in Ordnung) ...
        $date = 'Kein Datum angegeben';
        if (!empty($attributes['dates']) && is_array($attributes['dates']) && !empty($attributes['dates']['value'])) {
     $dateType = $attributes['dates']['type'] ?? '';
     $value = $attributes['dates']['value'] ?? [];
     switch ($dateType) {
         case 'fixed': //... (Rest der Datumslogik)
                    if (!empty($value['date'])) { $date = date('d.m.Y H:i', strtotime($value['date'])); }
         break;
                //... (alle anderen 'case' Blöcke) ...
     }
        }
        if ($date === 'Kein Datum angegeben' && !empty($attributes['date'])) {
     $date = date('d.m.Y H:i', strtotime($attributes['date']));
     }
        // ... (Ende Datums-Logik) ...

        $userName = htmlspecialchars($attributes['author']['username'] ?? 'Unbekannt');
        $userSlug = htmlspecialchars($attributes['author']['userslug'] ?? $attributes['author']['documentId']);
        $userDocumentID = htmlspecialchars($attributes['author']['documentId'] ?? 'Unbekannt');

        // Avatar für Modal vorbereiten
        $userAvatar = '';
        if (!empty($attributes['author']['avatar'][0]['url'])) {
            $userAvatar = htmlspecialchars(STRAPI_IMAGE_BASE . $attributes['author']['avatar'][0]['url']);
        } elseif (!empty($attributes['author']['id'])) {
            $hash = crc32($attributes['author']['id']);
            $index = ($hash % 8) + 1;
            $userAvatar = htmlspecialchars(WEBSITE_BASE . "/etc/images/avatar{$index}.png");
        } else {
            $userAvatar = htmlspecialchars(WEBSITE_BASE . "/etc/images/avatar1.png");
        }

        $city = 'Unbekannt';
        $placePrivacie = "Unbekannt";

        $locationData = $attributes['location']['Ort'] ?? null;
        $locationTitle = $attributes['location']['Titel'] ?? null;
        $locationSlug = $attributes['location']['slug'] ?? null;
        $locationType = $attributes['location']['Typ'] ?? null;
        $eventCity    = $attributes['event']['city'] ?? null;
        $eventSlug = $attributes['event']['slug'] ?? null;
        $userCity     = $attributes['author']['city'] ?? null;
        $eventTitle = $attributes['event']['Title'] ?? null;

        // 5. SICHERHEITS-FIX (XSS): Links und Link-Texte escapen
        if (!empty($locationData)) {
            $city = htmlspecialchars($locationData);
            $link = $baseUrl . "/map/" . urlencode($locationType) . "/" . urlencode($locationSlug);
            $placePrivacie = "<a href='" . htmlspecialchars($link, ENT_QUOTES, 'UTF-8') . "'>" . htmlspecialchars($locationTitle) . '</a>, öffentlich';
        } elseif (!empty($eventCity)) {
            $city = htmlspecialchars($eventCity);
            $link = WEBSITE_BASE . "/events/" . urlencode($eventSlug);
            $placePrivacie = "<a href='" . htmlspecialchars($link, ENT_QUOTES, 'UTF-8') . "'>" . htmlspecialchars($eventTitle) . '</a>, öffentlich';
        } elseif (!empty($userCity)) {
            $city = htmlspecialchars($userCity);
            $placePrivacie = "Privat";
        }

        $html .= '<div class="card">';
        $html .= '<span><a style="text-decoration:none" href="' . WEBSITE_BASE . 'user/' . $userSlug . '"><i class="fa-solid fa-user"></i> ' . $userName . '</a></span> ';
        $html .= '<span><i class="fa-solid fa-city"></i> ' . $city . '</span> ';
        $html .= '<span><i class="fa-solid fa-building"></i> ' . $placePrivacie . '</span>'; // $placePrivacie ist jetzt sicher

        // 6. PERFORMANCE-FIX (N+1): Aufruf entfernt, stattdessen Variable vergleichen
        if ($userDocumentID == $currentUserDocumentId) {
            // $meetingDocumentId wurde oben bereits escaped
            $html .= '<a href="#" class="delete-meeting" data-meeting="' . $meetingDocumentId . '"><i class="fa-solid fa-trash"></i></a>';
        }

        $html .= "<h3>{$title}</h3>";
        $html .= "<p><strong>Datum:</strong> {$date}</p>";

        if (!empty($description)) {
            $html .= "<p>{$description}</p>";
        }

        // 7. SICHERHEITS-FIX (XSS): $userDocumentID und $meetingDocumentId escapen
        $html .= '<a style="color:black" href="#" class="conversation send-message-button" data-recipient="' . $userDocumentID . '" data-recipient-name="' . $userName . '" data-recipient-avatar="' . $userAvatar . '" data-reference="' . $meetingDocumentId . '"><i class="fa-solid fa-envelope"></i> Antworten</a>';
        $html .= '</div>';
    }
    $html .= '</div>';
    return $html;
}


/**
 * ===================================================================
 * FUNKTION 3 (ANGEPASST): Die "CONTROLLER"-Schicht
 * Deine alte Funktion, die jetzt die neuen Funktionen aufruft.
 * Du musst deine bestehenden Aufrufe nicht ändern.
 * ===================================================================
 */
function get_meetings(?string $type = null, ?string $documentId = null, bool $onlyUnlinked = false): string {

    $jwt = $_SESSION['jwt'] ?? null;
    
    // Früher Abbruch (HTML-Rückgabe) bleibt erhalten
    if (!$jwt) {
        return '<div class="card"><p>Du musst eingeloggt sein, um Spieltreffen zu sehen.</p></div>';
    }
    
    // 1. Daten holen (gibt Array oder false zurück)
    $meetings_data = get_meetings_data($type, $documentId, $onlyUnlinked);
    
    // 2. HTML rendern (die Render-Funktion kann mit 'false' oder '[]' umgehen)
    $html = render_meetings_html($meetings_data, $type, $documentId);
    
    // 3. HTML zurückgeben
    return $html;
}