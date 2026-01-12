<?php
// ARCHITEKTUR-HINWEIS: Dependencies sollten global geladen werden.

require_once(__DIR__ . '/../config.php'); // Für STRAPI_API_BASE, STRAPI_API_TOKEN, debugLog
require_once(__DIR__ . '/../functions.php');
/**
 * Rendert den HTML-Link für den Like-Button.
 * Enthält Fallbacks, um Like-Zahl und Status via API zu holen, falls nicht übergeben.
 *
 * @param string $documentId Die ID des gelikten Elements.
 * @param string $contentType Der Typ ('post', 'event', etc.).
 * @param int|bool|null $likes Die Anzahl der Likes (int), false (um API zu triggern) oder null.
 * @param bool|null $liked Ob der User geliked hat (bool) oder null (um API zu triggern).
 * @return string Das gerenderte HTML.
 */
function renderLikeLink(string $documentId, string $contentType, $likes = false, $liked = null): string {
    
    // ARCHITEKTUR-HINWEIS: session_start() sollte global erfolgen.
    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }

    $jwt = $_SESSION['jwt'] ?? null;
    $isLiked = false;
    $likeCount = 0;

    // --- Like-Zahl holen (Fallback) ---
    // PERFORMANCE-HINWEIS: Dieser Block verursacht N+1 Queries, wenn $likes nicht übergeben wird.
    if ($likes === false) {
        // HINWEIS: Annahme, dass Like-Zahlen via Admin-Token abrufbar sind.
        // `@file_get_contents` ersetzt durch robusten curl-Aufruf.
        $queryCount = http_build_query([
            'filters[documentId][$eq]' => $documentId,
            'fields[0]' => 'likes' // Nur das benötigte Feld holen
        ]);
        $apiUrlCount = STRAPI_API_BASE . "/{$contentType}s?" . $queryCount;

        $chCount = curl_init($apiUrlCount);
        curl_setopt($chCount, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($chCount, CURLOPT_HTTPHEADER, [
            "Authorization: Bearer " . STRAPI_API_TOKEN, // Admin-Token
            "Accept: application/json"
        ]);
        curl_setopt($chCount, CURLOPT_TIMEOUT, 3);
        $responseCount = curl_exec($chCount);
        $httpCodeCount = curl_getinfo($chCount, CURLINFO_HTTP_CODE);
        $errCount = curl_error($chCount);
        curl_close($chCount);

        if (!$errCount && $httpCodeCount === 200 && $responseCount) {
            $dataCount = json_decode($responseCount, true);
            // Annahme: Strapi v4 Struktur mit 'attributes'
            if (!empty($dataCount['data'][0]['attributes']['likes'])) {
                $likeCount = (int)$dataCount['data'][0]['attributes']['likes'];
            }
            // Annahme: Strapi v3 Struktur ohne 'attributes'
            elseif (!empty($dataCount['data'][0]['likes'])) {
                 $likeCount = (int)$dataCount['data'][0]['likes'];
            }
        } else {
             debugLog("renderLikeLink (Count Fallback) Error: curl: $errCount, HTTP: $httpCodeCount");
             // Like-Zahl bleibt 0 bei Fehler
        }
    } else {
        $likeCount = (int) $likes; // Wert wurde übergeben
    }

    // --- Like-Status bestimmen ---
    if ($liked === true) {
        $isLiked = true;
    } elseif ($liked === false) {
        $isLiked = false;
    // PERFORMANCE-HINWEIS: Dieser Block verursacht N+1 Queries, wenn $liked=null ist.
    } elseif ($jwt !== null) { // Nur prüfen, wenn User eingeloggt UND Status unbekannt
        // ARCHITEKTUR-HINWEIS: Besser wäre die Nutzung von makeStrapiApiCall().
        $likeCheckUrl = STRAPI_API_BASE . "/likes/has-liked/" . urlencode($documentId);

        $chLike = curl_init($likeCheckUrl);
        curl_setopt($chLike, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($chLike, CURLOPT_HTTPHEADER, [
            "Authorization: Bearer $jwt", // User-Token
            "Accept: application/json"
        ]);
        curl_setopt($chLike, CURLOPT_TIMEOUT, 3);
        $likeResponse = curl_exec($chLike);
        $httpCodeLike = curl_getinfo($chLike, CURLINFO_HTTP_CODE);
        $errLike = curl_error($chLike);
        curl_close($chLike);

        if (!$errLike && $httpCodeLike === 200 && $likeResponse) {
            $likeData = json_decode($likeResponse, true);
            if (!empty($likeData['liked'])) {
                $isLiked = true;
            }
            // Wenn 'liked' nicht true ist, bleibt $isLiked false (Standard)
        } else {
            debugLog("renderLikeLink (Status Fallback) Error: curl: $errLike, HTTP: $httpCodeLike");
            // Status bleibt 'false' bei Fehler
        }
    }
    // Wenn $jwt null ist und $liked null ist, bleibt $isLiked false (Standard)

    // --- HTML rendern (Sicherheit: Alle Ausgaben sind escaped) ---
    $heart = $isLiked ? '<i class="fa-solid fa-heart"></i>' : '<i class="fa-regular fa-heart"></i>';
    $classes = 'likeit' . ($isLiked ? ' liked' : '');

    return '<a href="#" class="' . esc($classes) . '" 
               data-contenttype="' . esc($contentType) . '" 
               data-documentid="' . esc($documentId) . '" 
               title="Beitrag liken">
               <span>' . esc((string)$likeCount) . '</span> ' . $heart . '</a>';
}
?>