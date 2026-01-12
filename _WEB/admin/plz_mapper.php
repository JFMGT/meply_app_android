<?php
// ---------------------------------------------
// Konfiguration
// ---------------------------------------------
$RADIUS_KM = 100;  // 💡 Radius setzen
$JWT = 'cae9cf21af35b500d9cac77f642abb06767dfa068087a1e913e061db1388f5cff9f88fc8e0b780cd4c4a85c26a1296c6f5c027d9852519dbe39ddfb7b20f77dab8cf8ef1c713aa70a8134a1c938d594ea70ead259e2fb262f902f9b41c88033671ca7703a573fca4d1097b298ec1f1e565334c6a4b5541234c56ef1ee6560dc5';
define('STRAPI_API_BASE', 'https://admin.meeplemates.de/api');
$OUTPUT_PATH = __DIR__ . "/plz_map_{$RADIUS_KM}.json";

// Bestehende Datei laden oder initialisieren
$result = file_exists($OUTPUT_PATH)
    ? json_decode(file_get_contents($OUTPUT_PATH), true)
    : [];

// Erstelle alle zweistelligen Präfixe 01 bis 99
$allPrefixes = array_map(fn($n) => str_pad($n, 2, '0', STR_PAD_LEFT), range(1, 99));

// Hole den nächsten noch nicht bearbeiteten Präfix
$pendingPrefixes = array_diff($allPrefixes, array_keys($result));

if (empty($pendingPrefixes)) {
    echo "✅ Alle Präfixe wurden bereits berechnet.\n";
    exit;
}

$CURRENT_PREFIX = array_shift($pendingPrefixes);
echo "🔁 Bearbeite Präfix: $CURRENT_PREFIX\n";

// ---------------------------------------------
// Hilfsfunktionen
// ---------------------------------------------

function haversine($lat1, $lon1, $lat2, $lon2) {
    $R = 6371;
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat/2)**2 + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLon/2)**2;
    return $R * 2 * atan2(sqrt($a), sqrt(1 - $a));
}

// Holt alle PLZ mit einem gegebenen Prefix
function getPlzEntriesByPrefix($prefix, $jwt) {
    $entries = [];
    $page = 1;

    while (true) {
        $url = STRAPI_API_BASE . "/zipcodes?pagination[pageSize]=100&pagination[page]=$page";

        if ($prefix !== '') {
            $url .= "&filters[zip][$startsWith]=$prefix";
        }

        $url .= "&populate=*";

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $jwt,
            'Content-Type: application/json'
        ]);
        $response = curl_exec($ch);
        curl_close($ch);

        $data = json_decode($response, true);
        $pageEntries = $data['data'] ?? [];
        if (empty($pageEntries)) break;

        $entries = array_merge($entries, $pageEntries);
        $page++;
    }

    return $entries;
}

// ---------------------------------------------
// Schritt 1: Hole PLZ mit aktuellem Präfix
// ---------------------------------------------
$myPlzEntries = getPlzEntriesByPrefix($CURRENT_PREFIX, $JWT);

if (empty($myPlzEntries)) {
    echo "⚠️  Keine PLZ mit Prefix $CURRENT_PREFIX gefunden.\n";
    $result[$CURRENT_PREFIX] = [$CURRENT_PREFIX];
    file_put_contents($OUTPUT_PATH, json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    exit;
}

// ---------------------------------------------
// Schritt 2: Hole ALLE PLZ aus DB
// (nur einmal nötig, weil wir mit allen vergleichen müssen)
// ---------------------------------------------
$allPlzEntries = getPlzEntriesByPrefix('', $JWT);

// ---------------------------------------------
// Schritt 3: Finde erreichbare Nachbar-Präfixe
// ---------------------------------------------
$nearPrefixes = [$CURRENT_PREFIX];

foreach ($myPlzEntries as $entry) {
    $zip = $entry['zip'] ?? null;
    $coords = $entry['coordinates'] ?? null;

    if (!$zip || !$coords || !isset($coords['lat'], $coords['lng'])) continue;

    foreach ($allPlzEntries as $check) {
        $checkZip = $check['zip'] ?? null;
        $checkCoords = $check['coordinates'] ?? null;

        if (!$checkZip || !$checkCoords || !isset($checkCoords['lat'], $checkCoords['lng'])) continue;

        $distance = haversine($coords['lat'], $coords['lng'], $checkCoords['lat'], $checkCoords['lng']);
        if ($distance <= $RADIUS_KM) {
            $checkPrefix = substr($checkZip, 0, 2);
            $nearPrefixes[] = $checkPrefix;
        }
    }
}

$nearPrefixes = array_values(array_unique($nearPrefixes));
sort($nearPrefixes);

// ---------------------------------------------
// Schritt 4: Speichern
// ---------------------------------------------
$result[$CURRENT_PREFIX] = $nearPrefixes;
file_put_contents($OUTPUT_PATH, json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));

echo "✅ $CURRENT_PREFIX → [" . implode(', ', $nearPrefixes) . "] gespeichert.\n";