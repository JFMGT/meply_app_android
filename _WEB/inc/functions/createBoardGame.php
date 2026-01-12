<?php
require_once __DIR__ . '/../config.php';
require_once('normalizeDate.php');

/**
 * Erstellt ein neues Spiel oder aktualisiert ein bestehendes (UPSERT).
 *
 * @param string $title Spieltitel
 * @param int|null $minAge Mindestalter
 * @param int|null $minPlayer Min. Spieleranzahl
 * @param int|null $maxPlayer Max. Spieleranzahl
 * @param string|null $releaseDate Erscheinungsdatum
 * @param string|false $importSessionKey Import-Session-Key
 * @param int|null $bggId BoardGameGeek ID (NEU)
 * @return array ['success' => bool, 'id' => int|null, 'message' => string]
 */
function createBoardGame($title, $minAge, $minPlayer, $maxPlayer, $releaseDate, $importSessionKey = false, $bggId = null) {
    $apiBase = STRAPI_API_BASE . '/boardgames';

    $normalizedDate = normalizeDate($releaseDate);

    // Schritt 1: Prüfen, ob Spiel mit Titel bereits existiert
    $query = ['filters[title][$eq]' => $title];
    $url = $apiBase . '?' . http_build_query($query);
    
    $result = makeStrapiApiCall('GET', $url);

    if (!$result['success']) {
        return ['success' => false, 'id' => null, 'message' => 'API-Fehler bei Spiel-Prüfung.'];
    }

    $existingData = $result['response']['data'] ?? [];

    // Schritt 2: Spiel existiert bereits -> Felder auffüllen
    if (!empty($existingData) && isset($existingData[0]['documentId'])) {
        $existing = $existingData[0];
        $gameId = $existing['id'];
        $gameDocumentId = $existing['documentId'];

        $updateFields = [];
        if (empty($existing['min_age']) && $minAge !== null) {
            $updateFields['min_age'] = (int)$minAge;
        }
        if (empty($existing['min_player']) && $minPlayer !== null) {
            $updateFields['min_player'] = (int)$minPlayer;
        }
        if (empty($existing['max_player']) && $maxPlayer !== null) {
            $updateFields['max_player'] = (int)$maxPlayer;
        }
        if (empty($existing['release_date']) && !empty($normalizedDate)) {
            $updateFields['release_date'] = $normalizedDate;
        }
        // NEU: BGG ID ergänzen falls leer
        if (empty($existing['bgg_id']) && $bggId !== null) {
            $updateFields['bgg_id'] = (string)$bggId;
        }

        if (!empty($updateFields)) {
            $updatePayload = ['data' => $updateFields];
            $updateResult = makeStrapiApiCall('PUT', $apiBase . '/' . $gameDocumentId, $updatePayload, true);
            
            if (!$updateResult['success']) {
                 return ['success' => false, 'id' => $gameId, 'message' => 'Spiel existiert, aber Update fehlgeschlagen.'];
            }
        }
        
        return ['success' => true, 'id' => $gameId, 'message' => 'Spiel existierte bereits, Daten ggf. ergänzt.'];
    }

    // Schritt 3: Spiel existiert NICHT → neu anlegen
    $newGamePayload = [
        'data' => [
            'title' => $title,
            'min_age' => $minAge,
            'min_player' => $minPlayer,
            'max_player' => $maxPlayer,
            'release_date' => $normalizedDate
        ]
    ];
    
    // NEU: BGG ID hinzufügen
    if ($bggId !== null) {
        $newGamePayload['data']['bgg_id'] = (string)$bggId;
    }
    
    if ($importSessionKey) {
        $newGamePayload['data']['importSessionKey'] = $importSessionKey;
        $newGamePayload['data']['isPublicDataset'] = false;
    }

    $createResult = makeStrapiApiCall('POST', $apiBase, $newGamePayload, true);

    if (!$createResult['success']) {
        return ['success' => false, 'id' => null, 'message' => 'Spiel konnte nicht erstellt werden.'];
    }

    $newGameId = $createResult['response']['data']['id'] ?? null;
    
    if ($newGameId === null) {
         return ['success' => false, 'id' => null, 'message' => 'Spiel erstellt, aber ID konnte nicht gelesen werden.'];
    }

    return ['success' => true, 'id' => $newGameId, 'message' => 'Spiel erfolgreich neu erstellt.'];
}