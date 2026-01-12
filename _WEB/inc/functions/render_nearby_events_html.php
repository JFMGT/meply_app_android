<?php
// Benötigt: esc.php, renderLikeLink (die korrigierte Version!)
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Rendert das HTML für eine Liste von Event-Karten.
 * Enthält alle Sicherheits-Fixes (XSS).
 *
 * @param array $data Das Ergebnis von get_nearby_events_data().
 * @return string Das gerenderte HTML.
 */
function render_nearby_events_html(array $data): string {

    if (isset($data['error'])) {
        return '<div class="card">' . esc($data['error']) . '</div>';
    }
    if (empty($data['events'])) {
        return '<div class="card">Keine Events gefunden.</div>';
    }

    $events = $data['events'];
    $likedIds = $data['likedIds'];
    $likeCounts = $data['likeCounts'];
    $html = '';

    foreach ($events as $event) {
        // Annahme: Strapi v4 Struktur mit 'attributes' - anpassen falls v3!
        $e = $event['attributes'] ?? $event; // Fallback für v3?
        $documentId = $e['documentId'] ?? ''; // documentId von oberster Ebene
       
        // --- SICHERHEIT: Alle Variablen für HTML escapen ---
        $title = esc($e['Title'] ?? 'Ohne Titel');
        $slug_raw = $e['slug'] ?? $documentId; // Rohdaten für URL
        $city = esc(trim($e['city'] ?? 'Unbekannter Ort'));
        $description = esc($e['description'] ?? 'Keine Beschreibung verfügbar.');
        $url_raw = $e['url'] ?? '#'; // Rohdaten für URL
        $likes = (int)($likeCounts[$documentId] ?? 0);
        $liked = in_array($documentId, $likedIds);
        $meetings_raw = $e['meetingCount'] ?? 0; // Rohdaten

        // Bild-URL (Statisch, wie im Original)
        $image = esc(WEBSITE_BASE . "etc/images/pattern-7451714_1280-768x768.jpg");

        // Datum formatieren (unverändert)
        $start_date = $e['start_date'] ?? null;
        $end_date = $e['end_date'] ?? null;
        $formatted_date = 'Datum folgt';
        if ($start_date && $end_date) {
            $formatted_date = date("d.m.Y", strtotime($start_date)) . " bis " . date("d.m.Y", strtotime($end_date));
        } elseif ($start_date) {
            $formatted_date = date("d.m.Y", strtotime($start_date));
        }
        $formatted_date = esc($formatted_date); // Datum auch escapen

        // SICHERHEIT: URL-Teile und Ausgaben escapen
        $safe_slug = esc($slug_raw);
        $safe_meetings_count = esc((string)$meetings_raw);
        $safe_url = esc($url_raw);
        $safe_docId = esc($documentId); // Escapen für renderLikeLink
        
        $html .= "<div class='event'>";
        $html .= "<h2>" . $title . "</h2>";

        // Ruft die SICHERE Render-Funktion auf (die nur Daten braucht)
        $html .= renderLikeLink($safe_docId, 'event', $likes, $liked);

        $html .= "<p><strong><i class='fa-solid fa-location-dot'></i> " . $city . "</strong> ";
        $html .= "<strong><i class='fa-solid fa-calendar-days'></i> " . $formatted_date . "</strong></p>";
        $html .= "<p>" . $description . "</p>";

        if ($url_raw && $url_raw !== '#') {
            $html .= "<p><i class='fa-solid fa-up-right-from-square'></i> <a target='_blank' href='" . $safe_url . "'>zur Website</a></p>";
        }

        $html .= "<img src='{$image}' alt='Event Bild'>"; // $image wurde oben escaped
        $html .= "<p class='cta'><a class='compact' href='/events/{$safe_slug}'>"; // SICHER
        $html .= "<i class='fa-solid fa-users'></i><span> Info & Mitspieler <span class='count'>".$safe_meetings_count."</span></span></a></p>"; // SICHER
        $html .= "</div>";
    }

    return $html;
}