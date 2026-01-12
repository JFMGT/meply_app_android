<?php
session_start();
include('inc/header.php');
// ----------- Daten abrufen ------------------------
$slugOrId = $_GET['slug'] ?? null;

if (!$slugOrId) {
    http_response_code(400);
    die("Fehlender Parameter");
}

$baseUrl = STRAPI_API_BASE . '/events/';

// 1. Versuch: Suche über Slug
$slugUrl = $baseUrl . '?filters[slug][$eq]=' . urlencode($slugOrId) . '&populate[location][populate]=*&populate[organizer][populate]=*';
$jwt = STRAPI_API_TOKEN; // Aus config.php oder dynamisch übergeben

$opts = [
    "http" => [
        "method" => "GET",
        "header" => "Authorization: Bearer $jwt\r\n" .
                    "Accept: application/json\r\n"
    ]
];

$context = stream_context_create($opts);
$response = @file_get_contents($slugUrl, false, $context);
$data = json_decode($response, true);

$data = json_decode($response, true);

// Wenn Slug funktioniert hat
if (!empty($data['data'][0])) {
    $entry = $data['data'][0];
    $methode = 'slug';
} else {
    // 2. Versuch: Direkt über documentId
    $docIdUrl = $baseUrl . urlencode($slugOrId) . '?populate[location][populate]=*&populate[organizer][populate]=*';
    $opts = [
    "http" => [
        "method" => "GET",
        "header" => "Authorization: Bearer " . STRAPI_API_TOKEN . "\r\n" .
                    "Accept: application/json\r\n"
    ]
];
$context = stream_context_create($opts);
$response = @file_get_contents($docIdUrl, false, $context);
$data = json_decode($response, true);

    $data = json_decode($response, true);

    if (!empty($data['data'])) {
        $entry = $data['data'];
        $methode = 'documentId';
    } else {
        http_response_code(404);
        die("Kein Event gefunden");
    }
}

// ----------- Werte extrahieren ------------------------
function safe($field) {
    return htmlspecialchars($field ?? '', ENT_QUOTES, 'UTF-8');
}

$attributes = $entry ?? [];

$title          = safe($attributes['Title']);
$street         = safe($attributes['street']);
$streetNumber   = safe($attributes['street_number']);
$zip            = safe($attributes['zip']);
$city           = safe($attributes['city']);
$description    = nl2br(safe($attributes['description']));
$url            = safe($attributes['url']);
$documentId     = safe($attributes['documentId']);
$likes          = (int)($attributes['likes'] ?? 0);
$slug           = safe($attributes['slug']);
$fixedDate      = $attributes['fixed_date'] ?? false;
$datePlaceholder= safe($attributes['date_placeholder']);

// Datum vorbereiten
$startDate      = $attributes['start_date'] ?? null;
$startTime      = $attributes['start_time'] ?? null;
$endDate        = $attributes['end_date'] ?? null;
$endTime        = $attributes['end_time'] ?? null;

$formattedDate = 'Termin folgt';
if ($fixedDate) {
    if ($startDate) {
        $formattedDate = date("d.m.Y", strtotime($startDate));
        if ($endDate && $endDate !== $startDate) {
            $formattedDate .= ' – ' . date("d.m.Y", strtotime($endDate));
        }

        if ($startTime) {
            $formattedDate .= ' | ' . date("H:i", strtotime($startTime));
            if ($endTime && $endTime !== $startTime) {
                $formattedDate .= ' – ' . date("H:i", strtotime($endTime));
            }
        }
    }
} else {
    $formattedDate = $datePlaceholder ?: 'Termin folgt';
}

// Verknüpfte Location & Organizer
$locationName = safe($attributes['location']['data']['attributes']['Titel'] ?? '');
$organizerName = safe($attributes['organizer']['data']['attributes']['Titel'] ?? '');
?>


<div class="content content-plain top-margin">
  <h1><?= $title ?></h1>

  <div class="card">
    <?= displayPostOptions($documentId, 'event'); ?>
    <?= renderLikeLink($documentId, 'event', $likes); ?>

    <?php if ($description): ?>
      <p><?= $description ?></p>
    <?php endif; ?>

    <?php if ($formattedDate): ?>
      <h3>Datum</h3>
      <p><?= $formattedDate ?></p>
    <?php endif; ?>

    <h3>Ort</h3>
    <p><?= "{$street} {$streetNumber}<br>{$zip} {$city}" ?></p>

    <?php if ($locationName): ?>
      <p><strong>Location:</strong> <?= $locationName ?></p>
    <?php endif; ?>

    <?php if ($organizerName): ?>
      <p><strong>Veranstalter:</strong> <?= $organizerName ?></p>
    <?php endif; ?>

    <?php if ($url): ?>
      <h3>Weitere Infos</h3>
      <p><a target="_blank" href="<?= $url ?>"><?= $url ?></a></p>
    <?php endif; ?>
  </div>

 

<?php
echo renderCreateMeetingButton('event', $documentId, $fixedDate, $title);
?>
<?php
echo get_meetings('event', $documentId);
?>


</div>
<?php include('inc/footer.php'); ?>
