<?php
// NEU: Lädt die Konfiguration, die debugLog() verfügbar macht.
require_once __DIR__ . '/../config.php';

/**
 * Holt die Geo-Koordinaten (lat/lng) für eine Adresse über die Nominatim API.
 *
 * @param string $strasse Straße (optional).
 * @param string $plz Postleitzahl (optional).
 * @param string $ort Ort (optional).
 * @return array|null Ein Array ['lat' => float, 'lng' => float] bei Erfolg,
 * oder null bei Fehler / keinem Treffer.
 */
function getCoordinates($strasse, $plz, $ort) {
    
    // 1. KORREKTUR: Robuste Adress-Zusammensetzung
    $parts = [];
    if (!empty($strasse)) {
        $parts[] = $strasse;
    }
    // Kombiniere PLZ und Ort, entferne überflüssige Leerzeichen
    $plzOrt = trim("$plz $ort");
    if (!empty($plzOrt)) {
        $parts[] = $plzOrt;
    }

    // Wenn alle Teile leer sind, gibt es nichts zu suchen
    if (empty($parts)) {
        return null;
    }
    
    // Teile mit Komma verbinden (z.B. "Musterstraße 1, 12345 Berlin")
    $adresse = implode(', ', $parts);
    
    // Adresse URL-encoden (war bereits sicher)
    $adresseEncoded = urlencode($adresse);

    // Nominatim API URL
    $url = "https://nominatim.openstreetmap.org/search?q=$adresseEncoded&format=json&limit=1";

    // API Call
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_USERAGENT, "MeepleMates-Location-Finder/1.0"); // (Wichtig und korrekt!)
    curl_setopt($ch, CURLOPT_TIMEOUT, 5); // (NEU: Timeout hinzugefügt)
    
    $response = curl_exec($ch);
    
    // 2. KORREKTUR: Vollständige Fehlerbehandlung
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlErr = curl_error($ch);
    curl_close($ch);

    // Fall 1: Netzwerkfehler
    if ($curlErr) {
        debugLog("getCoordinates cURL Error: " . $curlErr);
        return null;
    }

    // Fall 2: API-Fehler (z.B. 429 Too Many Requests, 500 Server Error)
    if ($httpCode !== 200) {
        debugLog("getCoordinates API Error: HTTP $httpCode | Response: $response");
        return null;
    }

    // 3. Antwort verarbeiten (unverändert)
    $data = json_decode($response, true);

    if (!empty($data[0])) {
        return [
            'lat' => (float)$data[0]['lat'],
            'lng' => (float)$data[0]['lon']
        ];
    } else {
        // Adresse nicht gefunden (kein Fehler, nur kein Ergebnis)
        return null;
    }
}