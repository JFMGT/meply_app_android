<?php
include("../inc/header_auth.php");


$token = $_SESSION['jwt'] ?? null;

$slug = $_GET['slug'] ?? null;
$profileId = $_GET['id'] ?? null;
$curUserId = $_SESSION['profile']['id'];

function getSharedHighlyRatedGames($profileIdA, $profileIdB, $authToken = null) {
    $apiUrl = STRAPI_API_BASE . "/boardgames/shared-highly-rated/{$profileIdA}/{$profileIdB}";
    
    $ch = curl_init($apiUrl);

    $headers = [
        'Accept: application/json',
    ];

    if ($authToken) {
        $headers[] = "Authorization: Bearer {$authToken}";
    }

    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    if (curl_errno($ch)) {
        echo 'cURL-Fehler: ' . curl_error($ch);
        curl_close($ch);
        return [];
    }

    curl_close($ch);

    if ($status !== 200) {
        echo "Fehler beim Abrufen der Empfehlungen (Status $status)";
        return [];
    }

    $games = json_decode($response, true);

    // Maximal 5 zurückgeben
    return array_slice($games, 0, 5);
}

if ($slug && !$profileId) {
    // Neue Custom-Route in Strapi nutzen
    $apiUrl = STRAPI_API_BASE . "/profiles/slug/" . urlencode($slug);

    $ch = curl_init($apiUrl);
    $headers = [
        'Accept: application/json',
        "Authorization: Bearer $token"
    ];
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($status === 200) {
        $data = json_decode($response, true);
        $profile = $data['data'] ?? null;   // Achtung: Strapi packt die Antwort in { data: {...} }
        $profileId = $profile['documentId'] ?? null;
        $profileRealId = $profile['id'] ?? null;

    } elseif ($status === 204) {
        $profile = null; // Profil privat
    } else {
        echo "<p>❌ Fehler beim Abrufen des Profils (Status $status)</p>";
        $profile = null;
    }
}


function getAgeFromBirthdate($birthDate) {
    if (!$birthDate) return null;

    $birth = new DateTime($birthDate);
    $today = new DateTime();
    $age = $today->diff($birth)->y;

    return $age;
}

?>
<div class="content content-plain top-margin">
<div class="card relative">

<?php  
  if (!$profile) {       
   echo "<p>❌ Benutzer nicht gefunden.</p>";
       
    }elseif (!$profileId) {
    echo "<p>❌ Kein Profil angegeben.</p>";
    
}

?>

<!-- HTML-Ausgabe -->
<div class="profile-card">
 <div class="profile-header">
             
        <?php 
    if (!empty($profile['avatar'][0]['url'])){
        $imgSrc = STRAPI_IMAGE_BASE . $profile['avatar'][0]['url'];
    }else{
        $hash = crc32($profileRealId); // liefert konsistente Ganzzahl
        $index = ($hash % 8) + 1; // 1–5
        $imgSrc = WEBSITE_BASE . "/etc/images/avatar{$index}.png";
        
        
         
   
    
         
    } 

$genderLabels = [
    'male' => '<i class="fa-solid fa-mars"></i>',
    'female' => '<i class="fa-solid fa-venus"></i>',
    'diverse' => '<i class="fa-solid fa-non-binary"></i>',
    'trans' => '<i class="fa-solid fa-transgender"></i>',
    'other' => '<i class="fa-solid fa-otter"></i>',
    'none' => '<i class="fa-solid fa-otter"></i>',
];
$genderLabel = $genderLabels[$profile['gender']] ?? ucfirst($profile['gender']);

    ?>
        <div class="profile-avatar">
            <img src="<?= $imgSrc ?>" 
                 alt="Avatar von <?= htmlspecialchars($profile['username']) ?>" 
                 style="width: 100px; height: 100px; border-radius: 15px; object-fit: cover; border: 2px solid #fff;">
        </div>
    


    <div class="profile-header-text">
        <h2 style="margin: 0; margin-top:5px">
          <?= htmlspecialchars($profile['username']) ?>
          <?php 
            $age = getAgeFromBirthdate($profile['birthDate'] ?? null); 
            $details = [];

                if ($age) {
                    $details[] = $age;
                }
                if ($genderLabel) {
                    $details[] = $genderLabel;
                }

                if (!empty($details)) {
                    echo '<span style="font-size:0.75em">(' . implode(', ', $details) . ')</span>';
                } ?>
        </h2>
        <?php
            // Avatar für Modal vorbereiten
            $avatarUrl = !empty($profile['avatar'][0]['url'])
                ? STRAPI_IMAGE_BASE . $profile['avatar'][0]['url']
                : WEBSITE_BASE . "/etc/images/avatar" . ((crc32($profileRealId) % 8) + 1) . ".png";
        ?>
        <a style="margin-top:25px" href="#" class="conversation send-message-button"
           data-recipient="<?= $profileId; ?>"
           data-recipient-name="<?= htmlspecialchars($profile['username']) ?>"
           data-recipient-avatar="<?= htmlspecialchars($avatarUrl) ?>">
            <i class="fa-solid fa-envelope"></i> Nachricht schreiben
        </a>
    </div>
</div>
<?php 
$match = getMatchScore($profile['id'], $curUserId);

echo displayPostOptions($profile['userDocumentId'], 'user');
if($profile['allowProfileView']){ 
    echo "<div class='userbar'>";
    echo "<div class='userscore'>";
if (isset($match['response']['score'])) {
    
    echo "<span title='Matching-Score'><i class='fa-solid fa-people-arrows'></i> " . round($match['response']['score'] * 100) . "%</span>";
    echo "<span title='Gemeinsame Bewertungen'><i class='fa-solid fa-puzzle-piece'></i> " . intval($match['response']['sharedCount']) . "</span>";
    echo "<span title='ungefähre Distanz'><i class='fa-solid fa-location-dot'></i> " . $match['response']['distance'] . " km</span>";
   
}else{
    echo "<span title=''><i class='fa-solid fa-circle-question'></i> Nicht genug Daten verfügbar</span>";
}



 echo "</div>"; 

 echo "<div class='userfollow douserfollow' data-document-id='". $profile['userDocumentId'] ."'>";
  $isFollower = checkFollowStatus('current', $profile['userDocumentId']);
if($isFollower){
    echo "<span title=''><i class='fa-solid fa-minus'></i> Entfolgen</span>";
}else{
   echo "<span title=''><i class='fa-solid fa-plus'></i> Folgen</span>";
}

 echo "</div>";
 echo "</div>";



?>

<ul class="user-profile-menu">
  <li><a href="#" class="active" data-tab="vergleich">Vergleich</a></li>
  <li><a href="#" data-tab="flohmarkt">Flohmarkt</a></li>
  <li><a href="#" data-tab="beitraege">Beiträge</a></li>
  <li><a href="#" data-tab="gesuche">Gesuche</a></li>
</ul>

<?php

$profileIdCurrentUser = $_SESSION['profile']['id'];
$profileIdVisitedUser = $profileId;
echo"<div id='tab-vergleich' class='tab-content' style='display:block;'>";
echo"<div class='userinfo'>";
echo"<div class='userdata'>";
 ?>

    <?php if (!empty($profile['postalCode']) || !empty($profile['city'])): ?>
        <p>📍<?= htmlspecialchars(trim($profile['postalCode'] . ' ' . $profile['city'])) ?></p>
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
    <?php endif; 


$sharedGames = getSharedHighlyRatedGames($profileIdCurrentUser, $profileRealId, $token);
function renderStars($rating) {
    $stars = "";
    for ($i = 1; $i <= 5; $i++) {
        $stars .= $i <= $rating ? "★" : "☆";
    }
    return $stars;
}
// Ausgabe als HTML
echo"</div>";
echo "<div class='shared_games'>";
echo "<strong>Spiele die ihr beide mögt:</strong><br><ul style='list-style: none; padding-left: 0;'>";
if (!empty($sharedGames) && is_array($sharedGames)) {
foreach ($sharedGames as $game) {
    $title = htmlspecialchars($game['boardgame']['title']);
    $ratingB = intval($game['ratingB']); // Bewertung von BPuenktchen

    echo "<li style='margin: 4px 0;'>
            <span style='display: inline-block; width: 250px;'>$title</span><span style='color: gold;'>".renderStars($ratingB)."</span>
          </li>";
}
}else{
   echo "Gemeinsame Spiele werden nur angezeigt, wenn beide ihre Bewertungen teilen.";
}
echo "</ul></div>";
echo"</div>";
echo"</div>";


echo"<div id='tab-flohmarkt' class='tab-content' style='display:none;'>";
$salesData = getUserSales($profileRealId, $token);

if (!empty($salesData)) {
    echo "<div class='shared_sales'>";
    echo "<h3>Bietet aktuell zum Verkauf an:</h3><br><ul style='list-style: none; padding-left: 0;'>";

    foreach ($salesData as $sale) {
        $title = htmlspecialchars($sale['title']);
        $price = $sale['price'] ? number_format($sale['price'], 2, ',', '.') . " €" : "–";
        $cond = htmlspecialchars($sale['condition'] ?? '');
        $delivery = htmlspecialchars($sale['deliveryOption'] ?? '');
        $trade = $sale['tradePossible'] ? '🔁 Tausch möglich' : '';

        echo "<li style='margin: 4px 0;'>
                <span style='display: inline-block; width: 250px;'>$title</span>
                <small>$price • $cond • $delivery $trade</small>
              </li>";
    }

    echo "</ul></div>";
} else {
    echo "<div class='shared_sales'><em>Bietet derzeit keine Spiele zum Verkauf an.</em></div>";
}

echo"</div>";


echo"<div id='tab-beitraege' class='tab-content' style='display:none;'>";
?>

<div class="user-posts" >
    <h3>Letzten Beiträge des Nutzers</h3>


  


    <div class="feed" style="margin-top:0; margin-bottom:50px">
    </div>   
    <button id="load-more" type="button" class="load-more">Weitere Beiträge laden</button> 
     <div class="thread-view" style="display:none;">
    <button class="back-to-feed"><img src="<?= $baseUrl ?>/etc/images/back_arrow.png"> Post</button>
    <div class="thread-content"></div>
  </div>
       

</div> 
<?php

echo"</div>";

echo"<div id='tab-gesuche' class='tab-content' style='display:none;'>";
?>
<div class="user-meetings">
    <h3>Suchanfragen des Nutzers</h3>
    <?php echo get_meetings('author', $profileId); ?>
</div>
<style>
    .hold-meetings .card{
            border: 1px solid #fec50d;
    }
</style>
<?php
echo"</div>";


}else{
    echo "<p>❌ Keine Informationen zu diesem Profil.</p>";
}


if (!$profileId || !$token) {
    echo "<p>❌ Kein Profil oder Token vorhanden.</p>";
    return;
}?>
</div></div>

 

<!-- HTML-Ausgabe -->
    



   <script>
document.addEventListener('DOMContentLoaded', () => {
  const feedEl   = document.querySelector('.feed');
  const moreBtn  = document.querySelector('#load-more');
  const threadEl = document.querySelector('.thread-view');
  const threadContentEl = threadEl.querySelector('.thread-content');
  const backBtn  = threadEl.querySelector('.back-to-feed');

  let before = null;        // Cursor vom Server
  let isLoading = false;
  let allLoaded = false;

  // Initial: 2 Beiträge
  loadChunk(2);

  // Button: 5 weitere
  moreBtn.addEventListener('click', () => loadChunk(5));

  function loadChunk(limit) {
    if (isLoading || allLoaded) return;
    isLoading = true;
    moreBtn.disabled = true;
    moreBtn.textContent = 'Laden...';

    const params = new URLSearchParams({
      author: '<?= htmlspecialchars($profileId, ENT_QUOTES) ?>',
      limit: String(limit),
    });
    if (before) params.set('before', before);

    fetch('<?= $baseUrl ?>/inc/api/feedByUser.php?' + params.toString())
      .then(res => res.text())
      .then(html => {
        if (!html) {
          // Keine weiteren Inhalte
          allLoaded = true;
          moreBtn.style.display = 'none';
          return;
        }

        const temp = document.createElement('div');
        temp.innerHTML = html;

        // Posts anhängen
        temp.querySelectorAll('[data-post-id]').forEach(post => {
          feedEl.appendChild(post);
        });

        // Cursor übernehmen
        const marker = temp.querySelector('#cursor');
        if (marker) {
          before = marker.dataset.oldest || null;
          const hasMore = marker.dataset.hasmore === '1';
          if (!hasMore || !before) {
            allLoaded = true;
            moreBtn.style.display = 'none';
          }
        } else {
          // Kein Cursor => wir nehmen an, es gibt nichts mehr
          allLoaded = true;
          moreBtn.style.display = 'none';
        }
      })
      .catch(err => {
        console.error('❌ Fehler beim Laden:', err);
        // Button wieder aktivieren, damit der User nochmal probieren kann
      })
      .finally(() => {
        isLoading = false;
        if (!allLoaded) {
          moreBtn.disabled = false;
          moreBtn.textContent = 'Weitere Beiträge laden';
        }
      });
  }

  // Thread öffnen (Event Delegation, robust)
  feedEl.addEventListener('click', (e) => {
    const link = e.target.closest('.show-replys');
    if (!link) return;
    e.preventDefault();
    const postId = link.dataset.documentid;
    if (postId) showThread(postId);
  });

  function showThread(documentId) {
    feedEl.style.display = 'none';
    threadEl.style.display = 'block';
    threadContentEl.innerHTML = '<p>Lade Diskussion...</p>';
    history.pushState({ view: 'thread' }, '', '#thread-' + documentId);

    fetch('<?= $baseUrl ?>/inc/api/thread.php?documentId=' + encodeURIComponent(documentId))
      .then(res => res.text())
      .then(html => { threadContentEl.innerHTML = html; })
      .catch(err => {
        threadContentEl.innerHTML = '<p class="error">❌ Fehler beim Laden</p>';
        console.error(err);
      });
  }

  backBtn.addEventListener('click', () => {
    showFeed();
    history.pushState({ view: 'feed' }, '', '/');
  });

  window.addEventListener('popstate', () => { showFeed(); });

  function showFeed() {
    threadEl.style.display = 'none';
    feedEl.style.display = 'flex';
  }
});


document.querySelectorAll('.user-profile-menu a').forEach(link => {
  link.addEventListener('click', e => {
    e.preventDefault();
    document.querySelectorAll('.user-profile-menu a').forEach(a => a.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(tab => tab.style.display = 'none');

    link.classList.add('active');
    document.getElementById('tab-' + link.dataset.tab).style.display = 'block';
  });
});

</script>


    
<style>

.load-more { padding:8px 14px; border:1px solid #ccc; border-radius:6px; background:#fff; cursor:pointer }
.load-more[disabled] { opacity: .6; cursor: not-allowed }
    
  .feed, .thread-view {
    margin-bottom:25px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 600px;
  width:100%;
}

.post {
  color:black;
}

.post .icon-bar{
  position: absolute;
  right:10px;
  top:10px;
}

.post .meta {
  font-size: 0.9em;
  color: #666;
  margin-bottom: 0.5rem;
}

</style>


</div>

<?php include("../inc/footer.php"); ?>