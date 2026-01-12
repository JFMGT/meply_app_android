<?php
session_start();
header('Content-Type: application/json; charset=utf-8');
require_once('../config.php');
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();

// JWT aus der Session holen
$jwt = $_SESSION['jwt'] ?? null;

if (!$jwt) {
    http_response_code(401);
    echo json_encode([
        'status' => 'error',
        'error' => 'Nicht eingeloggt',
        'message' => 'Du musst angemeldet sein, um den Beitrag zu löschen.'
    ]);
    exit;
}

// documentId aus GET oder POST (je nach Frontend)
$documentId = $_GET['documentId'] ?? $_POST['documentId'] ?? null;

if (!$documentId) {
    http_response_code(400);
    echo json_encode([
        'status' => 'error',
        'error' => 'Fehlende Eingaben',
        'message' => 'documentId fehlt.'
    ]);
    exit;
}

// Anfrage an Strapi senden
$strapiUrl =  STRAPI_API_BASE . '/post-delete/' . urlencode($documentId);

$ch = curl_init($strapiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $jwt,
    'Accept: application/json',
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Erfolg?
if ($httpCode >= 200 && $httpCode < 300) {
    echo $response;
} else {
    http_response_code($httpCode);
    echo json_encode([
        'status' => 'error',
        'error' => "Strapi HTTP $httpCode",
        'message' => 'Fehler beim Löschen des Beitrags.'
    ]);
}