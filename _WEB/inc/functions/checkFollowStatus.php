<?php
// require_once __DIR__ . '/../config.php'; <-- Sollte schon global geladen sein
// require_once __DIR__ . '/../functions/getProfileData.php'; <-- Wird bei Bedarf geladen
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
session_start();


// 1. KRITISCHER FEHLER ENTFERNT:
// Die Zeilen '$userData = getProfileData();' und der zugehörige 
// require_once wurden entfernt. Funktionsdateien dürfen keinen
// Code im globalen Geltungsbereich ausführen.

/**
 * Prüft den Follow-Status zwischen zwei Benutzern.
 * Nutzt den /followers/followedby/{userA}/{userB} Custom Endpoint.
 *
 * @param string $userA ID von Benutzer A oder 'current' für den eingeloggten Benutzer.
 * @param string $userB ID von Benutzer B oder 'all' für alle.
 * @param string|null $status Optional: Filtert nach Status ('pending', 'accepted').
 * @return mixed Bool ('isFollowing') wenn A->B geprüft wird, Array ('users') wenn A->'all' geprüft wird,
 * oder NULL bei einem Fehler (z.B. Session fehlt, API-Fehler).
 */
function checkFollowStatus($userA = 'current', $userB = 'all', $status = null) {
    
    // 2. KONSISTENTE FEHLER-RÜCKGABE (Session-Prüfung)
    // Wir verwenden hier 'user', wie im Original-Code
    if (!isset($_SESSION['jwt'], $_SESSION['user']['documentId'])) {
        debugLog("checkFollowStatus: Session-Daten (jwt oder user.documentId) fehlen." . $_SESSION['profile']['documentId'] . " " . $_SESSION['user']['documentId']);
        return null; 
    }

    // $token wird vom Helfer geholt, wir brauchen nur die ID
    $current = $_SESSION['user']['documentId'];

    // Platzhalter auflösen
    $userA = ($userA === 'current') ? $current : $userA;
    $userB = ($userB === 'current') ? $current : $userB;

    // API-URL bauen (Deine urlencode-Nutzung war bereits sicher!)
    $apiUrl = STRAPI_API_BASE . "/followers/followedby/" . urlencode($userA) . "/" . urlencode($userB);

    // Optionalen Status anhängen (http_build_query ist hier robuster)
    if (!empty($status)) {
        $apiUrl .= '?' . http_build_query(['status' => $status]);
    }

    // 3. ROBUSTER API-AUFRUF
    // Ersetzt die gesamte cURL-Logik und nutzt den Helfer
    $result = makeStrapiApiCall('GET', $apiUrl);

    // 4. ECHTE FEHLERBEHANDLUNG
    if (!$result['success']) {
        // Logge den API-Fehler (z.B. 401, 500)
        debugLog("API-Fehler bei checkFollowStatus: " . json_encode($result['response']));
        return null; // Konsistenter Fehler-Rückgabewert
    }
    
    $data = $result['response'];

    // Rückgabeformat je nach Anfrage
    if ($userA !== 'all' && $userB !== 'all') {
        return $data['isFollowing'] ?? false;
    }

    return $data['users'] ?? [];
}