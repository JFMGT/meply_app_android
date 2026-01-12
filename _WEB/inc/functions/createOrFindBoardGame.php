<?php
/**
 * Erstellt ein neues Boardgame ODER findet ein existierendes
 * Prüft zuerst nach BGG ID (wenn vorhanden), dann nach Titel
 * 
 * @param string $title Spieltitel
 * @param int|null $bggId BoardGameGeek ID (optional)
 * @param string $importSessionKey Eindeutiger Import-Schlüssel
 * @return array ['success' => bool, 'id' => int, 'existed' => bool, 'message' => string]
 */
function createOrFindBoardGame($title, $bggId = null, $importSessionKey = null) {
    $apiBase = STRAPI_API_BASE . "/boardgames";
    
    // SCHRITT 1: Nach BGG ID suchen (wenn vorhanden)
    if ($bggId !== null && $bggId > 0) {
        $query = ['filters[bgg_id][$eq]' => $bggId];
        $url = $apiBase . '?' . http_build_query($query);
        
        $result = makeStrapiApiCall('GET', $url);
        
        if ($result['success'] && !empty($result['response']['data'])) {
            $existingGame = $result['response']['data'][0];
            return [
                'success' => true,
                'id' => $existingGame['id'],
                'existed' => true,
                'message' => "Gefunden über BGG ID: $bggId"
            ];
        }
    }
    
    // SCHRITT 2 & 3: Nutze die existierende createBoardGame Funktion
    // Die prüft selbst nach Titel und erstellt ggf. neu
    $result = createBoardGame(
        $title,
        null,  // minAge
        null,  // minPlayer
        null,  // maxPlayer
        null,  // releaseDate
        $importSessionKey,
        $bggId  // NEU: BGG ID übergeben
    );
    
    if ($result['success']) {
        // Prüfe ob es bereits existierte oder neu erstellt wurde
        $existed = (strpos($result['message'], 'existierte bereits') !== false);
        
        return [
            'success' => true,
            'id' => $result['id'],
            'existed' => $existed,
            'message' => $result['message']
        ];
    }
    
    return [
        'success' => false,
        'id' => null,
        'existed' => false,
        'message' => $result['message'] ?? 'Fehler beim Erstellen/Finden'
    ];
}