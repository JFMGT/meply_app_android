<?php
session_start();
require_once('../config.php');
require_once('../functions.php');

header('Content-Type: application/json; charset=utf-8');

// Parameter
$page = isset($_GET['page']) ? max(1, (int)$_GET['page']) : 1;
$pageSize = 50;
$titleFilter = $_GET['title'] ?? '';

$params = [
    'page' => $page,
    'pageSize' => $pageSize
];

if (!empty($titleFilter)) {
    $params['title'] = $titleFilter;
}

$queryString = http_build_query($params);
$url = STRAPI_API_BASE . '/user-boardgames/marketplace?' . $queryString;

debugLog("Marketplace API URL: " . $url);

// Nutze System Token für öffentlichen Zugriff
$result = makeStrapiApiCall('GET', $url, null, true);

if (!$result['success']) {
    http_response_code(500);
    debugLog("Marketplace API Error: HTTP " . $result['code'] . " - " . json_encode($result['response']));
    echo json_encode(['results' => [], 'count' => 0, 'error' => 'Fehler beim Laden der Angebote']);
    exit;
}

// Condition & Delivery Mapping
$conditionMap = [
    'New' => 'Neu',
    'Like New' => 'Wie neu',
    'Very Good' => 'Sehr gut',
    'Good' => 'Gut',
    'Used' => 'Gebraucht'
];

$deliveryMap = [
    'ShippingOnly' => 'Nur Versand',
    'PickupOnly' => 'Nur Abholung',
    'ShippingOrPickup' => 'Versand oder Abholung'
];

// Daten aufbereiten
$results = $result['response']['results'] ?? [];

foreach ($results as &$game) {
    foreach ($game['offers'] as &$offer) {
        $offer['condition'] = $conditionMap[$offer['condition']] ?? $offer['condition'];
        $offer['delivery'] = $deliveryMap[$offer['delivery']] ?? $offer['delivery'];
    }
}

echo json_encode([
    'results' => $results,
    'count' => $result['response']['count'] ?? 0,
    'pagination' => $result['response']['pagination'] ?? null
]);