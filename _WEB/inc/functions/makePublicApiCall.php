<?php
/**
 * Führt einen API-Aufruf an einen ÖFFENTLICHEN Endpunkt durch (kein JWT/Token nötig).
 * Kümmert sich um JSON-Payload und Fehlerbehandlung.
 */
function makePublicApiCall(string $method, string $url, ?array $data = null): array {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    $headers = []; // Keine Auth nötig

    if (($method === 'POST' || $method === 'PUT') && $data !== null) {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        $payload = json_encode($data);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        $headers[] = 'Content-Type: application/json';
        $headers[] = 'Content-Length: ' . strlen($payload);
    }
    if (!empty($headers)) {
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    }

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        return ['success' => false, 'code' => 0, 'response' => "cURL Fehler: " . $curlError];
    }
    if ($httpCode >= 400) {
        return ['success' => false, 'code' => $httpCode, 'response' => json_decode($response, true) ?? $response];
    }
    return ['success' => true, 'code' => $httpCode, 'response' => json_decode($response, true)];
}