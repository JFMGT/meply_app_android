<?php
// Annahme: config.php und der HELFER 'makeAdminStrapiApiCall'
// sind bereits durch die zentrale functions.php geladen.
// require_once __DIR__ . '/../config.php'; // HIER ENTFERNT
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';
/**
 * Registriert einen neuen Benutzer über Strapi's /auth/local/register Endpunkt.
 * Verwendet den ADMIN-Token für den Aufruf (gemäß spezifischer Konfiguration).
 *
 * @param string $username Gewünschter Benutzername.
 * @param string $email E-Mail-Adresse.
 * @param string $password Passwort (mind. 6 Zeichen).
 * @return array Standardisiertes Antwort-Array vom Helfer:
 * ['success' => bool, 'code' => int, 'response' => mixed]
 * Bei Erfolg enthält 'response' das User-Objekt und JWT von Strapi.
 */
function registerUser($username, $email, $password): array {
    
    // 1. Eingabevalidierung (war gut)
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        return ['success' => false, 'code' => 400, 'response' => ['error' => 'Ungültige E-Mail-Adresse']];
    }
    if (strlen($password) < 6) {
        return ['success' => false, 'code' => 400, 'response' => ['error' => 'Passwort muss mindestens 6 Zeichen lang sein.']];
    }

    // 2. URL
    $url = STRAPI_API_BASE . "/auth/local/register";

    // 3. Payload als JSON (bevorzugt)
    $payload = [
        'username' => $username,
        'email' => $email,
        'password' => $password,
    ];

    // 4. Admin-Helfer verwenden (DRY, Robustheit)
    $result = makeAdminStrapiApiCall('POST', $url, $payload);

    // 5. Ergebnis zurückgeben
    // Der Helfer gibt bereits das standardisierte Format zurück.
    // Bei Erfolg (HTTP 200) enthält $result['response'] das User-Objekt und JWT.
    // Bei Fehler (z.B. E-Mail existiert schon -> HTTP 400) enthält es die Strapi-Fehlermeldung.
    if (!$result['success']) {
         debugLog("Registration failed for email '$email': HTTP " . $result['code'] . " - " . json_encode($result['response']));
    }

    return $result;
}