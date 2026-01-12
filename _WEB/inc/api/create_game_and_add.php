<?php
session_start();
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
requireLogin();

header('Content-Type: application/json');

$input = json_decode(file_get_contents('php://input'), true);
$title = trim($input['title'] ?? '');
$minAge = (int)($input['min_age'] ?? 0);
$minPlayer = (int)($input['min_player'] ?? 0);
$maxPlayer = (int)($input['max_player'] ?? 0);
$releaseDateRaw = $input['release_date'] ?? null;
$releaseDate = (!empty(trim($releaseDateRaw))) ? trim($releaseDateRaw) : null;

if (empty($title)) {
    http_response_code(400);
    echo json_encode(['error' => 'Titel ist erforderlich']);
    exit;
}

// 1. Spiel erstellen
$gameResult = createBoardGame($title, $minAge, $minPlayer, $maxPlayer, $releaseDate);

if (!$gameResult['success']) {
    http_response_code(500);
    debugLog("Game Creation Error: " . ($gameResult['message'] ?? json_encode($gameResult)));
    echo json_encode(['error' => 'Spiel konnte nicht erstellt werden.']);
    exit;
}

$gameId = $gameResult['id'];

// 2. Spiel zur Sammlung hinzufügen - NEU: Custom Route
$url = STRAPI_API_BASE . '/user-boardgames/add-to-collection';
$data = ['boardgame' => $gameId];

$assignResult = makeStrapiApiCall('POST', $url, $data);

if (!$assignResult['success']) {
    http_response_code(500);
    debugLog("Assign Game Error (Game ID: $gameId): " . json_encode($assignResult));
    echo json_encode(['error' => 'Spiel erstellt, aber Zuweisung fehlgeschlagen.']);
    exit;
}

// 3. Erfolg
echo json_encode(['success' => true, 'id' => $gameId]);