<?php

// === Konfiguration ===
$strapiBase = "https://admin.meeplemates.de/api/locations";
$apiToken = ""; // ⚠️ Ersetze durch echten Token
$userAgent = "MeepleMatesGeocoder/1.0";

// === Hole alle Einträge ===
function fetchLocations($strapiBase, $apiToken) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $strapiBase . "?pagination[pageSize]=100");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $apiToken",
        "Content-Type: application/json"
    ]);

    $response = curl_exec($ch);
    curl_close($ch);

    return json_decode($response, true)["data"];
}

// === Hole Koordinaten von Nominatim ===
function geocodeAddress($address, $userAgent) {
    $url = "https://nominatim.openstreetmap.org/search?" . http_build_query([
        'q' => $address,
        'format' => 'json',
        'limit' => 1
    ]);

    $opts = [
        "http" => [
            "header" => "User-Agent: $userAgent\r\n"
        ]
    ];

    $context = stream_context_create($opts);
    $result = file_get_contents($url, false, $context);
    $data = json_decode($result, true);

    if (!empty($data[0])) {
        return [
            'lat' => (float)$data[0]['lat'],
            'lng' => (float)$data[0]['lon']
        ];
    }

    return null;
}

// === Aktualisiere Koordinaten in Strapi ===
function updateCoordinates($id, $lat, $lng, $strapiBase, $apiToken) {
    $payload = [
        "data" => [
            "lat" => $lat,
            "lng" => $lng
        ]
    ];

    echo "📦 Aktualisiere ID $id mit Koordinaten: lat=$lat, lng=$lng\n";

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, "$strapiBase/{$id}");
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PATCH");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $apiToken",
        "Content-Type: application/json",
        "Accept: application/json"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

    $response = curl_exec($ch);

    if (!$response) {
        echo "❌ Fehler: " . curl_error($ch) . "\n";
    } else {
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        echo "✅ HTTP $httpCode\n";
        echo "➡️ Antwort:\n$response\n";
    }

    curl_close($ch);

    return $response;
}

// === Hauptlogik ===
$locations = fetchLocations($strapiBase, $apiToken);

foreach ($locations as $entry) {
    $id = $entry["documentId"]; // ✅ UUID verwenden!
    $attrs = $entry["attributes"];

    if (!empty($attrs["lat"]) && !empty($attrs["lng"])) {
        echo "✅ $id hat bereits Koordinaten.\n";
        continue;
    }
    // Adresse zusammensetzen aus Attributen
    $addressParts = [
        $attrs["Strasse"] ?? '',
        $attrs["Hausnummer"] ?? '',
        $attrs["PLZ"] ?? '',
        $attrs["Ort"] ?? ''
    ];

     $addressParts = [
        $entry["Strasse"] ?? '',
        $entry["Hausnummer"] ?? '',
        $entry["PLZ"] ?? '',
        $entry["Ort"] ?? ''
    ];

    $fullAddress = implode(' ', array_filter($addressParts));

    echo "📍 Suche Koordinaten für: $fullAddress...\n";
    $coords = geocodeAddress($fullAddress, $userAgent);

    if ($coords) {
        echo "➡️  Lat: {$coords['lat']} | Lng: {$coords['lng']}\n";
        $updateResponse = updateCoordinates($id, $coords['lat'], $coords['lng'], $strapiBase, $apiToken);
        echo "✅ Aktualisiert ID $id\n";
    } else {
        echo "❌ Keine Koordinaten gefunden für ID $id ($fullAddress)\n";
    }

    // OSM-Nominatim will Pausen zwischen Requests
    sleep(1);
}

echo "🏁 Fertig.\n";
