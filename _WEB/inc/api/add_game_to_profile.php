<?php
session_start();
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
requireLogin();

header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
$gameId = isset($input['boardgame']) ? (int)$input['boardgame'] : null;

if (!$gameId) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing game ID']);
    exit;
}

// ✅ NEU: Custom Route nutzen
$url = STRAPI_API_BASE . '/user-boardgames/add-to-collection';
$data = ['boardgame' => $gameId];

$result = makeStrapiApiCall('POST', $url, $data);

if ($result['success']) {
    $response = $result['response'];
    
    // Prüfe ob bereits existierte
    if (isset($response['alreadyExists']) && $response['alreadyExists']) {
        echo json_encode(['success' => true, 'message' => 'Spiel bereits in deiner Sammlung']);
    } else {
        echo json_encode(['success' => true, 'id' => $response['id'], 'message' => 'Spiel hinzugefügt']);
    }
} else {
    http_response_code(500);
    echo json_encode(['error' => 'Fehler beim Hinzufügen des Spiels']);
}