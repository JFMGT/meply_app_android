

<?php 
/*
session_start(); 
header('Content-Type: text/html; charset=utf-8');
require_once('../config.php');
require_once('../functions/renderPosts.php');
$jwt = $_SESSION['jwt'] ?? null;

if (!$jwt) {
    echo "<p class='error'>❌ Nicht eingeloggt</p>";
    exit;
}

// Optional: Autor-ID aus URL übernehmen
$authorId = $_GET['author'] ?? null;
$limit = $_GET['limit'] ?? null;
// Basis-URL
$strapiUrl = STRAPI_API_BASE . '/feed';

// Wenn nach Autor gefiltert werden soll, Query-Parameter anhängen
if ($authorId) { 
    $strapiUrl = STRAPI_API_BASE . '/feed?author=' . urlencode($authorId).'&limit=0';
}                                                 

// Anfrage an Strapi senden
$ch = curl_init($strapiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Accept: application/json"
]);
$response = curl_exec($ch);
$data = json_decode($response, true);
curl_close($ch);

if (!isset($data['feed'])) {
    echo "<p class='error'>❌ Keine Daten</p>";
    exit;
}

// Beiträge ausgeben
renderPosts($data['feed']);
?>

<!-- Einfaches CSS für den Bildslider -->



