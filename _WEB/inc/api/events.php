<?php
header('Content-Type: application/json');
require_once('../config.php');
require_once('../functions.php'); // Lädt ALLE Helfer und Funktionen
session_start(); // Sollte zentral erfolgen

// CSRF-Token validieren (für POST-Requests)
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    requireCsrfToken();
}

// 1. Eingabe holen und validieren (wie vorher)
$zip = trim($_POST['zip'] ?? '') ?: ''; // Standard ist jetzt LEER
$zip = is_numeric($zip ?? '') && floatval($zip) > 0 ? floatval($zip) : 36037; // Radius (ggf. anpassen)
$radius = is_numeric($_POST['radius'] ?? '') && floatval($_POST['radius']) > 0 ? floatval($_POST['radius']) : 500; // Radius (ggf. anpassen)
$limit = is_numeric($_POST['limit'] ?? '') && intval($_POST['limit']) > 0 ? intval($_POST['limit']) : 25;

// Benötigte Session-Daten
$jwt = $_SESSION['jwt'] ?? null;
$profileDocumentId = $_SESSION['profile']['documentId'] ?? null;

// 2. Daten holen (mit der an die Original-Logik angepassten Funktion)
$data = get_nearby_events_data($zip, $radius, $limit, $jwt, $profileDocumentId);

// 3. Fehler prüfen und zurückgeben
if (isset($data['error'])) {
    echo json_encode(['error' => $data['error']]);
    exit;
}

// 4. Erfolg: HTML rendern (nutzt die unveränderte Render-Funktion)
$html = render_nearby_events_html($data);

// 5. JSON zurückgeben
echo json_encode(['html' => $html]);