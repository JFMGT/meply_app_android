<?php
/**
 * Führt einen API-Aufruf mit dem globalen ADMIN-Token (STRAPI_API_TOKEN) durch.
 *
 * Diese Funktion ist für administrative Aufgaben oder Aufrufe gedacht,
 * die erfolgen, bevor ein Benutzer eingeloggt ist (z.B. Login selbst).
 * Sie kümmert sich um JSON-Payload und robuste Fehlerbehandlung.
 *
 * @param string $method HTTP-Methode (GET, POST, PUT, DELETE).
 * @param string $url Die vollständige API-URL.
 * @param array|null $data Daten-Array für POST/PUT-Anfragen (wird JSON-kodiert).
 * @return array Standardisiertes Antwort-Array:
 * ['success' => bool, 'code' => int, 'response' => mixed]
 */

require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../functions.php';

function makeAdminStrapiApiCall(string $method, string $url, ?array $data = null): array {
    // 1. Prüfen, ob der Admin-Token verfügbar ist
    if (!defined('STRAPI_API_TOKEN') || empty(STRAPI_API_TOKEN)) {
         return [
            'success' => false,
            'code' => 500, // Interner Serverfehler (Konfiguration)
            'response' => 'Server-Konfigurationsfehler: STRAPI_API_TOKEN fehlt oder ist leer.'
         ];
    }

    // 2. cURL initialisieren
    $ch = curl_init($url);

    // 3. Standard-Optionen
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10); // 10 Sekunden Timeout

    // 4. Header mit ADMIN-Token
    $headers = [
        "Authorization: Bearer " . STRAPI_API_TOKEN,
        // Content-Type wird unten bei Bedarf hinzugefügt
    ];

    // 5. Methodenspezifische Optionen
    $method = strtoupper($method);
    if (($method === 'POST' || $method === 'PUT') && $data !== null) {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        $payload = json_encode($data);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        $headers[] = 'Content-Type: application/json';
        $headers[] = 'Content-Length: ' . strlen($payload);
    } elseif ($method === 'DELETE') {
         curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'DELETE');
    }
    // GET ist Standard

    // 6. Header setzen
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    // 7. Aufruf ausführen und Fehler abfangen
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    // 8. Einheitliche Rückgabe (identisch zu makeStrapiApiCall)
    if ($response === false) {
        // Netzwerkfehler
        return ['success' => false, 'code' => 0, 'response' => "cURL Fehler: " . $curlError];
    }
    if ($httpCode >= 400) {
        // API-Fehler (4xx, 5xx)
        return ['success' => false, 'code' => $httpCode, 'response' => json_decode($response, true) ?? $response];
    }
    // Erfolg (2xx)
    return ['success' => true, 'code' => $httpCode, 'response' => json_decode($response, true)];
}