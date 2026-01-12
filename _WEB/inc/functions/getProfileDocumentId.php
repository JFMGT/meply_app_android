<?php
// Assumption: config.php and the helper 'makeStrapiApiCall'
// are already loaded by the central functions.php.
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Gets the documentId of the current user's profile.
 * Caches the result in $_SESSION['profileDocumentId'].
 *
 * @return string|null The documentId on success, null otherwise.
 */
function getProfileDocumentId(): ?string {
    
    // 1. ARCHITECTURE FIX: session_start() removed.
    // Must be called globally elsewhere (e.g., config.php).

    // 2. Check Cache (logic was good)
    if (!empty($_SESSION['profileDocumentId'])) {
        return $_SESSION['profileDocumentId'];
    }

    // 3. Check Prerequisites (logic was good)
    $userDocumentId = $_SESSION['user']['documentId'] ?? null;
    // $jwt is fetched automatically by the helper from $_SESSION['jwt']
    if (!$userDocumentId || empty($_SESSION['jwt'])) {
        return null; // Not logged in or user session incomplete
    }

    // 4. Prepare API Request (SECURITY FIX - Query Injection already prevented by urlencode)
    $query = http_build_query([
        'filters[userDocumentId][$eq]' => $userDocumentId,
        'pagination[limit]' => 1,
        'fields[0]' => 'documentId' // Optimization: Only fetch the needed field
    ]);
    $apiUrl =  STRAPI_API_BASE . '/profiles?' . $query;

    // 5. Call API via Helper (ROBUSTNESS FIX & DRY FIX)
    $result = makeStrapiApiCall('GET', $apiUrl);

    // 6. Check Helper Response
    if ($result['success'] && !empty($result['response']['data'][0]['documentId'])) {
        $profileDocId = $result['response']['data'][0]['documentId'];

        // Store in Session Cache
        $_SESSION['profileDocumentId'] = $profileDocId;

        return $profileDocId;
    }
    
    // Log the error if the API call failed
    if (!$result['success']) {
         debugLog("getProfileDocumentId API Error: " . ($result['response'] ?? 'Unknown Error'));
    }

    // API error or profile not found
    return null; 
}