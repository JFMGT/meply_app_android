<?php
session_start();
require_once('../config.php');
require_once('../functions.php');

header('Content-Type: text/html; charset=utf-8');


if (empty($_SESSION['jwt'])) {
    http_response_code(401);
    echo "<p class='error'>❌ Nicht autorisiert. Session fehlt.</p>";
    exit;
}
$jwt = $_SESSION['jwt'];

// --- 2. Eingabe holen und validieren ---
$documentIdRaw = $_GET['documentId'] ?? null;
if (empty($documentIdRaw)) {
    echo "<p class='error'>❌ Ungültiger Aufruf (ID fehlt)</p>";
    exit;
}

// SICHERHEIT: Path Injection Fix
$documentIdEncoded = urlencode($documentIdRaw);
$strapiUrl = STRAPI_API_BASE . '/post-tree/' . $documentIdEncoded;

// --- 3. API-Aufruf ---
$ch = curl_init($strapiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Accept: application/json"
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
$response = curl_exec($ch);

$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

// --- 4. Fehlerbehandlung ---
if ($curlError || $httpCode !== 200 || !$response) {
    error_log("Post-Tree API Error: HTTP {$httpCode}, cURL: {$curlError}");
    
    // ✅ Spezielle Behandlung für 403
    if ($httpCode === 403) {
        echo "<p class='error'>❌ Zugriff verweigert. Möglicherweise ist dein Login abgelaufen.</p>";
    } else {
        echo "<p class='error'>❌ Fehler beim Laden des Beitragsbaums (Code: {$httpCode}).</p>";
    }
    exit;
}

$data = json_decode($response, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    error_log("Post-Tree JSON Decode Error: " . json_last_error_msg());
    echo "<p class='error'>❌ Fehler beim Verarbeiten der API-Antwort.</p>";
    exit;
}

// 🔁 Ausgabe rekursiv rendern
function renderPost($post, $level = 0) {
    // --- SICHERHEIT: ALLE Ausgaben escapen ---
    $author = esc($post['author']['username'] ?? '???');
    // $authorId (nur für crc32) muss nicht escaped werden, aber sicher holen:
    $authorIdRaw = $post['author']['userId'] ?? 'default'; 
    $slug = esc($post['author']['userslug'] ?? '');
    $text = nl2br(esc($post['content'] ?? '')); // esc() VOR nl2br
    $documentId = esc($post['documentId'] ?? '');
    $liked = $post['isLiked'] ?? false; // Boolean
    $heartIcon = $liked ? '<i class="fa-solid fa-heart"></i>' : '<i class="fa-regular fa-heart"></i>';
    $likeClass = esc($liked ? 'likeit liked' : 'likeit'); // SICHERHEITS-FIX
    $likes = esc($post['likes'] ?? 0); // SICHERHEITS-FIX
    $visibility = esc($post['visibility'] ?? 'unbekannt'); // SICHERHEITS-FIX

    // Avatar (Robustheit & Sicherheit)
    $avatar = $post['author']['avatar'][0]['formats']['thumbnail']['url'] ?? null;
    $avatarUrl = '';
    if (!empty($avatar)) {
        $avatarUrl = esc(STRAPI_IMAGE_BASE . $avatar); // SICHERHEITS-FIX
    } else {
        $hash = crc32($authorIdRaw);
        $index = ($hash % 8) + 1;
        $avatarUrl = esc(WEBSITE_BASE . "/etc/images/avatar{$index}.png"); // SICHERHEITS-FIX
    }

    // Bilder-Slider (Bug & XSS Fixes)
    $sliderHtml = '';
    if (!empty($post['image']) && is_array($post['image'])) {
        // BUG-FIX: $index existiert nicht. Nutze $documentId (bereits escaped)
        // ODER eine eindeutige ID
        $rawIdForSlider = $post['documentId'] ?? bin2hex(random_bytes(4)); // Eindeutige ID
        $sliderId = esc("slider_" . $rawIdForSlider); // SICHERHEITS-FIX

        $sliderHtml .= "<div class='post-slider' id='{$sliderId}'>";

        // Array mit allen Bildern für Lightbox-Galerie erstellen
        $lightboxImages = [];
        foreach ($post['image'] as $image) {
            $fullUrlRaw = $image['url'] ?? '';
            $fullUrlRaw = $fullUrlRaw ? "https://media.meeplemates.de" . $fullUrlRaw : '';
            $altTextRaw = $image['alternativeText'] ?? 'Post-Bild';

            if (filter_var($fullUrlRaw, FILTER_VALIDATE_URL) &&
                preg_match('/\.(jpg|jpeg|png|gif|webp|svg)$/i', parse_url($fullUrlRaw, PHP_URL_PATH))) {
                $lightboxImages[] = [
                    'url' => $fullUrlRaw,
                    'alt' => $altTextRaw
                ];
            }
        }

        // Bilder-Slider rendern
        foreach ($post['image'] as $i => $image) {
            $format = $image['formats']['medium']['url'] ?? $image['formats']['small']['url'] ?? $image['url'] ?? null;
            $imgUrlRaw = $format ? "https://media.meeplemates.de" . $format : '';
            $altTextRaw = $image['alternativeText'] ?? 'Post-Bild';
            $activeClass = $i === 0 ? 'active' : '';

            // SICHERHEITS-FIX (XSS): Alle Attribute escapen
            $imgUrl = esc($imgUrlRaw);
            $altText = esc($altTextRaw);

            // Lightbox-Array als JSON-String für onclick
            $lightboxJson = htmlspecialchars(json_encode($lightboxImages), ENT_QUOTES, 'UTF-8');
            $onClickJs = $imgUrl ? "onclick='openLightbox(" . $lightboxJson . ", \"\", " . $i . ")'" : '';

            $sliderHtml .= "<div class'slide {$activeClass}'>";
            if ($imgUrl) { // Nur rendern, wenn Bild-URL gültig
                $sliderHtml .= "<img src='{$imgUrl}' alt='{$altText}' {$onClickJs}>";
            }
            $sliderHtml .= "</div>";
        }
        if (count($post['image']) > 1) {
             // SICHERHEITS-FIX (XSS): onclick-Attribute escapen
             $onClickPrev = "onclick='prevSlide(\"" . esc($rawIdForSlider) . "\")'"; // JS erwartet die ROHE ID
             $onClickNext = "onclick='nextSlide(\"" . esc($rawIdForSlider) . "\")'";
            $sliderHtml .= "<button class'prev' {$onClickPrev}>&#10094;</button>";
            $sliderHtml .= "<button class'next' {$onClickNext}>&#10095;</button>";
        }
        $sliderHtml .= "</div>";
    }
    
    // HTML-Aufbau (mit gesäuberten Variablen)
    $html = "<div class='post' style='margin-left:" . ($level * 20) . "px'>";
    $html .= "<img class='post_profile' src='{$avatarUrl}'>"; // Sicher
    $html .= "<div class='meta'><strong>";
    $userLink = '#'; // Fallback
    if ($slug) {
        $userLink = esc(WEBSITE_BASE . "/user/" . $slug, ENT_QUOTES, 'UTF-8'); // URL auch escapen
    }
    if ($slug) $html .= "<a href='{$userLink}'>"; // Sicher
    $html .= $author; // Sicher
    if ($slug) $html .= "</a>";
    $html .= "</strong> – <i class='fa-regular fa-eye'></i> {$visibility}</div>"; // Sicher
    $html .= "<div class='feed-content'>{$text}</div>"; // Sicher
    $html .= $sliderHtml; // Sicher
    $html .= "<div class='icon-bar'>";
    $html .= "<a href='#' class='{$likeClass}' data-contenttype='post' data-documentid='{$documentId}' title='Beitrag liken'><span>{$likes}</span> {$heartIcon}</a>"; // Sicher
    $html .= '<button class="reply-button postModal" title="Antworten" data-documentid="'.$documentId.'"><i class="fa-solid fa-reply"></i></button>'; // Sicher

    // Autor-Check
    $isOwner = ($post['author']['userDocumentId'] ?? null) === ($_SESSION['user']['documentId'] ?? null); // Verwendet user.documentId wie im Original
    
    // displayPostOptions (verwendet $documentId, der bereits escaped ist)
    // Wenn displayPostOptions die rohe ID erwartet, $post['documentId'] übergeben.
    $html .= displayPostOptions($post['documentId'] ?? '', 'post', $isOwner, 'item-options-post'); 
    
    $html .= "</div>"; // icon-bar
    $html .= "</div>"; // post

    // Rekursion (Logik war ok)
    if (!empty($post['children'])) {
        foreach ($post['children'] as $child) {
            $html .= renderPost($child, $level + 1);
        }
    }

    return $html;
}

// --- 5. Finale Ausgabe (jetzt sicher vor Fatal Error) ---
// $data ist die decodierte JSON-Antwort von der API
if (is_array($data)) {
    echo renderPost($data);
} else {
    // Fallback, falls $data (entgegen der Annahme) kein Array ist
    echo "<p class='error'>❌ Ungültige API-Antwortsstruktur.</p>";
}