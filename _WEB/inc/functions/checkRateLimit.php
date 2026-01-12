<?php
// NEU: Lädt die Konfiguration, die 'SECLOGIN_SECRET' verfügbar macht.
require_once __DIR__ . '/../config.php';

/**
 * Prüft das Rate-Limit für eine IP und einen Scope bei einem externen Dienst.
 *
 * Diese Funktion signiert die Anfrage mit einem HMAC-SHA256-Schlüssel.
 *
 * @param string $scope Der Kontext der Prüfung (z.B. 'register', 'login').
 * @param int|null $limit Optional: Setzt ein neues Limit.
 * @param int|null $windowSec Optional: Setzt ein neues Zeitfenster.
 * @param string|null $ip Optional: Die zu prüfende IP. Wird automatisch ermittelt, wenn null.
 * @return array Ein standardisiertes Antwort-Array:
 * ['success' => bool, 'code' => int, 'response' => mixed]
 */
function checkRateLimit($scope = 'register', $limit = null, $windowSec = null, $ip = null) {
    
    // 1. IP-Adresse ermitteln (Deine Logik, unverändert)
    if ($ip === null) {
        if (!empty($_SERVER['HTTP_CLIENT_IP'])) {
            $ip = $_SERVER['HTTP_CLIENT_IP'];
        } elseif (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            $ipList = explode(',', $_SERVER['HTTP_X_FORWARDED_FOR']);
            $ip = trim($ipList[0]);
        } else {
            $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
        }
    }

    // 2. Body-Daten aufbauen (Deine Logik, unverändert)
    $data = [
        "ip" => $ip,
        "scope" => $scope
    ];
    if ($limit !== null) {
        $data["limit"] = (int)$limit;
    }
    if ($windowSec !== null) {
        $data["windowSec"] = (int)$windowSec;
    }
    
    $ts = time();
    $body = json_encode($data);

    // 3. SICHERHEITS-FIX: Secret sicher aus der Konfiguration laden
    $secret = defined('SECLOGIN_SECRET') ? SECLOGIN_SECRET : '';

    if (empty($secret)) {
        // Sicherer Abbruch, falls das Secret nicht geladen werden konnte
        return [
            'success' => false,
            'code' => 500, // 500 für Server-Konfigurationsfehler
            'response' => 'Server-Konfigurationsfehler: SECLOGIN_SECRET fehlt.'
        ];
    }

    // 4. Signatur erstellen (Deine Logik, unverändert)
    $sig = hash_hmac('sha256', $ts . "." . $body, $secret);

    // 5. CURL-Request (unverändert)
    $ch = curl_init("https://seclogin.meply.de/precheck/rate-limit");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Content-Type: application/json",
        "X-Timestamp: $ts",
        "X-Signature: $sig"
    ]);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $body);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    // 6. ROBUSTE FEHLERBEHANDLUNG: Einheitliche Rückgabewerte

    // Fall 1: cURL-Netzwerkfehler
    if ($err) {
        return [
            'success' => false,
            'code' => 0, // 0 für cURL/Netzwerkfehler
            'response' => "cURL Fehler: " . $err
        ];
    }

    $responseData = json_decode($response, true);

    // Fall 2: API-Fehler (z.B. 400, 429 Too Many Requests, 500)
    if ($httpCode >= 400) {
        return [
            'success' => false,
            'code' => $httpCode,
            'response' => $responseData ?? "API-Fehler ohne JSON-Antwort."
        ];
    }

    // Fall 3: Erfolg (HTTP 2xx)
    return [
        'success' => true,
        'code' => $httpCode,
        'response' => $responseData
    ];
}

// Beispiel-Aufruf (unverändert):
//$result = checkRateLimit("8.8.8.8");
//print_r($result);