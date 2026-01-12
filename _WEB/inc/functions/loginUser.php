<?php
// Annahme: config.php und der NEUE Helfer 'makeAdminStrapiApiCall'
// sind bereits durch die zentrale functions.php geladen.
// require_once("setScheduledDeletion.php"); // AUS Funktion entfernt!

/**
 * Versucht, einen Benutzer über Strapi's /auth/local Endpunkt einzuloggen.
 * Verwendet den ADMIN-Token für den Aufruf (gemäß spezifischer Konfiguration).
 *
 * @param string $identifier Benutzername oder E-Mail.
 * @param string $password Passwort.
 * @return array|false Das Ergebnis-Array von Strapi (mit jwt, user) bei Erfolg, sonst false.
 */
function loginUser($identifier, $password) {
    // URL bleibt gleich
    $url = STRAPI_API_BASE . "/auth/local";

    // JSON-Payload (bevorzugt von Strapi)
    $payload = [
        'identifier' => $identifier,
        'password' => $password
    ];

    // NEU: Den korrekten Admin-Helfer verwenden
    $result = makeAdminStrapiApiCall('POST', $url, $payload);

    // Prüfe, ob der Aufruf erfolgreich war UND ein JWT zurückkam
    if ($result['success'] && isset($result['response']['jwt'])) {
        // Gib die erfolgreiche Antwort zurück (enthält jwt und user)
        return $result['response'];
    }

    // Bei Fehler (falsches PW, Netzwerkfehler etc.) false zurückgeben
    if (!$result['success']) {
        // Optional: Detaillierteres Logging für Server-Admins
        debugLog("Login failed for identifier '$identifier': HTTP " . $result['code'] . " - " . json_encode($result['response']));
    }
    
    return false; // Login fehlgeschlagen oder API-Fehler
}