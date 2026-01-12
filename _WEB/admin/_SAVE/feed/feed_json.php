<?php
session_start(); 
header('Content-Type: application/json; charset=utf-8'); // JSON-Ausgabe
require_once('../config.php');

$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
    echo json_encode(['error' => 'Nicht eingeloggt']);
    exit;
}

$authorId = $_GET['author'] ?? null;
$strapiUrl = STRAPI_API_BASE . '/feed';

if ($authorId) {
    $strapiUrl .= '?author=' . urlencode($authorId) . '&limit=0';
}

$ch = curl_init($strapiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Accept: application/json"
]);
$response = curl_exec($ch);
$data = json_decode($response, true);
curl_close($ch);

echo json_encode($data); // ✨ einfach weitergeben