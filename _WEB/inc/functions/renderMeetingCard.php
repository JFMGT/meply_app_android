<?php
// require_once('getProfileData.php'); // ENTFERNT (Performance-Fix)

require_once __DIR__ . '/../config.php'; // Nötig für Konstanten und debugLog
require_once __DIR__ . '/../functions.php'; // Nötig für Konstanten und debugLog

/**
 * Rendert eine einzelne "Meeting-Karte" als HTML.
 * Diese Funktion ist sicherheitsgehärtet und für den Aufruf in einer Schleife optimiert.
 *
 * @param array $meeting Das Meeting-Daten-Array von Strapi.
 * @param string|null $currentUserDocumentId Die ID des eingeloggten Benutzers (Performance-Fix).
 * @return string Das gerenderte HTML.
 */
function renderMeetingCard(array $meeting, ?string $currentUserDocumentId = null): string {

    $m = $meeting;
    $meetingId = $m['id'];
    
    // -----------------------------------------------------------------
    // SICHERHEITS-FIX (XSS): Alle data-Attribute MÜSSEN escaped werden.
    // -----------------------------------------------------------------
    $meetingDocumentId = esc($m['documentId'] ?? null); 
    
    $attributes = $m ?? [];
    $title = esc($attributes['title'] ?? 'Ohne Titel');
    $description = nl2br(esc($attributes['description'] ?? ''));
    
    // ... (Deine gesamte Datums-Logik ist in Ordnung und bleibt unverändert) ...
    $date = 'Kein Datum angegeben';
    if (!empty($attributes['dates']) && is_array($attributes['dates']) && !empty($attributes['dates']['value'])) {
    $dateType = $attributes['dates']['type'] ?? '';
    $value = $attributes['dates']['value'] ?? [];
    switch ($dateType) {
        case 'fixed':
          if (!empty($value['date'])) { $date = date('d.m.Y H:i', strtotime($value['date'])); }
          break;
        case 'range':
          if (!empty($value['start']) && !empty($value['end'])) {
            $start = date('d.m.Y', strtotime($value['start']));
            $end = date('d.m.Y', strtotime($value['end']));
            $date = "Zeitraum: {$start} – {$end}";
          }
          break;
        case 'recurring':
          $days = $value['days'] ?? []; $freq = $value['frequency'] ?? 'wöchentlich';
          $dayNames = ['Sonntag','Montag','Dienstag','Mittwoch','Donnerstag','Freitag','Samstag'];
          $labels = [];
          foreach ($days as $day) { $labels[] = isset($dayNames[$day]) ? $dayNames[$day] : esc($day); }
          $dayList = implode(', ', $labels); $freqLabel = ucfirst($freq);
          $date = "{$freqLabel} an: {$dayList}";
          break;
        case 'eventDays':
          $days = $value['days'] ?? [];
          if (!empty($days)) {
            $formattedDays = array_map(fn ($d) => date('d.m.Y', strtotime($d)), $days);
            $date = 'Beim Event an: ' . implode(', ', $formattedDays);
          }
          break;
    }
    }
    if ($date === 'Kein Datum angegeben' && !empty($attributes['date'])) {
    $date = date('d.m.Y H:i', strtotime($attributes['date']));
    }
    // ... (Ende Datums-Logik) ...

    $userName = esc($attributes['author']['username'] ?? 'Unbekannt');
    $userSlug = esc($attributes['author']['userslug'] ?? $attributes['author']['documentId']);
    $userDocumentID = esc($attributes['author']['documentId'] ?? 'Unbekannt');

    // Avatar für Modal vorbereiten
    $userAvatar = '';
    if (!empty($attributes['author']['avatar'][0]['url'])) {
        $userAvatar = esc(STRAPI_IMAGE_BASE . $attributes['author']['avatar'][0]['url']);
    } elseif (!empty($attributes['author']['id'])) {
        $hash = crc32($attributes['author']['id']);
        $index = ($hash % 8) + 1;
        $userAvatar = esc(WEBSITE_BASE . "/etc/images/avatar{$index}.png");
    } else {
        // Fallback wenn keine ID vorhanden
        $userAvatar = esc(WEBSITE_BASE . "/etc/images/avatar1.png");
    }
    
    $city = 'Unbekannt';
    $placePrivacie = "Unbekannt";
    $baseurl = WEBSITE_BASE;

    // Alle Link-Bestandteile sicher escapen
    $locationData = $attributes['location']['Ort'] ?? null;
    $locationTitle = $attributes['location']['Titel'] ?? null;
    $locationSlug = $attributes['location']['slug'] ?? null;
    $locationType = $attributes['location']['Typ'] ?? null;

    $eventCity = $attributes['event']['city'] ?? null;
    $eventSlug = $attributes['event']['slug'] ?? null;
    $eventTitle = $attributes['event']['Title'] ?? null;

    $userCity = $attributes['author']['city'] ?? null;

    if (!empty($locationData)) {
        $city = esc($locationData);
        // urlencode für Pfad-Teile, esc für href-Attribut
        $link = esc($baseurl . "/map/" . urlencode($locationType) . "/" . urlencode($locationSlug));
        $placePrivacie = "<a href='$link'>" . esc($locationTitle) . '</a>, öffentlich';
    } elseif (!empty($eventCity)) {
        $city = esc($eventCity);
        $link = esc($baseurl . "/events/" . urlencode($eventSlug));
        $placePrivacie = "<a href='$link'>" . esc($eventTitle) . '</a>, öffentlich';
    } elseif (!empty($userCity)) {
        $city = esc($userCity);
        $placePrivacie = "Privat";
    }
    
    $html = '<div class="card">';
    
    // -----------------------------------------------------------------
    // HTML-FIX: <span> und <a> korrekt verschachtelt
    // -----------------------------------------------------------------
    $html .= '<span><a style="text-decoration:none" href="'. WEBSITE_BASE .'user/'. $userSlug .'"><i class="fa-solid fa-user"></i> ' . $userName . '</a></span> ';
    
    // -----------------------------------------------------------------
    // ✨ NEU: Match-Badge direkt nach Username
    // -----------------------------------------------------------------
    $matchData = $attributes['matchData'] ?? null;
    if ($matchData && isset($matchData['score'])) {
        $score = round($matchData['score'] * 100); // 0.85 → 85
        $sharedCount = $matchData['sharedCount'] ?? 0;
        $distance = $matchData['distance'] ?? null;
        
        // Nur ab 50% Match zeigen (vermeidet schlechte Matches)
        if ($score >= 50) {
            // Badge-Klasse je nach Score
            $badgeClass = 'match-badge';
            if ($score >= 80) {
                $badgeClass .= ' match-high'; // Grün
            } elseif ($score >= 65) {
                $badgeClass .= ' match-medium'; // Blau
            }
            
            // Tooltip-Text aufbauen (mit Sicherheits-Escaping)
            $tooltipParts = [];
            $tooltipParts[] = $sharedCount . ' gemeinsame Spiele';
            if ($distance !== null) {
                $tooltipParts[] = round($distance, 1) . ' km';
            }
            $tooltip = esc(implode(' • ', $tooltipParts));
            
            $html .= '<span class="' . esc($badgeClass) . '" title="' . $tooltip . '">';
            $html .= '<i class="fa-solid fa-people-arrows"></i> ' . esc($score) . '%';
            $html .= '</span> ';
        }
    }
    
    $html .= '<span><i class="fa-solid fa-city"></i> ' . $city . '</span> ';
    $html .= '<span><i class="fa-solid fa-building"></i> ' . $placePrivacie . '</span>';

    // -----------------------------------------------------------------
    // PERFORMANCE-FIX: Verwendet den übergebenen Parameter statt API-Aufruf
    // -----------------------------------------------------------------
    if ($currentUserDocumentId && $userDocumentID == $currentUserDocumentId) {
        // $meetingDocumentId wurde oben bereits sicher escaped
        $html .= '<a href="#" class="delete-meeting" data-meeting="' . $meetingDocumentId . '"><i class="fa-solid fa-trash"></i></a>';
    }

    $html .= "<h3>{$title}</h3>";
    $html .= "<p><strong>Datum:</strong> {$date}</p>";
    if (!empty($description)) {
        $html .= "<p>{$description}</p>";
    }
    
    // $userDocumentID und $meetingDocumentId wurden oben bereits sicher escaped
    $html .= "<a style='color:black' href='#' class='conversation send-message-button' data-recipient='" . $userDocumentID . "' data-recipient-name='" . $userName . "' data-recipient-avatar='" . $userAvatar . "' data-reference='" . $meetingDocumentId . "'><i class='fa-solid fa-envelope'></i> Antworten</a>";
    $html .= '</div>';
    
    return $html;
}