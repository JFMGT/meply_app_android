<?php
session_start();
require_once('../config.php');

// 🔒 Authentifizierung prüfen
if (!isset($_SESSION['jwt'])) {
    http_response_code(401);
    echo json_encode(['error' => 'Nicht autorisiert']);
    exit;
}

$jwt = $_SESSION['jwt'];
$profileId = $_GET['profile_id'] ?? null;

if (!$profileId) {
    http_response_code(400);
    echo json_encode(['error' => 'Fehlende Profil-ID']);
    exit;
}

// 🔎 Strapi-Endpoint vorbereiten
$url = STRAPI_API_BASE . '/user-boardgames';
$params = [
    'filters[profile][id][$eq]' => $profileId,
    'filters[forSale][$eq]' => 'true',
    'populate' => 'boardgame',
    'pagination[pageSize]' => 50, // Sicherheitslimit
    'sort' => 'boardgame.title:asc'
];

$query = http_build_query($params);
$url .= '?' . $query;

// 🔄 Anfrage an Strapi
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt"
]);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200) {
    http_response_code($httpCode);
    echo json_encode(['error' => 'Strapi-Fehler oder keine Daten']);
    exit;
}

$data = json_decode($response, true);
$sales = [];

if (!empty($data['data'])) {
    foreach ($data['data'] as $entry) {
        $game = $entry['boardgame'] ?? [];
        $sales[] = [
            'title' => $game['title'] ?? 'Unbekannt',
            'price' => $entry['price'] ?? null,
            'condition' => $entry['condition'] ?? null,
            'deliveryOption' => $entry['deliveryOption'] ?? null,
            'tradePossible' => $entry['tradePossible'] ?? false,
        ];
    }
}

// 🧾 JSON-Ausgabe
header('Content-Type: application/json');
echo json_encode(['results' => $sales]);
