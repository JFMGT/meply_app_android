<?php ///WIRD DIESE DATEI ÜBERHAUPT EINGETSETZT? 

header('Content-Type: application/json');
include('../functions/renderLikeLink.php');
require_once('../config.php');
session_start();

$documentId = trim($_POST['documentId'] ?? '');
$limit = isset($_POST['limit']) ? intval($_POST['limit']) : 25;
$jwt = $_SESSION['jwt'] ?? null;

// 🛑 Validierung
if (!$documentId) {
    echo json_encode(['html' => '<div class="card">Keine Location-ID übergeben.</div>']);
    exit;
}

// 📡 Events nach Location documentId abfragen
$query = http_build_query([
    'filters[location][documentId][$eq]' => $documentId,
    'sort' => 'start_date:asc',
    'pagination[pageSize]' => 100
]);
$apiUrl = STRAPI_API_BASE . '/events?' . $query;

$response = file_get_contents($apiUrl);
$data = json_decode($response, true);
$events = $data['data'] ?? [];

// 🗓 Nur zukünftige oder heutige Events
$today = strtotime(date('Y-m-d'));
$events = array_filter($events, function ($event) use ($today) {
    $attr = $event['attributes'] ?? [];
    $start = isset($attr['start_date']) ? strtotime($attr['start_date']) : null;
    $end   = isset($attr['end_date']) ? strtotime($attr['end_date']) : null;
    return ($end && $end >= $today) || ($start && $start >= $today);
});

$events = array_slice($events, 0, $limit);

// 💙 Likes prüfen
$documentIds = array_map(fn($e) => $e['attributes']['documentId'] ?? '', $events);
$likedIds = [];
if ($jwt && count($documentIds) > 0) {
    $query = implode('&', array_map(fn($id) => 'documentIds[]=' . urlencode($id), $documentIds));
    $likeCheckUrl = STRAPI_API_BASE . "/likes/has-liked?$query";

    $ch = curl_init($likeCheckUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $jwt",
        "Accept: application/json"
    ]);
    $likeResponse = curl_exec($ch);
    curl_close($ch);

    $likeData = json_decode($likeResponse, true);
    if (!empty($likeData['liked']) && is_array($likeData['liked'])) {
        $likedIds = $likeData['liked'];
    }
}

// 👍 Like-Zähler laden
$likeCounts = [];
if ($jwt) {
    $ch = curl_init(STRAPI_API_BASE . '/like-counts?pagination[pageSize]=1000');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $jwt,
        'Content-Type: application/json',
    ]);
    $response = curl_exec($ch);
    curl_close($ch);

    $result = json_decode($response, true);
    if (!empty($result['data'])) {
        foreach ($result['data'] as $likeEntry) {
            if (isset($likeEntry['targetDocumentId']) && isset($likeEntry['count'])) {
                $likeCounts[$likeEntry['targetDocumentId']] = $likeEntry['count'];
            }
        }
    }
}

// 🧱 HTML-Ausgabe
$html = '';
foreach ($events as $event) {
    $e = $event['attributes'];
    $title = $e['Title'] ?? 'Ohne Titel';
    $slug = $e['slug'] ?? $e['documentId'];
    $city = trim($e['city'] ?? 'Unbekannter Ort');
    $description = $e['description'] ?? 'Keine Beschreibung verfügbar.';
    $start_date = $e['start_date'] ?? null;
    $end_date = $e['end_date'] ?? null;
    $url = $e['url'] ?? '#';
    $image = WEBSITE_BASE . "etc/images/pattern-7451714_1280-768x768.jpg";
    $docId = $e['documentId'] ?? '';
    $likes = $likeCounts[$docId] ?? 0;
    $liked = in_array($docId, $likedIds);
    $meetings = $e['meetingCount'] ?? 0;

    // 📆 Datum formatieren
    $formatted_date = 'Datum folgt';
    if ($start_date && $end_date) {
        $formatted_date = date("d.m.Y", strtotime($start_date)) . " bis " . date("d.m.Y", strtotime($end_date));
    } elseif ($start_date) {
        $formatted_date = date("d.m.Y", strtotime($start_date));
    }

    // 📄 HTML Block
    $html .= "<div class='event'>";
    $html .= "<h2>" . htmlspecialchars($title) . "</h2>";
    $html .= renderLikeLink($docId, 'event', $likes, $liked);
    $html .= "<p><strong><i class='fa-solid fa-location-dot'></i> " . htmlspecialchars($city) . "</strong> ";
    $html .= "<strong><i class='fa-solid fa-calendar-days'></i> " . $formatted_date . "</strong></p>";
    $html .= "<p>" . htmlspecialchars($description) . "</p>";

    if ($url && $url !== '#') {
        $html .= "<p><i class='fa-solid fa-up-right-from-square'></i> <a target='_blank' href='" . htmlspecialchars($url) . "'>zur Website</a></p>";
    }

    $html .= "<img src='{$image}' alt='Event Bild'>";
    $html .= "<p class='cta'><a class='compact' href='/events/{$slug}'>";
    $html .= "<i class='fa-solid fa-users'></i><span> Info & Mitspieler <span class='count'>".$meetings."</span></span></a></p>";
    $html .= "</div>";
}

// 🐞 Debug-Log
file_put_contents('debug-location.log', print_r([
    'documentId' => $documentId,
    'apiUrl' => $apiUrl,
    'events_found' => count($events)
], true));

echo json_encode([
    'html' => $html ?: '<div class="card">Keine Events gefunden.</div>'
]);
