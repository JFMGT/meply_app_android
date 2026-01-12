<?php
require_once __DIR__ . '/../config.php';

/**
 * Weist dem aktuellen Profil ein Spiel zu oder aktualisiert dessen Bewertung.
 * Nutzt die Custom Route 'add-to-collection', die serverseitig die Authentifizierung übernimmt.
 *
 * @param string $profileId Die documentId des Profils (nicht verwendet, nur für Kompatibilität)
 * @param int $gameId Die ID des Spiels
 * @param int|null $rating Die Bewertung (0-5)
 * @return array ['success' => bool, 'message' => string]
 */
function assignGameToUser($profileId, $gameId, $rating = null) {
    
    // Korrekte URL für die Custom Route
    $apiUrl = STRAPI_API_BASE . '/user-boardgames/add-to-collection';
    
    $payload = ['boardgame' => (int)$gameId];
    
    if ($rating !== null) {
        $payload['rating'] = (int)$rating;
    }
    
    $result = makeStrapiApiCall('POST', $apiUrl, $payload);
    
    if (!$result['success']) {
        return [
            'success' => false,
            'message' => $result['error'] ?? 'Fehler beim Hinzufügen zur Sammlung'
        ];
    }
    
    $response = $result['response'];
    
    // Prüfe ob bereits existierte
    if (isset($response['alreadyExists']) && $response['alreadyExists']) {
        // Falls Rating aktualisiert wurde
        if (isset($response['updated']) && $response['updated']) {
            return [
                'success' => true,
                'message' => 'Bewertung aktualisiert'
            ];
        }
        return [
            'success' => true,
            'message' => 'Spiel bereits in Sammlung'
        ];
    }
    
    // Neu hinzugefügt
    return [
        'success' => true,
        'message' => 'Spiel zur Sammlung hinzugefügt' . ($rating !== null ? ' und bewertet' : '')
    ];
}