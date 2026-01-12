<?php
session_start();
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
requireLogin();

header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
$entryId = $input['id'] ?? null;

if (!$entryId) {
    http_response_code(400);
    echo json_encode(['error' => 'Entry ID required']);
    exit;
}

// Custom Route nutzen
$url = STRAPI_API_BASE . '/user-boardgames/remove/' . urlencode($entryId);

$result = makeStrapiApiCall('DELETE', $url);

if ($result['success']) {
    echo json_encode(['success' => true, 'message' => 'Spiel entfernt']);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'Fehler beim Löschen']);
}