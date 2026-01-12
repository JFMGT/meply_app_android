<?php
session_start();
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
requireLogin();

header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
$entryId = $input['id'] ?? null;
$updateData = $input['update'] ?? null;

if (!$entryId || !$updateData) {
    http_response_code(400);
    echo json_encode(['error' => 'Entry ID and update data required']);
    exit;
}

// Custom Route nutzen
$url = STRAPI_API_BASE . '/user-boardgames/update/' . urlencode($entryId);

$result = makeStrapiApiCall('PUT', $url, $updateData);

if ($result['success']) {
    echo json_encode(['success' => true, 'message' => 'Eintrag aktualisiert']);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'Fehler beim Aktualisieren']);
}