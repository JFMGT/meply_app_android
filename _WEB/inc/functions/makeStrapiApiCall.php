<?php
/**
 * Eine zentrale Wrapper-Funktion für alle Strapi-API-Aufrufe.
 *
 * Diese Funktion kümmert sich um Authentifizierung, Methodenauswahl,
 * das Senden von JSON-Daten und eine robuste Fehlerbehandlung.
 *
 * WICHTIG: Diese Funktion erwartet, dass `session_start()` bereits
 * an einer zentralen Stelle (z.B. in der Haupt-functions.php) aufgerufen wurde.
 *
 * @param string $method HTTP-Methode (z.B. 'GET', 'POST', 'PUT').
 * @param string $url Die vollständige API-URL, die aufgerufen werden soll.
 * @param array|null $data Ein assoziatives PHP-Array, das für POST/PUT-
 * Anfragen als JSON-Body gesendet wird.
 * @param bool $useSystemToken Wenn true, wird STRAPI_API_TOKEN statt User-JWT verwendet.
 * @return array Ein assoziatives Array mit einem einheitlichen Format:
 * [
 * 'success' => bool,
 * 'code'    => int (HTTP-Statuscode, 0 bei cURL-Fehler),
 * 'response' => mixed (Die decodierte JSON-Antwort oder Fehlermeldung)
 * ]
 */

require_once __DIR__ . '/../config.php';
function makeStrapiApiCall(string $method, string $url, ?array $data = null, bool $useSystemToken = false): array
{
    // 1. Authentifizierung holen
    if ($useSystemToken) {
        // System-Token aus Umgebungsvariable oder Konfiguration
        $token = defined('STRAPI_API_TOKEN') ? STRAPI_API_TOKEN : null;

        
        if (empty($token)) {
            return [
                'success' => false,
                'code' => 500,
                'response' => 'Fehler: STRAPI_API_TOKEN ist nicht konfiguriert.'
            ];
        }
    } else {
        // User-JWT aus Session
        // HINWEIS: session_start() muss bereits vorher global aufgerufen worden sein!
        $token = $_SESSION['jwt'] ?? '';
        
        if (empty($token)) {
            return [
                'success' => false,
                'code' => 401,
                'response' => 'Fehler: Kein JWT-Token in der Session gefunden.'
            ];
        }
    }

    // 2. cURL initialisieren
    $ch = curl_init($url);

    // 3. Standard-Optionen setzen
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10); // 10 Sekunden Timeout

    // 4. Standard-Header (Auth)
    $headers = [
        "Authorization: Bearer $token"
    ];

    // 5. Methodenspezifische Optionen
    $method = strtoupper($method);

    if ($method === 'POST' || $method === 'PUT' || $method === 'DELETE') {
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        
        if ($data !== null && ($method === 'POST' || $method === 'PUT')) {
            $payload = json_encode($data);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
            
            $headers[] = 'Content-Type: application/json';
            $headers[] = 'Content-Length: ' . strlen($payload);
        }
    } elseif ($method === 'GET') {
        // GET ist Standard, nichts zu tun
        curl_setopt($ch, CURLOPT_HTTPGET, true);
    }

    // 6. Header setzen
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    // 7. Aufruf ausführen und Fehler abfangen
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    
    curl_close($ch);

    // 8. Einheitliche Rückgabe
    
    // Fall 1: cURL-Netzwerkfehler
    if ($response === false) {
        return [
            'success' => false,
            'code' => 0,
            'response' => "cURL Fehler: " . $curlError
        ];
    }

    // Fall 2: API-Fehler (z.B. 404, 401, 500)
    if ($httpCode >= 400) {
        $errorResponse = json_decode($response, true) ?? $response;
        
        return [
            'success' => false,
            'code' => $httpCode,
            'response' => $errorResponse
        ];
    }

    // Fall 3: Erfolg (200, 201)
    return [
        'success' => true,
        'code' => $httpCode,
        'response' => json_decode($response, true)
    ];
}