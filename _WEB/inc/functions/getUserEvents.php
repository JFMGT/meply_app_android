<?php
// Assumption: config.php, the helper 'makeStrapiApiCall', and 'determinePostStatus'
// are already loaded by the central functions.php.
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Holt ein einzelnes Event oder alle Events des aktuellen Benutzers.
 * Bestimmt den Status (draft/published) für die Liste aller Events.
 *
 * @param string|null $documentId Die ID des spezifischen Events oder null für alle.
 * @param bool $draft Nur relevant, wenn $documentId gesetzt ist. True = Draft holen.
 * @return array|null Ein einzelnes Event-Array, eine Liste von Event-Arrays oder null bei Fehler.
 */
function getUserEvents(?string $documentId = null, bool $draft = false): ?array {
    
    // Check login status via JWT existence (handled by helper, but good practice)
    if (empty($_SESSION['jwt'])) {
        debugLog("getUserEvents Error: No JWT found in session.");
        return null; 
    }

    // --- CASE 1: Fetch a single event ---
    if ($documentId) {
        $status = $draft ? 'draft' : 'published';
        
        // SECURITY FIX: urlencode the documentId for the path
        $safeDocumentId = urlencode($documentId);
        
        $query = http_build_query([
            'publicationState' => ($status === 'draft' ? 'preview' : 'live'), // v4 syntax
            // 'status' => $status, // v3 syntax (adjust if needed)
            'populate[0]' => 'author',
            'populate[1]' => 'location'
        ]);
        $apiUrl = STRAPI_API_BASE . "/events/{$safeDocumentId}?" . $query;

        $result = makeStrapiApiCall('GET', $apiUrl);

        if (!$result['success']) {
            debugLog("getUserEvents (single) API Error for ID {$documentId}: " . ($result['response'] ?? 'Error'));
            return null; // Return null on error
        }
        
        // Return the single event data array
        return $result['response']['data'] ?? null; 
    } 
    // --- CASE 2: Fetch all events for the current user ---
    else {
        $profileDocumentId = $_SESSION['profile']['documentId'] ?? null;
        if (!$profileDocumentId) {
             debugLog("getUserEvents Error: profileDocumentId not found in session.");
             return null;
        }

        // OPTIMIZATION: Query /events endpoint directly, filtering by author
        // Fetch BOTH draft and published versions using publicationState=preview
        $query = http_build_query([
            'filters[author][documentId][$eq]' => $profileDocumentId,
            'publicationState' => 'preview', // Gets draft and published
            'populate' => '*' // Keep populate broad here if needed downstream
        ]);
        $apiUrl = STRAPI_API_BASE . "/events?" . $query;
        
        $result = makeStrapiApiCall('GET', $apiUrl);

        if (!$result['success']) {
            debugLog("getUserEvents (all) API Error for profile {$profileDocumentId}: " . ($result['response'] ?? 'Error'));
            return []; // Return empty array on error for list view
        }

        $allEvents = $result['response']['data'] ?? [];
        
        // --- Process events to determine status (using determinePostStatus) ---
        // Strapi v4's 'preview' might return duplicates if draft is identical to published.
        // We need to group by documentId and determine the latest state.
        
        $eventsByDocId = [];
        foreach ($allEvents as $event) {
            $docId = $event['documentId'] ?? null;
            if (!$docId) continue;
            
            $isPublished = isset($event['publishedAt']) && $event['publishedAt'] !== null;
            
            if (!isset($eventsByDocId[$docId])) {
                $eventsByDocId[$docId] = ['draft' => null, 'published' => null];
            }
            
            if ($isPublished) {
                // Keep the latest published version if multiple exist (shouldn't happen often)
                if ($eventsByDocId[$docId]['published'] === null || $event['updatedAt'] > $eventsByDocId[$docId]['published']['updatedAt']) {
                    $eventsByDocId[$docId]['published'] = $event;
                }
            } else {
                 // Keep the latest draft version
                 if ($eventsByDocId[$docId]['draft'] === null || $event['updatedAt'] > $eventsByDocId[$docId]['draft']['updatedAt']) {
                    $eventsByDocId[$docId]['draft'] = $event;
                }
            }
        }
        
        $finalEvents = [];
        foreach ($eventsByDocId as $docId => $versions) {
            $draftEvent = $versions['draft'];
            $publishedEvent = $versions['published'];

            // If only published exists, treat it as the 'draft' for status check
            if ($draftEvent === null && $publishedEvent !== null) {
                 $draftEvent = $publishedEvent; 
            }
            
            if ($draftEvent !== null) {
                 // Use your existing function to compare dates
                 $draftEvent['status'] = determinePostStatus($draftEvent, $publishedEvent);
                 $finalEvents[] = $draftEvent;
            }
            // If only published exists and wasn't treated as draft, you might want to add it here
            // else if ($publishedEvent !== null) { ... add publishedEvent with status 'published' ... }
        }

        return $finalEvents;
    }
}