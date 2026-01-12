<?php
include("../inc/header_auth.php");
?>

<div class="content content-plain top-margin">
<?php 

// Konfiguration
$strapiUrl = STRAPI_API_BASE;
$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
  echo json_encode(['error' => 'Nicht eingeloggt']);
  exit;
}

$userId = $_SESSION['profile']['id']; // Benutzerprofil-ID in Strapi
$profileId = getProfileData(true);
?>


<?php
// API-Aufruf vorbereiten
$limit = 5;
$url = "$strapiUrl/matches/best/$userId?limit=$limit";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
  "Authorization: Bearer $jwt",
  "Content-Type: application/json",
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Prüfen ob erfolgreich
if ($httpCode !== 200) {
  echo "<p class='error'>Fehler beim Laden der Matches (Code $httpCode)</p>";
  exit;
}
?>
<h1>Dashboard</h1>

<div class="container">

  <div class="column-100">
    <div class="card">
      <h2>Willkommen zurück <?= $_SESSION['profile']['username'] ?></h2>
<p>Schön, dass du wieder da bist. Entdecke neue Events, finde Mitspieler und teile deine Leidenschaft für Brettspiele.
   </p> </div>
  </div>
  <div class="column-50">
    <?php
    $matches = json_decode($response, true);
echo"<div class='card'>";
// Darstellung
echo "<h2>User in deiner Nähe</h2>";

if (empty($matches)) {
  echo "<p>Keine passenden Mitspieler gefunden.</p>";
} else {

  foreach ($matches as $match) {
    $user = $match['otherProfile'] ?? [];
$score = isset($match['score']) ? round($match['score'] * 100) : '?';

    $distance = $match['distance'] ?? '?';
    $name = $user['username'] ?? 'Unbekannt';
    $profileId = $user['userslug'] ?? null;

    echo "<div>";
    echo "<strong>$name</strong><br>";
    echo "Übereinstimmung: <strong>$score%</strong><br>";
    echo "Entfernung: <strong>$distance km</strong><br>";
    if ($profileId) {
      echo "<a style='text-decoration:none' href='$baseurl/user/$profileId'>Zum Profil</a>";
    }
    echo "</div><hr>";
  }

} echo"</div>";
    ?>
  </div>
    <div class="column-50">
   <div class="card">
  <h2>Spielempfehlungen</h2>
  <?php
  $recommendUrl = "$strapiUrl/boardgames/recommendations/";

  $ch = curl_init($recommendUrl);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Content-Type: application/json",
  ]);

  $recommendResponse = curl_exec($ch);
  $recommendHttpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
  curl_close($ch);

  if ($recommendHttpCode !== 200) {
    echo "<p class='error'>Fehler beim Laden der Empfehlungen (Code $recommendHttpCode)</p>";
  } else {
    $recommendations = json_decode($recommendResponse, true);

    if (empty($recommendations)) {
      echo "<p>Keine Empfehlungen gefunden.</p>";
    } else {
      foreach ($recommendations as $entry) {
        $game = $entry['game'];
        $reasons = $entry['recommendedBecause'];

        $title = htmlspecialchars($game['title'] ?? 'Unbenannt');
        $minAge = $game['min_age'] ?? null;
        $minPlayer = $game['min_player'] ?? null;
        $maxPlayer = $game['max_player'] ?? null;

        echo "<div class='recommendation'>";
        echo "<strong>$title</strong><br>";
        if ($minAge !== null) echo "Ab $minAge Jahren<br>";
        if ($minPlayer !== null && $maxPlayer !== null) {
          echo "Spieler: $minPlayer–$maxPlayer<br>";
        }

        // Empfehlungen durch andere Nutzer anzeigen
        if (!empty($reasons)) {
        echo "<div class='recommendation-list'>";

foreach ($reasons as $reason) { 
  $user = htmlspecialchars($reason['username'] ?? 'Unbekannt');
  if($user == "Unbekannt" || $user == "Versteckt"){
    $user = "einem Benutzer";
  }


  $slug = htmlspecialchars($reason['userslug'] ?? '');
  $rating = intval($reason['rating']);

  echo "<div class='recommendation-entry'>";
  echo "<span class='rating'>★ $rating Sterne von</span>";
  if ($slug) {
    echo "<a class='user-link' href=\"/user/$slug\">$user</a>";
  } else {
    echo "<span class='user-link'>$user</span>";
  }
  echo "</div>";
}
echo "</div>";

        }

        echo "</div><hr>";
      }
    }
  }
  ?>
</div>

  </div>

</div>



</div>
<?php include("../inc/footer.php"); ?>
