<?php 
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../inc/functions.php'); // Lädt config, Helfer, esc, debugLog etc.
include('../inc/header.php'); 

$slugRaw = $_GET['slug'] ?? null;
$blog = null;
$error = null;

// 1. Slug prüfen
if (!$slugRaw) {
    http_response_code(404);
    $error = "Kein Beitrag gefunden.";
} else {
    // 2. API-Abfrage (ROBUSTHEIT: cURL statt file_get_contents)
    // SICHERHEIT: urlencode für den Slug
    $url = STRAPI_API_BASE . "/blogposts?filters[slug][\$eq]=" . urlencode($slugRaw) . "&populate=author";
    
    // Wir nutzen manuelles cURL, um den Stil beizubehalten, aber sicher.
    // Besser wäre: $result = makeStrapiApiCall('GET', $url);
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    // Optional: Falls Auth nötig ist, hier Header einfügen:
    // curl_setopt($ch, CURLOPT_HTTPHEADER, ["Authorization: Bearer " . STRAPI_API_TOKEN]);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode === 200 && $response) {
        $data = json_decode($response, true);
        if (!empty($data['data'][0])) {
            $blog = $data['data'][0]; // Annahme: Strapi v4 Struktur
            // Falls 'attributes' genutzt wird: $blog = $data['data'][0]['attributes'];
        } else {
            http_response_code(404);
            $error = "Beitrag nicht gefunden.";
        }
    } else {
        // API-Fehler
        debugLog("Blog Single API Error: HTTP $httpCode");
        http_response_code(500);
        $error = "Fehler beim Laden des Beitrags.";
    }
}

// ARCHITEKTUR-HINWEIS: Diese Funktion gehört eigentlich in functions.php!
if (!function_exists('renderRichtext')) {
    function renderRichtext($blocks) {
        if (!is_array($blocks)) return '';
        $html = '';
        foreach ($blocks as $block) {
            // Einfache Implementierung für Paragraphs
            if (($block['type'] ?? '') === 'paragraph') {
                $html .= '<p>';
                if (!empty($block['children']) && is_array($block['children'])) {
                    foreach ($block['children'] as $child) {
                        // SICHERHEIT: Text IMMER escapen
                        $text = esc($child['text'] ?? '');
                        
                        // Formatierungen sicher anwenden
                        if (!empty($child['bold'])) { $text = '<strong>' . $text . '</strong>'; }
                        if (!empty($child['italic'])) { $text = '<em>' . $text . '</em>'; }
                        if (!empty($child['underline'])) { $text = '<u>' . $text . '</u>'; }
                        if (!empty($child['strikethrough'])) { $text = '<s>' . $text . '</s>'; }
                        if (!empty($child['code'])) { $text = '<code>' . $text . '</code>'; }

                        $html .= $text;
                    }
                }
                $html .= '</p>';
            }
            // HINWEIS: Andere Block-Typen (heading, list, image) fehlen hier noch!
            // Sie sollten implementiert werden, um vollständigen Content zu zeigen.
        }
        return $html; // Gibt sicheres HTML zurück
    }
}
?>

<div class="content content-plain top-margin">
    <div class="card">
  <?php if ($error): ?>
    <h1>Fehler</h1>
    <p><?= esc($error) ?></p>
  <?php else: 
      $attr = $blog['attributes'] ?? $blog; // Kompatibilität
      $title = esc($attr['title'] ?? '');
      // Autor-Daten sicher holen
      $authorData = $attr['author']['data']['attributes'] ?? $attr['author'] ?? [];
      $author = esc($authorData['username'] ?? 'Unbekannt');
      
      $date = 'Unbekanntes Datum';
      if (!empty($attr['publishedAt'])) {
          try { $date = (new DateTime($attr['publishedAt']))->format('d.m.Y'); } catch (Exception $e) {}
      }
      $content = $attr['content'] ?? [];
  ?>
    <h1><?= $title ?></h1>
    <p class="meta"><i class="fa-solid fa-user"></i> <?= $author ?> · <i class="fa-solid fa-calendar-days"></i> <?= $date ?></p>
    
    <div class="richtext"><?= renderRichtext($content) ?></div>

  <?php endif; ?>
    </div>
</div>

<?php include('../inc/footer.php'); ?>