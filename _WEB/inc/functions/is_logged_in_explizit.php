<?php
// Annahme: config.php und der Helfer 'makeStrapiApiCall'
// sind bereits durch die zentrale functions.php geladen.
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
$profile = getProfileData();
/**
 * Überprüft explizit serverseitig, ob das JWT in der Session gültig ist.
 * Dies ist die SICHERE Methode zur Überprüfung des Login-Status.
 *
 * @return bool True, wenn das JWT gültig ist, sonst false.
 */
function is_logged_in_explizit(): bool {
    // 1. JWT prüfen (wird auch vom Helfer gemacht, aber schadet nicht)
    if (empty($_SESSION['jwt'])) {
        return false;
    }

    // 2. URL korrekt bauen (mit Konstante)
    $url = STRAPI_API_BASE . '/users/me';

    // 3. Helfer nutzen (löst DRY, Robustheit, Wartbarkeit)
    $result = makeStrapiApiCall('GET', $url);

    // 4. Erfolg prüfen
    // Der Helfer gibt 'success: true' nur bei HTTP 2xx zurück.
    // Wir prüfen zusätzlich, ob die Antwort eine 'id' enthält.
    return $result['success'] && isset($result['response']['id']);
}