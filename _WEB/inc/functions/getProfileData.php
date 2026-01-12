<?php
// Annahme: config.php und der Helfer 'makeStrapiApiCall'
// werden bereits durch die zentrale functions.php geladen.
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Holt die vollständigen Profil- und Rechtedaten eines Benutzers
 * und speichert sie in der $_SESSION['profile'].
 * Nutzt einen Cache in der Session, um API-Aufrufe zu minimieren.
 *
 * @param bool $forceRefresh Wenn true, wird der Cache ignoriert
 * und die Daten werden neu von der API geladen.
 * @return string|false Die documentId des Profils bei Erfolg, sonst false.
 */
function getProfileData($forceRefresh = false) {
    
    // 1. ARCHITEKTUR-FIX: session_start() hier entfernt.
    // Diese muss an einer globalen Stelle (z.B. config.php) aufgerufen werden.

    // 2. Aus Cache (Logik war bereits gut)
    if (!empty($_SESSION['profile']['documentId']) AND !$forceRefresh) {
        return $_SESSION['profile']['documentId'];
    }

    // 3. Voraussetzungen prüfen (Logik war bereits gut)
    if (empty($_SESSION['user']['documentId']) || empty($_SESSION['jwt'])) {
        return false;
    }
    $userDocumentId = $_SESSION['user']['documentId'];
    // $jwt wird vom Helfer 'makeStrapiApiCall' automatisch aus der Session geholt.

    // 4. API-Query Profile (ROBUSTHEITS-FIX)
    $params = [
        'filters[userDocumentId][$eq]' => $userDocumentId,
        'pagination[limit]' => 1
    ];
    $url = STRAPI_API_BASE . '/profiles/me?populate=avatar';
$resultProfile = makeStrapiApiCall('GET', $url);

if (!$resultProfile['success'] || empty($resultProfile['response']['data'])) {
    debugLog("getProfileData: /profiles/me-Aufruf fehlgeschlagen...");
    return false;
}
$profileData = $resultProfile['response']['data'];

    // 6. Privacy-Log-Parsing (Logik war bereits gut)
    $lastAcceptedVersion = 0;
    if (!empty($profileData['privacy_acceptance_log'])) {
        $log = $profileData['privacy_acceptance_log'];
        if (is_array($log) && !empty($log)) {
            $lastAcceptedVersion = end($log)['version'] ?? null;
        }
    }

    $profileDocumentId = $profileData['documentId'];

    // 7. API-Query User Rights (ROBUSTHEITS-FIX)
    $paramsRights = [
        'filters[profile][documentId][$eq]' => $profileDocumentId,
        'pagination[limit]' => 1,
    ];
    $urlRights = STRAPI_API_BASE . '/user-rights?populate=profile&' . http_build_query($paramsRights);

    // Ersetzt den zweiten cURL-Block
    $resultRights = makeStrapiApiCall('GET', $urlRights);

    // 8. Echte Fehlerbehandlung für Rechte (BUG-FIX)
    $userCanEdit = false;
    $userCanPublish = false;

    if ($resultRights['success'] && !empty($resultRights['response']['data'][0])) {
        $rightsData = $resultRights['response']['data'][0];
        $userCanEdit = $rightsData['canEdit'] ?? false;
        $userCanPublish = $rightsData['canPublish'] ?? false;
    } else {
        // Loggt den Fehler, anstatt stillschweigend zu scheitern
        debugLog("getProfileData: /user-rights-Aufruf fehlgeschlagen für Profil: " . $profileDocumentId);
    }

    // 9. Session füllen (Logik war bereits gut)
    if ($profileData) {
        $_SESSION['profile'] = [
            'id' => $profileData['id'] ?? null,
            'documentId' => $profileData['documentId'] ?? null,
            'username' => $profileData['username'] ?? null,
            'firstName' => $profileData['firstName'] ?? null,
            'lastName' => $profileData['lastName'] ?? null,
            'birthDate' => $profileData['birthDate'] ?? null,
            'postalCode' => $profileData['postalCode'] ?? null,
            'city' => $profileData['city'] ?? null,
            'confirmed' => $profileData['confirmed'] ?? false,
            'createdAt' => $profileData['createdAt'] ?? null,
            'updatedAt' => $profileData['updatedAt'] ?? null,
            'searchRadius' => $profileData['searchRadius'] ?? null,
            'showInUserList' => $profileData['showInUserList'] ?? false,
            'allowProfileView' => $profileData['allowProfileView'] ?? false,
            'showBoardGameRatings' => $profileData['showBoardGameRatings'] ?? false,
            'usersCanFollow' => $profileData['followPrivacy'],
            'userCanEdit' => $userCanEdit, // Ist jetzt 'false', wenn Aufruf fehlschlägt
            'userCanPublish' => $userCanPublish, // Ist jetzt 'false', wenn Aufruf fehlschlägt
            'gender' => $profileData['gender'] ?? null,
            'boardgamegeekProfile' => $profileData['boardgamegeekProfile'] ?? null,
            'boardGameArenaUsername' => $profileData['boardGameArenaUsername'] ?? null,
            'avatar' => $profileData['avatar'] ?? null,
            'deleteDate' => $profileData['scheduledDeletionAt'] ?? null,
            'lastAcceptedPrivacyVersion' => $lastAcceptedVersion,
        ];  
        return $profileData['documentId'];
    }

    return false;
}