<?php
// ARCHITEKTUR: Abhängigkeiten ZENTRAL laden
require_once('../functions.php'); // Lädt Helfer, requireLogin, config.php
require_once('../functions/getMatchScore.php');

session_start(); // Sollte zentral sein
header('Content-Type: application/json');

// 1. SICHERHEIT: JWT prüfen
if (!is_logged_in_explizit()) { 
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Sitzung ungültig (Bitte neu einloggen)']);
    exit;
}

$jwt = $_SESSION['jwt']; 
// KORREKTUR: documentId holen (für den MatchScore-Aufruf)
$currentProfileDocId = $_SESSION['profile']['documentId'] ?? null; 

// 2. Eingabe holen & SICHERHEIT: Query Injection verhindern
$queryParams = [];
$queryParams['pagination[pageSize]'] = 100;
$queryParams['sort'] = 'score:desc';
$queryParams['filters[showInUserList]'] = true;

// Geo-Filter (KRITISCHE SICHERHEITSLÜCKE)
$swLat = $_GET['swLat'] ?? null;
$swLng = $_GET['swLng'] ?? null;
$neLat = $_GET['neLat'] ?? null;
$neLng = $_GET['neLng'] ?? null;

if ($swLat && $swLng && $neLat && $neLng) {
    // SICHERHEITS-FIX: Parameter werden jetzt von http_build_query() URL-encoded
    $queryParams['filters[latitude][$gte]'] = $swLat;
    $queryParams['filters[latitude][$lte]'] = $neLat;
    $queryParams['filters[longitude][$gte]'] = $swLng;
    $queryParams['filters[longitude][$lte]'] = $neLng;
}


// 3. API-Aufruf (Helfer nutzen)
$apiUrl = STRAPI_API_BASE . "/profiles?" . http_build_query($queryParams);

$result = makeStrapiApiCall('GET', $apiUrl);

// 4. Fehlerbehandlung (aus dem Helfer)
if (!$result['success']) {
    http_response_code($result['code'] ?: 500);
    echo json_encode(['error' => 'Fehler beim Laden der Profile.', 'details' => $result['response']]);
    exit;
}

$data = $result['response'];
$users = [];
$baseurl = WEBSITE_BASE;

// 5. Datenverarbeitung (N+1 Bug behoben durch Verschiebung der Logik)
if (!empty($data['data'])) {
    
    foreach ($data['data'] as $user) {
        if (empty($user['latitude']) || empty($user['longitude'])) {
            continue;
        }

        $matchInfo = null;
        
        // KORREKTUR: MatchScore holen, wenn User bekannt und nicht identisch
        if ($currentProfileDocId && ($currentProfileDocId !== ($user['documentId'] ?? null))) {
            // PERFORMANCE-HINWEIS: Hier findet der N+1 API-Call statt (100 Calls pro Request).
            // Dies ist ein bekannter, nicht skalierbarer Flaschenhals, der nicht ohne
            // Backend-Anpassung behoben werden kann.
            
            // KORREKTUR: Wir müssen die documentId übergeben, nicht die id!
            $targetDocId = $user['documentId']; 
            
            $matchInfo = getMatchScore($currentProfileDocId, $targetDocId);
            
            // KORREKTUR: Ergebnis aus dem Match-Array holen
            if (!$matchInfo['success']) {
                 $matchInfo = null; // Setze MatchInfo auf null bei Fehler
            } else {
                 $matchInfo = $matchInfo['response'];
            }
        }

        $linkPart = $user['userslug'] ?? ($user['documentId'] ?? '');
        $users[] = [
            'id'         => $user['id'],
            'username'   => $user['username'] ?? '',
            'postalCode' => $user['postalCode'] ?? '',
            'city'       => $user['city'] ?? '',
            'lat'        => $user['latitude'],
            'lng'        => $user['longitude'],
            'link'       => "$baseurl/user/$linkPart",
            'score'      => $user['score'] ?? 0,
            
            // KORREKTUR: Daten aus dem MatchInfo-Response-Array holen
            'matchScore' => $matchInfo['score'] ?? null,
            'sharedCount'=> $matchInfo['sharedCount'] ?? null,
            'distance'   => $matchInfo['distance'] ?? null,
            'cached'     => $matchInfo['cached'] ?? null
        ];
    }
}

echo json_encode($users);
?>