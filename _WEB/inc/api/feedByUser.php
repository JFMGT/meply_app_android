<?php
session_start();
header('Content-Type: text/html; charset=utf-8');
require_once('../config.php');
// ARCHITEKTUR-HINWEIS: functions.php sollte geladen werden,
// um makeStrapiApiCall und debugLog zu verwenden.
require_once('../functions.php'); 
require_once('../functions/renderPosts.php'); // Benötigt renderPosts

// --- Login-Prüfung ---
$jwt = $_SESSION['jwt'] ?? null;
$authorDocId = $_GET['author'] ?? null; // ⚠️ Profile.documentId!
$limit = (int)($_GET['limit'] ?? 2);
$before = $_GET['before'] ?? null; // optionaler Cursor

// ARCHITEKTUR/SICHERHEITS-FIX: Explizite Prüfung nutzen
// Führe eine explizite JWT-Prüfung durch
if (!is_logged_in_explizit()) { 
    echo "<p class='error'>❌ Sitzung ungültig (Bitte neu einloggen)</p>";
    exit;
}
$jwt = $_SESSION['jwt']; // JWT nach expliziter Prüfung als gültig angenommen

if (!$authorDocId) {
    echo "<p class='error'>❌ Kein Autor angegeben</p>";
    exit;
}

// SICHERHEITS-FIX: Prüfung auf gültiges ID-Format
// if (!isDocumentId($authorDocId)) { echo "..."; exit; } // FÜGE HIER DEINE isDocumentId() PRÜFUNG EIN

// --- API-Abfrage ---
$query = [
    'author' => $authorDocId, // SICHER: Wird von http_build_query behandelt
    'limit'  => $limit,
];
if (!empty($before)) {
    $query['before'] = $before;
}

$url = STRAPI_API_BASE . '/feed?' . http_build_query($query);

// ARCHITEKTUR-HINWEIS: Hier sollte makeStrapiApiCall genutzt werden.
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Accept: application/json",
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 5); // Timeout hinzugefügt
$response = curl_exec($ch);

// --- NEU: Robustheits-Check ---
$code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

if ($error || $code !== 200 || !$response) {
    // Fehler loggen
    debugLog("Feed API Error: HTTP {$code}, cURL: {$error}");
    echo "<p class='error'>❌ Fehler beim Laden des Feeds</p>";
    exit;
}

// --- Verarbeitung ---
$data   = json_decode($response, true);
$feed   = $data['feed']   ?? [];
$cursor = $data['cursor'] ?? null;

// Nichts mehr?
if (empty($feed)) {
    exit;
}

// HTML ausgeben
ob_start();
// HINWEIS: Da renderPosts die Posts nur rendert, muss hier die Logik stimmen
renderPosts($feed, false); // renderPosts nutzt htmlspecialchars intern
$html = ob_get_clean();
echo $html;

// Cursor-Marker für JS (zum Nachladen)
if (is_array($cursor)) {
    // SICHERHEITS-FIX: Escaping war im Originalcode IN der Variablenzuweisung,
    // aber das echo MUSS die escapten Variablen verwenden.
    $oldest = htmlspecialchars($cursor['oldestCreatedAt'] ?? '', ENT_QUOTES, 'UTF-8');
    $hasMore = !empty($cursor['hasMore']) ? '1' : '0';

    // SICHERHEITS-FIX: Variablen werden hier in doppelten Anführungszeichen ausgegeben,
    // daher ist die explizite htmlspecialchars-Behandlung nötig und jetzt korrekt.
    echo '<div id="cursor" data-oldest="'.$oldest.'" data-hasmore="'.$hasMore.'" hidden></div>';
}
?>