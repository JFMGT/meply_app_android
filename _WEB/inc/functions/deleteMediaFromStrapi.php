<?php
// Annahme: config.php und der Helfer 'makeStrapiApiCall'
// werden bereits durch die zentrale functions.php geladen.

/**
 * Löscht eine Mediendatei aus Strapi.
 *
 * Nutzt die zentrale Helfer-Funktion 'makeStrapiApiCall' und
 * holt das JWT automatisch aus der Session.
 *
 * @param string|int $mediaId Die ID der zu löschenden Datei.
 * @return array Das standardisierte Antwort-Array vom Helfer:
 * ['success' => bool, 'code' => int, 'response' => mixed]
 */
function deleteMediaFromStrapi($mediaId)
{
    // 1. SICHERHEIT: urlencode für den Pfad-Parameter
    $url = STRAPI_API_BASE . "/upload/files/" . urlencode($mediaId);

    // 2. ROBUSTHEIT: Der Helfer kümmert sich um JWT, Fehler (Netzwerk/HTTP)
    $result = makeStrapiApiCall('DELETE', $url);

    return $result;
}