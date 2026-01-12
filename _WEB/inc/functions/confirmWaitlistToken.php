<?php
/**
 * Bestätigt ein Wartelisten-Token über einen Admin-API-Endpunkt.
 *
 * HINWEIS (Architektur): Diese Funktion verwendet den STRAPI_API_TOKEN.
 * Sie könnte von einer zentralen Admin-API-Helferfunktion
 * profitieren (ähnlich wie 'makeStrapiApiCall' für Benutzer-JWTs),
 * um cURL-Logik und Fehlerbehandlung zu standardisieren.
 *
 * @param string $documentId Die zu bestätigende documentId.
 * @return array Ein Status-Array ['status' => '...', 'message' => '...']
 */
function confirmWaitlistToken(string $documentId): array
{
    // 1. Eingabevalidierung
    if (empty($documentId)) {
        return [
            "status" => "error",
            "message" => "❌ Ungültiger Link – kein Token vorhanden."
        ];
    }

    $strapiToken = "Bearer " . STRAPI_API_TOKEN;
    $confirmUrl  = STRAPI_API_BASE . "/waitinglists/confirm";

    $payload = json_encode([
        "documentId" => $documentId
    ]);

    // 2. POST an Strapi
    $ch = curl_init($confirmUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: $strapiToken",
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10); // Timeout hinzugefügt

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    
    // 3. NEU: Netzwerkfehler-Prüfung
    $curlErr = curl_error($ch);
    curl_close($ch);

    if ($curlErr) {
        debugLog("confirmWaitlistToken cURL Error: " . $curlErr);
        return [
            "status" => "error",
            "message" => "❌ Es konnte keine Verbindung zum Server hergestellt werden. Bitte später erneut versuchen."
        ];
    }

    // 4. Antwort auswerten
    $data = json_decode($response, true);

    if ($httpCode >= 200 && $httpCode < 300 && !empty($data['success'])) {
        return [
            "status"  => "success",
            "message" => "✅ Deine Anmeldung zur Warteliste wurde bestätigt. Vielen Dank!"
        ];
    }

    // Fallback-Fehler
    return [
        "status"  => "error",
        "message" => $data['message'] ?? "❌ Der Bestätigungslink ist ungültig oder wurde bereits verwendet."
    ];
}