<?php
$jwt = 'cae9cf21af35b500d9cac77f642abb06767dfa068087a1e913e061db1388f5cff9f88fc8e0b780cd4c4a85c26a1296c6f5c027d9852519dbe39ddfb7b20f77dab8cf8ef1c713aa70a8134a1c938d594ea70ead259e2fb262f902f9b41c88033671ca7703a573fca4d1097b298ec1f1e565334c6a4b5541234c56ef1ee6560dc5'; 
$csvPath = 'plz.csv';


// Reset?
if (isset($_GET['reset'])) {
    unset($_SESSION['last_line']);
    echo "<p>🔁 Fortschritt zurückgesetzt.</p>";
}

// Aktuelle Zeile auslesen
$startLine = $_SESSION['last_line'] ?? 0;
echo "<p>🔢 Starte bei Zeile: " . ($startLine + 1) . "</p>";

$handle = fopen($csvPath, 'r');
if (!$handle) {
    die("❌ CSV konnte nicht geöffnet werden.");
}

$header = fgetcsv($handle); // Header
$currentLine = 0;

while (($row = fgetcsv($handle)) !== false) {
    $currentLine++;

    // Überspringe, wenn noch nicht an der gespeicherten Stelle
    if ($currentLine <= $startLine) {
        continue;
    }

    $data = array_combine($header, $row);

    $zip = trim($data['zip']);
    $city = trim($data['city']);
    $country = strtoupper(trim($data['country']));
    $lat = floatval($data['lat']);
    $lng = floatval($data['lng']);
    $code = "{$country}-{$zip}";

    // Existenz prüfen
    $checkUrl = "https://admin.meeplemates.de/api/zipcodes?filters[zip][\$eq]=" . urlencode($zip)
              . "&filters[city][\$eq]=" . urlencode($city);

    $checkCh = curl_init($checkUrl);
    curl_setopt($checkCh, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($checkCh, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $jwt,
        'Accept: application/json',
    ]);
    $checkResponse = curl_exec($checkCh);
    curl_close($checkCh);

    $checkData = json_decode($checkResponse, true);
    $alreadyExists = !empty($checkData['data']);

    if ($alreadyExists) {
        echo "⏭️ [$currentLine] $zip $city – bereits vorhanden<br>";
        $_SESSION['last_line'] = $currentLine;
        continue;
    }

    // Eintrag erstellen
    $payload = [
        'data' => [
            'zip' => $zip,
            'city' => $city,
            'country' => $country,
            'coordinates' => [
                'lat' => $lat,
                'lng' => $lng,
            ],
            'code' => $code,
        ]
    ];

    $ch = curl_init('https://admin.meeplemates.de/api/zipcodes');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $jwt,
        'Content-Type: application/json',
    ]);
    curl_setopt($ch, CURLOPT_POST, true);

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($status === 200 || $status === 201) {
        echo "✅ [$currentLine] Import $zip $city erfolgreich (HTTP $status)<br>";
        $_SESSION['last_line'] = $currentLine;
    } else {
        echo "<strong>❌ Fehler bei [$currentLine] $zip $city:</strong> HTTP $status<br>";
        echo "<pre>Antwort: $response</pre><hr>";
    }
}

fclose($handle);
