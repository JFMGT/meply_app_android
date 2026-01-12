<?php
session_start();
require_once('../functions.php');
requireLogin();

header('Content-Type: application/json');

// Parameter sammeln
$params = [
    'page' => isset($_GET['page']) ? max(1, (int)$_GET['page']) : 1,
    'pageSize' => 25
];

// Optionale Filter
if (isset($_GET['state'])) {
    $params['state'] = $_GET['state'];
}

if (isset($_GET['min_rating'])) {
    $params['minRating'] = (float)$_GET['min_rating'];
}

if (isset($_GET['title'])) {
    $params['title'] = trim($_GET['title']);
}

if (isset($_GET['for_sale'])) {
    $params['forSale'] = $_GET['for_sale'];
}

if (isset($_GET['sort_by'])) {
    $params['sortBy'] = $_GET['sort_by'];
}

// Custom Route
$queryString = http_build_query($params);
$url = STRAPI_API_BASE . '/user-boardgames/my-collection?' . $queryString;

$result = makeStrapiApiCall('GET', $url);

if ($result['success']) {
    echo json_encode($result['response']);
} else {
    http_response_code(500);
    echo json_encode([
        'results' => [],
        'pagination' => null,
        'error' => 'Fehler beim Laden der Spieleliste'
    ]);
}