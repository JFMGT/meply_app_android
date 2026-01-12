<?php
// Annahme: config.php, der Helfer 'makeStrapiApiCall' und 'determinePostStatus'
// sind bereits durch die zentrale functions.php geladen.

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

/**
 * Holt eine einzelne Location oder alle Locations des aktuellen Benutzers.
 * Bestimmt den Status (draft/published) für die Liste aller Locations.
 *
 * @param string|null $documentId Die ID der spezifischen Location oder null für alle.
 * @param bool $draft Nur relevant, wenn $documentId gesetzt ist. True = Draft holen.
 * @return array|null Eine einzelne Location (Array), eine Liste von Locations (Array von Arrays) oder null bei Fehler.
 */
function getUserLocations(?string $documentId = null, bool $draft = false): ?array {
    
    // Check login status
    if (empty($_SESSION['jwt'])) {
        debugLog("getUserLocations Error: No JWT found in session.");
        return null; 
    }

    // --- CASE 1: Fetch a single location ---
    if ($documentId) {
        $status = $draft ? 'draft' : 'published';
        
        // SICHERHEITS-FIX: urlencode für den Pfad-Parameter
        $safeDocumentId = urlencode($documentId);
        
        $query = http_build_query([
            'publicationState' => ($status === 'draft' ? 'preview' : 'live'), // v4
            'populate' => 'author'
        ]);
        $apiUrl = STRAPI_API_BASE . "/locations/{$safeDocumentId}?" . $query;

        $result = makeStrapiApiCall('GET', $apiUrl);

        if (!$result['success']) {
            debugLog("getUserLocations (single) API Error for ID {$documentId}: " . ($result['response'] ?? 'Error'));
            return null;
        }
        
        return $result['response']['data'] ?? null;
    } 
    // --- CASE 2: Fetch all locations for the current user ---
    else {
        $profileDocumentId = $_SESSION['profile']['documentId'] ?? null;
        if (!$profileDocumentId) {
             debugLog("getUserLocations Error: profileDocumentId not found in session.");
             return null;
        }

        // OPTIMIERUNG: Direkte Abfrage des /locations Endpunkts
        $query = http_build_query([
            'filters[author][documentId][$eq]' => $profileDocumentId,
            'publicationState' => 'preview', // Holt Draft & Published
            'populate' => '*' // Oder spezifischer, falls möglich
        ]);
        $apiUrl = STRAPI_API_BASE . "/locations?" . $query;
        
        $result = makeStrapiApiCall('GET', $apiUrl);

        if (!$result['success']) {
            debugLog("getUserLocations (all) API Error for profile {$profileDocumentId}: " . ($result['response'] ?? 'Error'));
            return []; // Leeres Array bei Fehler für Listenansicht
        }

        $allLocations = $result['response']['data'] ?? [];
        
        // --- Verarbeite Locations, um den Status zu bestimmen ---
        $locationsByDocId = [];
        foreach ($allLocations as $location) {
            $docId = $location['documentId'] ?? null;
            if (!$docId) continue;
            
            $isPublished = isset($location['publishedAt']) && $location['publishedAt'] !== null;
            
            if (!isset($locationsByDocId[$docId])) {
                $locationsByDocId[$docId] = ['draft' => null, 'published' => null];
            }
            
            if ($isPublished) {
                if ($locationsByDocId[$docId]['published'] === null || $location['updatedAt'] > $locationsByDocId[$docId]['published']['updatedAt']) {
                    $locationsByDocId[$docId]['published'] = $location;
                }
            } else {
                 if ($locationsByDocId[$docId]['draft'] === null || $location['updatedAt'] > $locationsByDocId[$docId]['draft']['updatedAt']) {
                    $locationsByDocId[$docId]['draft'] = $location;
                }
            }
        }
        
        $finalLocations = [];
        foreach ($locationsByDocId as $docId => $versions) {
            $draftLocation = $versions['draft'];
            $publishedLocation = $versions['published'];

            // FIX: Handle all cases properly
            if ($draftLocation !== null) {
                // Draft exists - use it and determine status
                $draftLocation['status'] = determinePostStatus($draftLocation, $publishedLocation);
                $finalLocations[] = $draftLocation;
            } elseif ($publishedLocation !== null) {
                // Only published exists (no draft) - show published version
                $publishedLocation['status'] = 'published';
                $finalLocations[] = $publishedLocation;
            }
            // If both are null (shouldn't happen), skip this location
        }

        return $finalLocations;
    }
}