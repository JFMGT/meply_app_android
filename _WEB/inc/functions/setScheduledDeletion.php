<?php
// Annahme: config.php und der HELFER 'makeStrapiApiCall'
// sind bereits durch die zentrale functions.php geladen.

require_once __DIR__ . '/../config.php'; // Nötig für Konstanten und debugLog
require_once __DIR__ . '/../functions.php'; // Nötig für Konstanten und debugLog
/**
 * Plant oder bricht die Löschung eines Benutzerprofils ab.
 * - Wenn $deleteDate leer oder ungültig ist, wird die Löschung auf +14 Tage gesetzt.
 * - Wenn $deleteDate ein gültiges 'Y-m-d' Datum ist, wird die Löschung abgebrochen (auf null gesetzt).
 *
 * @param string $profileDocumentId Die documentId des zu aktualisierenden Profils.
 * @param string|null $deleteDate Optional. Ein Datum 'Y-m-d' zum Abbrechen oder leer/ungültig zum Planen.
 * @return array ['success' => bool, 'scheduledDeletionAt' => string|null]
 */
function setScheduledDeletion(string $profileDocumentId, ?string $deleteDate = null): array {

    // 1. Berechne Zieldatum
    $scheduledDeletionAt = null; // Standard: Abbrechen
    if (empty($deleteDate) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $deleteDate)) {
        // Planen
        $scheduledDeletionAt = date('Y-m-d', strtotime('+14 days'));
    }

    // 2. URL sicher bauen
    $url = STRAPI_API_BASE . '/profiles/' . urlencode($profileDocumentId);

    // 3. Payload
    $payload = [
        'data' => [
            'scheduledDeletionAt' => $scheduledDeletionAt
        ]
    ];

    // 4. Helfer verwenden (holt JWT aus Session)
    $result = makeStrapiApiCall('PUT', $url, $payload);

    // 5. Strukturiertes Ergebnis zurückgeben
    if ($result['success']) {
        // Die Session-Daten für 'deleteDate' müssen außerhalb aktualisiert werden!
        return ['success' => true, 'scheduledDeletionAt' => $scheduledDeletionAt];
    } else {
        debugLog("setScheduledDeletion API Error: Code " . $result['code'] . " | Response: " . json_encode($result['response']));
        return ['success' => false, 'scheduledDeletionAt' => null];
    }
}
?>