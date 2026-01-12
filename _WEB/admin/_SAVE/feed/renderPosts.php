<?php
function renderPosts(array $posts, $interact = true) {
    foreach ($posts as $index => $post) {
        $author = htmlspecialchars($post['author']['username'] ?? '???');
        $slug = htmlspecialchars($post['author']['userslug'] ?? '');
        $text = nl2br(htmlspecialchars($post['content'] ?? ''));
        $visibility = $post['visibility'] ?? 'unknown';
        $documentId = $post['documentId'];
        $liked = $post['liked'];
        $likes = $post['likeCount'];
        $replys = $post['replyCount'];

        // Sichtbarkeitsfilter (anpassbar)
        if (!in_array($visibility, ['public', 'members'])) continue;

        // Bilder-Slider einfügen (wenn mehrere Bilder vorhanden sind)
        $sliderHtml = '';
        if (!empty($post['image']) && is_array($post['image'])) {
            $sliderId = "slider_$index";
            $sliderHtml .= "<div class='post-slider' id='$sliderId'>";
            foreach ($post['image'] as $i => $image) {
                $format = $image['formats']['medium']['url'] ?? $image['formats']['small']['url'] ?? $image['url'];
                $imgUrl = "https://media.meeplemates.de" . $format;
                $altText = htmlspecialchars($image['alternativeText'] ?? 'Post-Bild');
                $activeClass = $i === 0 ? 'active' : '';
                $fullUrl = "https://media.meeplemates.de" . ($image['url'] ?? '');
                $sliderHtml .= "<div class='slide $activeClass'>
                  <img src='$imgUrl' alt='$altText' onclick='openLightbox(\"$fullUrl\", \"$altText\")'>
                </div>";
            }
            if (count($post['image']) > 1) {
                $sliderHtml .= "
                    <button class='prev' onclick='prevSlide(\"$sliderId\")'>&#10094;</button>
                    <button class='next' onclick='nextSlide(\"$sliderId\")'>&#10095;</button>
                ";
            }
            $sliderHtml .= "</div>";
        }

        echo "<div class='post'>";
        echo "<div class='meta'><strong>";
        if ($slug) echo "<a href='/user/" . $slug . "'>";
        echo $author; 
        if ($slug) echo "</a>";
        echo "</strong> – Sichtbarkeit: $visibility</div>";

        echo "<div class='feed-content'>$text</div>";
        echo $sliderHtml;

        $heartIcon = $liked ? '❤️' : '🤍';
        $heart = $isLiked ? '<i class="fa-solid fa-heart"></i>' : '<i class="fa-regular fa-heart"></i>';
        $likeClass = $liked ? 'likeit liked' : 'likeit';
        echo "<div class='icon-bar'>";
        if($interact){
        echo "<a href='#' class='$likeClass' data-contenttype='post' data-documentid='" . $documentId . "' title='Beitrag liken'><span>$likes</span> $heartIcon</a>";
        echo "<button class='reply-button postModal' title='Antworten' data-documentid='" . $documentId . "'><i class='fa-solid fa-reply-all'></i></button>";
        }
        $isOwner = ($post['author']['documentId'] ?? null) === ($_SESSION['profile']['documentId'] ?? null);
        echo '<div class="post-options">';
        echo '<button class="options-toggle" title="Optionen">⋮</button>';
        echo '<div class="options-menu">';
        if ($isOwner) {
            echo '<button class="delete-post" data-documentid="' . $documentId . '">🗑 Beitrag löschen</button>';
        }
        echo '<button class="report-post" data-type="post" data-documentid="' . $documentId . '">⚠️ Beitrag melden</button>';
        echo '</div></div>';

        echo "</div>";
        if ($replys == 1) {
            echo "<a data-documentid='" . $documentId . "' href='' class='show-replys'>Antwort Anzeigen</a>";
        } elseif ($replys > 1) {
            echo "<a data-documentid='" . $documentId . "' href='' class='show-replys'>" . $replys . " Antworten Anzeigen</a>";
        }

        echo "</div>";
    }
}
?>
