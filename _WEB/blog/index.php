<?php 
// ARCHITEKTUR: functions.php für esc() und debugLog() laden
require_once('../inc/functions.php');
include('../inc/header.php'); 
?>

<div class="content top-margin content-plain">
  <h1>Blog Übersicht</h1>

  <?php
  $currentPage = isset($_GET['page']) ? max(1, intval($_GET['page'])) : 1;
  $pageSize = 5; 

  // --- API-Aufruf (Zurück zum Original-Stil, falls Token das Problem war) ---
  // Wir nutzen trotzdem cURL statt file_get_contents für Basis-Robustheit
  $url = STRAPI_API_BASE . "/blogposts?sort=createdAt:desc&pagination[page]=$currentPage&pagination[pageSize]=$pageSize";
  
  $ch = curl_init($url);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  curl_setopt($ch, CURLOPT_TIMEOUT, 5);
  // HINWEIS: Authorization-Header entfernt, da es vorher ohne lief.
  // Falls Sie doch Auth brauchen, fügen Sie die Zeilen wieder ein.
  
  $response = curl_exec($ch);
  $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
  curl_close($ch);

  if ($httpCode !== 200 || !$response) {
      // Fehler leise loggen, dem User nur "nichts gefunden" zeigen oder Fehlermeldung
      debugLog("Blog Error: $httpCode");
      echo "<p>Keine Blogbeiträge gefunden.</p>";
      $data = [];
  } else {
      $data = json_decode($response, true);
  }
  // ------------------------------------------------------------

  if (empty($data['data'])) {
      echo "<p>Keine Blogbeiträge gefunden.</p>";
  } else {
      foreach ($data['data'] as $post) {
          // Original-Logik für Datenstruktur beibehalten
          $attributes = $post; 

          // --- SICHERHEITS-FIXES (XSS) ---
          // Hier war die kritische Lücke! Wir escapen jetzt alles.
          $title = esc($attributes['title'] ?? 'Ohne Titel');
          $excerpt = esc($attributes['excerpt'] ?? ''); // <-- WICHTIGSTER FIX
          $slugRaw = $attributes['slug'] ?? '';
          $url = "/blog/" . urlencode($slugRaw); // <-- WICHTIG: urlencode für Pfade
          // -------------------------------

          echo "<div class='card'>";
          echo "<h2>{$title}</h2>";
          echo "<div class='teaser'><p>{$excerpt}</p></div>";
          echo "<a href='{$url}' class='read-more'>Weiterlesen</a>";
          echo "</div>";
      }

      // Pagination (unverändert)
      $pagination = $data['meta']['pagination'] ?? null;
      if ($pagination && $pagination['pageCount'] > 1) {
          echo "<div class='pagination'>";
          if ($currentPage > 1) {
              echo "<a href='?page=" . ($currentPage - 1) . "' class='prev'>&laquo; Zurück</a>";
          }
          for ($i = 1; $i <= $pagination['pageCount']; $i++) {
              $active = $i === $currentPage ? "class='active'" : "";
              echo "<a href='?page=$i' $active>$i</a>";
          }
          if ($currentPage < $pagination['pageCount']) {
              echo "<a href='?page=" . ($currentPage + 1) . "' class='next'>Weiter &raquo;</a>";
          }
          echo "</div>";
      }
  }
  ?>
</div>

<style>
.pagination {
  margin-top: 2rem;
  text-align: center;
}
.pagination a {
  display: inline-block;
  margin: 0 5px;
  padding: 6px 12px;
  text-decoration: none;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #333;
}
.pagination a.active {
  font-weight: bold;
  background-color: #eee;
  border-color: #999;
}
.pagination a.prev,
.pagination a.next {
  font-weight: bold;
}
</style>

<?php include('../inc/footer.php'); ?>
