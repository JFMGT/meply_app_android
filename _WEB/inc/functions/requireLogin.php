<?php
// Annahme: config.php, is_logged_in_explizit() sind global geladen.
// session_start() wurde bereits global aufgerufen.
require_once(__DIR__ . '/../config.php'); // Für STRAPI_IMAGE_BASE etc.
require_once(__DIR__ . '/../functions.php'); // Für STRAPI_IMAGE_BASE etc.

/**
 * Erzwingt einen gültigen Login-Status durch explizite Prüfung des JWT.
 * Verhindert die weitere Ausführung, wenn der Benutzer nicht gültig eingeloggt ist.
 *
 * @return bool True, wenn der Benutzer gültig eingeloggt ist.
 * Gibt false zurück und sendet einen 403-Header + JSON-Fehler,
 * wenn nicht eingeloggt. (Anpassbar, je nach gewünschtem Fehlerhandling)
 */
function requireLogin(): bool {
    // ARCHITEKTUR-FIX: session_start() hier entfernt.

    // SICHERHEITS-FIX: Explizite Prüfung verwenden!
    if (!is_logged_in_explizit()) {
        // FLEXIBLERES FEHLERHANDLING:
        // Option 1: Wie bisher (für reine API-Endpunkte)
        if (!headers_sent()) { // Nur senden, wenn noch keine Ausgabe erfolgte
            http_response_code(403);
            header('Content-Type: application/json'); // Korrekten Header setzen
        }
        echo json_encode(["error" => "Nicht autorisiert"]);
        exit;

        // Option 2: False zurückgeben (besser für gemischte Kontexte)
        // return false;

        // Option 3: Exception werfen (sauberste Lösung)
        // throw new Exception("Nicht autorisiert", 403);
    }

    // Wenn die explizite Prüfung erfolgreich war
    return true;
}