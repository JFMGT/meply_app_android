<?php
// === Konfiguration ===
$apiUrl = "https://admin.meeplemates.de/api/locations/2"; // <--- ID hier einsetzen
$apiToken = "f47c5205f77bcf6f1d534ee97492e7818c22c88a6de690c3ffbe18073d355177065cbc2486032035060879c4c33fa132ac3cfb9b1e659e20986248bd1d32f31704a1075f306b450c5cf18692c95c6392365a458017df2d4ec953ee0757366f9e272ad80ff6d8b31e8a4e1e97ed6224c72da33bffc7ce12bde7b736e8e5dbc97a"; // <--- Token hier einsetzen

// === Daten vorbereiten ===
$data = [
    "data" => [
        "lat" => 52.5,
        "lng" => 13.4
    ]
];

$payload = json_encode($data);

// === cURL-Request ===
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $apiUrl);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PATCH");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $apiToken",
    "Content-Type: application/json",
    "Accept: application/json"
]);
curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);

$response = curl_exec($ch);

if (!$response) {
    echo "❌ Fehler: " . curl_error($ch) . "\n";
} else {
    echo "✅ Antwort:\n$response\n";
}

curl_close($ch);
