<?php
// Benötigt eine Implementierung von getCoordinatesFromZip unten!

/**
 * Berechnet die Distanz zwischen zwei PLZs mittels Haversine-Formel.
 *
 * @param string $zip1 PLZ 1.
 * @param string $zip2 PLZ 2.
 * @param float $earthRadius Erdradius in km (Standard: 6371).
 * @return float|null Distanz in km oder null, wenn Koordinaten nicht gefunden werden konnten.
 */
function haversineGreatCircleDistance(string $zip1, string $zip2, float $earthRadius = 6371): ?float
{
    // HINWEIS: Diese Funktion ist abhängig von getCoordinatesFromZip!
    $coord1 = getCoordinatesFromZip($zip1);
    $coord2 = getCoordinatesFromZip($zip2);

    // Wenn eine der Koordinaten nicht gefunden wurde -> null zurückgeben
    if ($coord1 === null || $coord2 === null) {
        return null;
    }

    // Koordinaten extrahieren
    [$lat1, $lon1] = $coord1;
    [$lat2, $lon2] = $coord2;

    // Grad in Radiant umwandeln
    $latFrom = deg2rad($lat1);
    $lonFrom = deg2rad($lon1);
    $latTo   = deg2rad($lat2);
    $lonTo   = deg2rad($lon2);

    // Deltas berechnen
    $latDelta = $latTo - $latFrom;
    $lonDelta = $lonTo - $lonFrom;

    // Haversine-Formel anwenden
    $angle = 2 * asin(sqrt(pow(sin($latDelta / 2), 2) +
             cos($latFrom) * cos($latTo) * pow(sin($lonDelta / 2), 2)));

    // Ergebnis in km zurückgeben
    return $angle * $earthRadius;
}

// -------------------------------------------------------------------------

/**
 * PLATZHALTER-FUNKTION: Holt Koordinaten für eine Postleitzahl.
 *
 * !!! WICHTIG !!!
 * Diese Funktion ist aktuell nur ein Platzhalter mit Beispiel-Daten.
 * Sie MUSS durch eine echte Implementierung ersetzt werden, die auf eine
 * Datenbank, eine lokale Geo-Datei oder eine geeignete API zugreift.
 * Die Verwendung der Nominatim-API (via getCoordinates) ist möglich,
 * aber SEHR LANGSAM (2 API-Calls pro Distanzberechnung).
 *
 * @param string $zip Die zu suchende Postleitzahl.
 * @return array|null Ein Array [latitude, longitude] oder null, wenn nicht gefunden.
 */
function getCoordinatesFromZip(string $zip): ?array
{
    // Nur Ziffern aus PLZ extrahieren
    $zip = preg_replace('/\D/', '', $zip);
    if (empty($zip)) {
        return null;
    }

    // === PLATZHALTER: BEGINN ===
    // Dies ist nur ein Beispiel. ERSETZE DIES durch deine Datenquelle!
    $zips = [
        '10115' => [52.532, 13.384], // Berlin Mitte
        '50667' => [50.940, 6.957],  // Köln Altstadt
        '80331' => [48.137, 11.575], // München Altstadt
        '33607' => [52.024, 8.558],  // Bielefeld (Beispiel)
        // ... FÜGE HIER DEINE DATENQUELLE EIN (DB-Abfrage, etc.) ...
    ];

    // Suche im Platzhalter-Array
    if (isset($zips[$zip])) {
        return $zips[$zip];
    }
    // === PLATZHALTER: ENDE ===

    // Wenn nicht im Platzhalter gefunden (oder wenn echte Implementierung fehlschlägt)
    return null;
}