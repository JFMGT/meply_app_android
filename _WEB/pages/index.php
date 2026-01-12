<?php 
include('../inc/header.php'); 

$slug = $_GET['page'] ?? null;
$page = null;
$error = null;

// Prüfen ob ein Slug übergeben wurde
if (!$slug) {
    http_response_code(404);
    $error = "Seite nicht gefunden.";
} else {
    $apiUrl = STRAPI_API_BASE . "/pages?filters[slug][\$eq]=" . urlencode($slug);
    $response = @file_get_contents($apiUrl);
    $data = json_decode($response, true);

    if (!empty($data['data'][0])) {
        $page = $data['data'][0];
    } else {
        http_response_code(404);
        $error = "Seite nicht gefunden.";
    }
}
?>

<div class="content content-plain top-margin">
  <div class="card">
  <?php if ($error): ?>
    <h1>Fehler beim Laden</h1>
    <p><?= htmlspecialchars($error) ?></p>
  <?php else: ?>
    <h1><?= htmlspecialchars($page['headline'] ?? 'Ohne Titel') ?></h1>

    <div class="richtext">
    <?php
    // ✅ Platzhalter-Dateien definieren (beliebig erweiterbar)
    $placeholders = [
        '[DATENSCHUTZ]' => __DIR__ . '/datenschutz_file.html',
        '[IMPRESSUM]'   => __DIR__ . '/impressum_file.html',
        // Beispiel: '[AGB]' => __DIR__ . '/agb.html',
    ];

    // Richtext aus Strapi ausgeben
    if (!empty($page['content'])) {
        foreach ($page['content'] as $block) {
            if ($block['type'] === 'paragraph') {
                $rawText = '';
                foreach ($block['children'] as $child) {
                    $text = htmlspecialchars($child['text']);
                    if (!empty($child['bold'])) $text = "<strong>$text</strong>";
                    if (!empty($child['italic'])) $text = "<em>$text</em>";
                    $rawText .= nl2br($text);
                }

                // 🔍 Prüfen, ob ein Platzhalter enthalten ist
                $foundPlaceholder = false;
                foreach ($placeholders as $placeholder => $filePath) {
                    if (strpos($rawText, $placeholder) !== false) {
                        $foundPlaceholder = true;
                        if (file_exists($filePath)) {
                            echo file_get_contents($filePath);
                        } else {
                            echo "<p><em>Datei für $placeholder nicht gefunden.</em></p>";
                        }
                        break;
                    }
                }

                // Falls kein Platzhalter enthalten war → normaler Textblock
                if (!$foundPlaceholder) {
                    echo "<p>$rawText</p>";
                }
            }
        }
    } else {
        echo "<p><em>Kein Inhalt verfügbar.</em></p>";
    }
    ?>
    </div>

  <?php endif; ?>
  </div>
</div>

<?php include('../inc/footer.php'); ?>
