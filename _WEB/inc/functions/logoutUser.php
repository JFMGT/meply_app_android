<?php
/**
 * Loggt den Benutzer aus, indem die Session zerstört wird.
 * Leitet optional auf eine SICHERE lokale URL weiter.
 *
 * @param string|null $redirectTo Die relative URL, zu der weitergeleitet werden soll (z.B. '/login').
 * Muss ein lokaler Pfad sein, um Open Redirect zu verhindern.
 */
function logoutUser($redirectTo = null) {
    // ARCHITEKTUR-FIX: session_start() hier entfernt.
    // Muss global aufgerufen werden.

    // Session sicher beenden
    session_unset();
    session_destroy();

    // SICHERHEITS-FIX (Open Redirect): Nur lokale Weiterleitungen erlauben
    if ($redirectTo) {
        // Entferne führende Slashes und prüfe, ob es nach einer externen URL aussieht
        $safeRedirectTo = ltrim($redirectTo, '/');
        if (strpos($safeRedirectTo, '//') === false && strpos($safeRedirectTo, ':') === false) {
            // Baue die URL sicher zusammen (relativer Pfad)
            $location = '/' . $safeRedirectTo;

            // Robustheit: Sicherstellen, dass keine Ausgabe vorher stattgefunden hat
            if (!headers_sent()) {
                header("Location: " . $location);
                exit;
            } else {
                // Fallback oder Log, wenn Header schon gesendet wurden
                debugLog("Logout Redirect failed: Headers already sent.");
                // Optional: JavaScript-Redirect als Fallback
                // echo "<script>window.location.href='" . htmlspecialchars($location, ENT_QUOTES, 'UTF-8') . "';</script>";
                exit; // exit ist hier wichtig
            }
        } else {
            // Logge den Versuch einer externen Weiterleitung
            debugLog("Logout Security Warning: Invalid redirect attempt to: " . $redirectTo);
            // KEINE Weiterleitung bei ungültigem Ziel
        }
    }
    // Kein $redirectTo oder ungültiges Ziel: Skript endet hier normal.
}