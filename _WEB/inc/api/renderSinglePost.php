<?php
// ARCHITEKTUR: Session ZENTRAL starten
session_start();
// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../functions.php'); // Lädt config, Helfer, renderPosts, is_logged_in_explizit etc.

// 1. SICHERHEIT: Explizite Login-Prüfung
// Stellt sicher, dass nur eingeloggte Benutzer Posts rendern können
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo "<p class='error'>❌ Nicht autorisiert</p>"; // HTML-Fehler
    exit;
}

// 2. EINGABE: Erwarte eine ID, nicht das ganze Objekt
$input = json_decode(file_get_contents('php://input'), true);
$documentId = $input['documentId'] ?? null; 

// Validierung (Beispiel)
if (empty($documentId) /* || !isDocumentId($documentId) */) { 
    http_response_code(400);
    echo "<p class='error'>❌ Ungültige Post-ID</p>";
    exit;
}

// 3. DATEN HOLEN (SICHER): Hole die Daten von der API
// Annahme: Es gibt einen Endpunkt, um einen einzelnen Post zu holen,
// der ALLE benötigten Daten (Author, Likes, etc.) enthält.
// Passe 'populate' ggf. an!
$url = STRAPI_API_BASE . "/posts/" . urlencode($documentId) . "?populate[author][populate][avatar]=*&populate[image]=*"; // Beispiel-URL
$result = makeStrapiApiCall('GET', $url); // Nutze den Helfer

// 4. FEHLERBEHANDLUNG
if (!$result['success'] || empty($result['response']['data'])) {
    http_response_code(404); // Not Found
    debugLog("Render Single Post Error: " . json_encode($result));
    echo "<p class='error'>❌ Beitrag nicht gefunden oder Fehler beim Laden.</p>";
    exit;
}

// 5. RENDERN: Übergebe die *vertrauenswürdigen* API-Daten an renderPosts
// renderPosts erwartet ein Array von Posts.
// Stelle sicher, dass die Struktur von $result['response']['data']
// dem entspricht, was renderPosts erwartet.
// (Strapi v4 gibt Daten oft unter 'attributes' zurück, passe das ggf. an)
$postData = $result['response']['data']; 

renderPosts([$postData], false); // $interact=false
?>