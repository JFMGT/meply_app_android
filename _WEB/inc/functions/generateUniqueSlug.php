<?php

function generateUniqueSlug($title) {
    // 1. NEU: Robuste Transliteration (wandelt é in e, ñ in n, etc.)
    $slug = iconv('UTF-8', 'ASCII//TRANSLIT', $title);
    
    // 2. Kleinbuchstaben
    $slug = strtolower($slug);

    // 3. Whitelist (unverändert, ist perfekt)
    $slug = preg_replace('/[^a-z0-9_-]/', '-', $slug);

    // 4. Aufräumen (unverändert)
    $slug = preg_replace('/-+/', '-', $slug);
    $slug = trim($slug, '-');
    
    // 5. Sicherer Zufalls-Anhang (unverändert)
    $slug .= '-' . generateRandomString(5); 

    return $slug;
}