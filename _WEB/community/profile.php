<?php
include("../inc/header_auth.php");
?>

<div class="content content-plain top-margin">


<?php
$profileId = $_GET['id'] ?? null;
if (!$profileId) {
    echo "<p>❌ Kein Profil angegeben.</p>";
    return;
}


// Strapi API-Endpunkt
$apiUrl = $adminBaseurl . "/api/profiles/" . urlencode($profileId);
$token = $_SESSION['jwt'] ?? null;

// cURL vorbereiten
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

// Auth-Header mit Bearer Token aus der Session
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $token"
]);
$response = curl_exec($ch);
curl_close($ch);

// JSON dekodieren
$data = json_decode($response, true);

// Fehlerbehandlung
if (!isset($data['data'])) {
    echo "<p>❌ Profil nicht gefunden.</p>";
    return;
}

$profile = $data['data'];

?>

<!-- HTML-Ausgabe -->
<div class="profile-card">
    <h2><?= htmlspecialchars($profile['username']) ?></h2>
   

    <?php if (!empty($profile['birthDate'])): ?>
        <p>🎂 <strong>Geburtsdatum:</strong> <?= htmlspecialchars($profile['birthDate']) ?></p>
    <?php endif; ?>

    <?php if (!empty($profile['gender'])): ?>
        <p>🚻 <strong>Geschlecht:</strong> <?= htmlspecialchars($profile['gender']) ?></p>
    <?php endif; ?>

    <?php if (!empty($profile['postalCode']) || !empty($profile['city'])): ?>
        <p>📍 <strong>Ort:</strong> <?= htmlspecialchars(trim($profile['postalCode'] . ' ' . $profile['city'])) ?></p>
    <?php endif; ?>

    <?php if (!empty($profile['boardgamegeekProfile']) || !empty($profile['boardGameArenaProfile'])): ?>
        <div style="margin-top: 1rem;">
            <strong>🎲 Brettspiel-Profile:</strong>
            <ul style="list-style: none; padding-left: 0;">
                <?php if (!empty($profile['boardgamegeekProfile'])): ?>
                    <li>🧠 <a href="<?= htmlspecialchars($profile['boardgamegeekProfile']) ?>" target="_blank" rel="noopener">BoardGameGeek</a></li>
                <?php endif; ?>
                <?php if (!empty($profile['boardGameArenaProfile'])): ?>
                    <li>🌐 <a href="<?= htmlspecialchars($profile['boardGameArenaProfile']) ?>" target="_blank" rel="noopener">Board Game Arena</a></li>
                <?php endif; ?>
            </ul>
        </div>
    <?php endif; ?>
</div>

<?php
if (!$profileId || !$token) {
    echo "<p>❌ Kein Profil oder Token vorhanden.</p>";
    return;
}

// Strapi-API: Alle Posts des Autors abrufen
$apiUrl = $adminBaseurl . "/api/posts?filters[author][documentId][\$eq]=$profileId&sort=createdAt:desc&populate=*";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $token"
]);

$response = curl_exec($ch);
curl_close($ch);

$data = json_decode($response, true);

$posts = $data['data'] ?? [];
?>

<!-- HTML-Ausgabe -->
<div class="user-posts">
    <h3>📚 Beiträge des Nutzerss</h3>


    <div class="feed" data-author-id="<?= htmlspecialchars($_GET['id']) ?>"></div>

    </div>    
            
<div class="user-meetings">
    <?php print_r($profile); ?>
    <?php get_meetings('author', $profile['documentId']); ?>
</div>
    
<script>
document.addEventListener('DOMContentLoaded', () => {
  const authorId = document.querySelector('.feed').dataset.authorId;
  const url = '../inc/api/feed.php' + (authorId ? `?author=${authorId}` : '');

  fetch(url)
    .then(res => res.text())
    .then(html => {
      document.querySelector('.feed').innerHTML = html;
    })
    .catch(err => {
      document.querySelector('.feed').innerHTML = '<p class="error">❌ Fehler beim Laden des Feeds</p>';
      console.error(err);
    });
});
</script>
</div>

<style>
  .feed {
    margin-top:25px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 600px;
  width:100%;
  color:black;
}

.post {
  border: 1px solid #ccc;
  padding: 1rem;
  border-radius: 8px;
  background: #fff;
  color:black;
}

.post .meta {
  font-size: 0.9em;
  color: #666;
  margin-bottom: 0.5rem;
}

</style>



</div>

<?php include("../inc/footer.php"); ?>