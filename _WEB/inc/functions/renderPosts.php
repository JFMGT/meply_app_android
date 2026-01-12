<?php
// ARCHITEKTUR-HINWEIS: Dependencies sollten global geladen werden.

require_once(__DIR__ . '/../config.php'); // Für STRAPI_IMAGE_BASE etc.
require_once(__DIR__ . '/../functions.php'); // Für STRAPI_IMAGE_BASE etc.

/**
 * Rendert eine Liste von Posts als HTML.
 * ACHTUNG: Enthält architektonische Mängel (HTML in PHP, interne Requires).
 * Sicherheitslücken (XSS) wurden behoben.
 *
 * @param array $posts Array von Post-Daten.
 * @param bool $interact Ob Interaktions-Elemente (Like/Reply) angezeigt werden sollen.
 */
function renderPosts(array $posts, $interact = true) {
    // ARCHITEKTUR-HINWEIS: Login-Prüfung gehört VOR den Aufruf dieser Funktion.
    // require_once('../functions/requireLogin.php');
    // requireLogin(); // Temporär auskommentiert, da es hier nicht hingehört.

    foreach ($posts as $index => $post) {
        // --- Daten sicher extrahieren und escapen ---
        $author = esc($post['author']['username'] ?? '???');
        $authorId = esc($post['author']['id'] ?? '1'); // Meist nur für Fallback Avatar, aber sicher ist sicher
        $slug = esc($post['author']['userslug'] ?? '');
        $text = nl2br(esc($post['content'] ?? '')); // esc() VOR nl2br
        $visibility = esc($post['visibility'] ?? 'unknown');
        $documentId = esc($post['documentId'] ?? ''); // SICHERHEITS-FIX
        $postDate = $post['createdAt'] ?? null; // Datum nicht escapen, wird formatiert
        $id = esc($post['id'] ?? ''); // SICHERHEITS-FIX
        $liked = $post['liked'] ?? false; // Boolean nicht escapen
        $likes = esc($post['likeCount'] ?? 0); // SICHERHEITS-FIX (als String behandeln)
        $replys = esc($post['replyCount'] ?? 0); // SICHERHEITS-FIX (als String behandeln)
        $authorDocumentId = $post['author']['documentId'] ?? null; // Roh für Vergleich

        // Avatar-URL Logik (war ok, aber $avatarUrl muss escaped werden)
        $avatarData = $post['author']['avatar'][0]['formats']['thumbnail']['url'] ?? null;
        $avatarUrl = '';
        if (!empty($avatarData)) {
            $avatarUrl = esc(STRAPI_IMAGE_BASE . $avatarData); // SICHERHEITS-FIX
        } else {
            $hash = crc32($authorId); // $authorId wurde oben escaped
            $avatarIndex = ($hash % 8) + 1;
            $avatarUrl = esc(WEBSITE_BASE . "/etc/images/avatar{$avatarIndex}.png"); // SICHERHEITS-FIX
        }

        // Sichtbarkeitsfilter (unverändert)
        // Hinweis: $visibility wurde oben escaped, der Vergleich funktioniert trotzdem
        if (!in_array($post['visibility'] ?? 'unknown', ['public', 'members', 'follower'])) continue;

        // --- Bilder-Slider sicher bauen ---
        $sliderHtml = '';
        if (!empty($post['image']) && is_array($post['image'])) {
            // SICHERHEITS-FIX: $sliderId escapen
            $sliderIdRaw = "slider_" . $index . "_" . ($post['id'] ?? ''); // Eindeutiger machen
            $sliderId = esc($sliderIdRaw);

            $sliderHtml .= "<div class='post-slider' id='{$sliderId}'>"; // Sicher
            $imageCount = count($post['image']);

            // Array mit allen Bildern für Lightbox-Galerie erstellen
            $lightboxImages = [];
            foreach ($post['image'] as $image) {
                $fullUrlRaw = STRAPI_IMAGE_BASE . ($image['url'] ?? '');
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
                $format = $image['formats']['medium']['url'] ?? $image['formats']['small']['url'] ?? $image['url'] ?? '';
                $imgUrlRaw = STRAPI_IMAGE_BASE . $format;

                // URL-Validierung
                $isValidImage = filter_var($imgUrlRaw, FILTER_VALIDATE_URL) &&
                                preg_match('/\.(jpg|jpeg|png|gif|webp|svg)$/i', parse_url($imgUrlRaw, PHP_URL_PATH));

                $imgUrl = $isValidImage ? esc($imgUrlRaw) : '';
                $altText = esc($image['alternativeText'] ?? 'Post-Bild');
                $activeClass = $i === 0 ? 'active' : '';

                // Lightbox-Array als JSON-String für onclick
                $lightboxJson = htmlspecialchars(json_encode($lightboxImages), ENT_QUOTES, 'UTF-8');
                $onclickAttr = $isValidImage ? "onclick='openLightbox(" . $lightboxJson . ", \"\", " . $i . ")'" : '';

                $sliderHtml .= "<div class='slide {$activeClass}'>";
                if ($isValidImage) {
                    // Lazy Loading für bessere Performance
                    $sliderHtml .= "<img src='{$imgUrl}' alt='{$altText}' loading='lazy' {$onclickAttr}>";
                }
                $sliderHtml .= "</div>";
            }

            if ($imageCount > 1) {
                // SICHERHEITS-FIX: $sliderId in onclick escapen
                 $prevClick = "onclick='prevSlide(\"{$sliderId}\")'";
                 $nextClick = "onclick='nextSlide(\"{$sliderId}\")'";
                 $sliderHtml .= "<button class='prev' {$prevClick}>&#10094;</button>";
                 $sliderHtml .= "<button class='next' {$nextClick}>&#10095;</button>";

                 // Image Counter (Text)
                 $sliderHtml .= "<div class='slider-counter'>1 / {$imageCount}</div>";

                 // Dot Indicators
                 $sliderHtml .= "<div class='slider-dots'>";
                 for ($d = 0; $d < $imageCount; $d++) {
                     $dotClass = $d === 0 ? 'dot active' : 'dot';
                     $sliderHtml .= "<span class='{$dotClass}'></span>";
                 }
                 $sliderHtml .= "</div>";
            }
            $sliderHtml .= "</div>";
        }

        // --- HTML-Ausgabe (Alle variablen Teile sind jetzt escaped) ---
        echo "<div class='post' data-post-id='{$id}'>"; // Sicher
        echo "<img class='post_profile' src='{$avatarUrl}'>"; // Sicher
        echo "<div class='meta'><strong>";
        if ($slug) echo "<a href='/user/" . $slug . "'>"; // $slug wurde oben escaped
        echo $author; // Wurde oben escaped
        if ($slug) echo "</a>";


        // Relative Datumsformatierung
        $dateOutput = formatRelativeTime($postDate);

        echo "</strong> | ";
        echo $dateOutput;
        echo " <i class='fa-regular fa-eye'></i> {$visibility} </div>"; // $visibility wurde oben escaped

        // Wrapper für collapsible content
        echo "<div class='post-body'>";
        echo "<div class='feed-content'>{$text}</div>"; // $text wurde oben escaped
        echo $sliderHtml; // Wurde oben sicher gebaut
        echo "</div>"; // Ende .post-body

        // Expand/Collapse Button (wird via JS ein/ausgeblendet)
        echo "<button class='post-expand-btn' style='display:none;'>";
        echo "<i class='fa-solid fa-chevron-down'></i> <span class='expand-text'>Post vollständig anzeigen</span>";
        echo "</button>";

        // Like-Button sicher bauen
        $heartIcon = $liked ? '<i class="fa-solid fa-heart"></i>' : '<i class="fa-regular fa-heart"></i>';
        $likeClass = $liked ? 'likeit liked' : 'likeit';
        echo "<div class='icon-bar'>";
        if ($interact) {
            echo "<a href='#' class='{$likeClass}' data-contenttype='post' data-documentid='{$documentId}' title='Beitrag liken'><span>{$likes}</span> {$heartIcon}</a>"; // Sicher
            echo '<button class="reply-button postModal" title="Antworten" data-documentid="'.$documentId.'"><i class="fa-solid fa-reply"></i></button>'; // Sicher
        }

        // Optionen sicher rendern (displayPostOptions escaped intern)
        $isOwner = ($authorDocumentId) === ($_SESSION['profile']['documentId'] ?? null);
        echo displayPostOptions($post['documentId'] ?? '', 'post', $isOwner, 'item-options-post'); // Übergabe des *rohen* $documentId ist ok, da die Funktion es escaped

        echo "</div>";

        // Reply-Links sicher bauen
        if (($post['replyCount'] ?? 0) == 1) {
            echo "<a data-documentid='{$documentId}' href='#' class='show-replys'>Antwort Anzeigen</a>"; // Sicher
        } elseif (($post['replyCount'] ?? 0) > 1) {
             // SICHERHEITS-FIX: $replys (Anzahl) escapen
            echo "<a data-documentid='{$documentId}' href='#' class='show-replys'>{$replys} Antworten Anzeigen</a>"; // Sicher
        }

        echo "</div>"; // Ende .post
    } // Ende foreach
} // Ende Funktion
?>