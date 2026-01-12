<?php
include('inc/header.php'); 

// ----------- Daten abrufen ------------------------
$location = $_GET['location'] ?? null;

if (!$location) {
    http_response_code(400);
    die("Fehlender Parameter");
}

// Nutze die neue PUBLIC Route - kein Token nötig!
$apiUrl = STRAPI_API_BASE . '/locations/public/' . urlencode($location);

// cURL Request OHNE Token (public route)
$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200) {
    http_response_code(404);
    die("Kein Eintrag gefunden oder nicht veröffentlicht");
}

$data = json_decode($response, true);

if (empty($data['data'])) {
    http_response_code(404);
    die("Kein Eintrag gefunden");
}

$entry = $data['data'];

// ----------- HTML-Ausgabe ------------------------
function safe($field) {
    return htmlspecialchars($field ?? '', ENT_QUOTES, 'UTF-8');
}

$titel = safe($entry['Titel'] ?? '');
$strasse = safe($entry['Strasse'] ?? '');
$hausnummer = safe($entry['Hausnummer'] ?? '');
$plz = safe($entry['PLZ'] ?? '');
$ort = safe($entry['Ort'] ?? '');
$beschreibung = safe($entry['Beschreibung'] ?? '');
$phone = safe($entry['Telefon'] ?? '');
$mail = safe($entry['Mail'] ?? '');
$website = safe($entry['Website'] ?? '');
$oeffnungszeiten = safe($entry['Oeffnungszeiten'] ?? '');
$id = safe($entry['id'] ?? '');
$documentId = safe($entry['documentId'] ?? '');

$profile = getProfileData();
?>

<div class="content content-plain top-margin">
  <h1><?= $titel ?></h1>
  
  <div class="card">
    <?= displayPostOptions($documentId, 'event'); ?>
    <?php echo renderLikeLink($documentId, 'location'); ?>
    
    <?php if ($beschreibung): ?>
      <p><?= nl2br($beschreibung) ?></p>
    <?php endif; ?>

    <h3>Standort</h3>
    <?= $strasse ?> <?= $hausnummer ?><br>
    <?= $plz ?> <?= $ort ?>

    <?php if ($phone || $mail || $website): ?>
      <h3>Kontakt</h3>
      <?php if ($phone): ?>
        Telefon: <?= $phone ?><br>
      <?php endif; ?>
      <?php if ($mail): ?>
        E-Mail: <a href="mailto:<?= $mail ?>" target="_blank"><?= $mail ?></a><br>
      <?php endif; ?>
      <?php if ($website): ?>
        Website: <a href="<?= $website ?>" target="_blank"><?= $website ?></a><br>
      <?php endif; ?>
    <?php endif; ?>

    <?php if ($oeffnungszeiten): ?>
      <h3>Öffnungszeiten</h3>
      <?= nl2br($oeffnungszeiten) ?>
    <?php endif; ?>

    <h3>Bilder</h3>
    Keine Bilder hinterlegt
  </div>

  <?php
  echo renderCreateMeetingButton('location', $documentId);
  echo get_meetings('location', $documentId);
  ?>

  <h2>Anstehende Veranstaltung</h2>
  <?php
  echo getEventsByLocation($documentId, 5, $_SESSION['jwt'] ?? null);
  ?>
</div>

<?php include('inc/footer.php'); ?>