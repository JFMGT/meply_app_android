<?php
/**
 * Archiviert Benachrichtigungen für den aktuellen Benutzer nach Typ.
 *
 * Diese Funktion holt alle *nicht-archivierten* Benachrichtigungen für den
 * eingeloggten Benutzer (basierend auf der Session), die einem bestimmten $type
 * und optional einer $elementDocumentId entsprechen.
 *
 * Jede gefundene Benachrichtigung wird anschließend einzeln per PUT-Request an Strapi
 * als "archiviert" und "gesehen" markiert.
 *
 * WICHTIG: Um ein N+1-Problem (Performance) und Timeouts zu vermeiden,
 * wird der initiale Abruf der Benachrichtigungen auf 50 Einträge begrenzt.
 *
 * @param string $type Erforderlich. Der Typ der zu archivierenden Benachrichtigungen (z.B. 'pm').
 * @param string|null $elementDocumentId Optional. Dient als zusätzlicher Filter für die elementDocumentId.
 * @return bool Gibt `true` zurück, wenn der Abruf und die Archivierungs-Schleife
 * gestartet wurden. Gibt `false` zurück, wenn keine Session-Daten
 * vorhanden sind oder der Abruf der Benachrichtigungen fehlschlägt.
 */
function archiveNotificationsByType(string $type, ?string $elementDocumentId = null): bool
{   
    $jwt = $_SESSION['jwt'] ?? '';
    $profileDocId = $_SESSION['profile']['documentId'] ?? '';

    if (!$jwt || !$profileDocId || !$type) {
        return false;
    }

    // Strapi-Query bauen
    $query = [
        "filters[receiver][documentId][\$eq]" => $profileDocId,
        "filters[type][\$eq]" => $type,
        "filters[archived][\$eq]" => "false",
        "pagination[limit]" => 50
    ];

    if ($elementDocumentId) {
        $query["filters[elementDocumentId][\$eq]"] = $elementDocumentId;
    }

    $url =  STRAPI_API_BASE . "/notifications?" . http_build_query($query);

    // Notifications abrufen
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $jwt"
    ]);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if (!$response || $httpCode >= 400) {
        return false;
    }

    $data = json_decode($response, true);
    $notifications = $data['data'] ?? [];

    if (empty($notifications)) {
        return false;
    }

    // Archivieren
    foreach ($notifications as $notif) {
        $notifId = $notif['documentId'];

        $update = curl_init( STRAPI_API_BASE . "/notifications/$notifId");
        curl_setopt($update, CURLOPT_CUSTOMREQUEST, "PUT");
        curl_setopt($update, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($update, CURLOPT_POSTFIELDS, json_encode([
            "data" => ["archived" => true, "seen" => true]
        ]));
        curl_setopt($update, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Content-Type: application/json"
]);
        curl_exec($update);
        curl_close($update);
    }

    return true;
}
