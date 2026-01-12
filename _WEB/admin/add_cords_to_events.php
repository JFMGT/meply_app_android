<?php
session_start();
$jwt = 'cae9cf21af35b500d9cac77f642abb06767dfa068087a1e913e061db1388f5cff9f88fc8e0b780cd4c4a85c26a1296c6f5c027d9852519dbe39ddfb7b20f77dab8cf8ef1c713aa70a8134a1c938d594ea70ead259e2fb262f902f9b41c88033671ca7703a573fca4d1097b298ec1f1e565334c6a4b5541234c56ef1ee6560dc5';



$result = null;
$error = null;

function fetchWithJwt($url, $jwt) {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Authorization: Bearer ' . $jwt,
        'Accept: application/json'
    ]);
    $response = curl_exec($ch);
    curl_close($ch);
    return $response;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = trim($_POST['plz_ort'] ?? '');
    if (preg_match('/^(\d{4,5})\s+(.+)$/', $input, $matches)) {
        $zip = $matches[1];
        $city = $matches[2];
        $country = 'DE';

        

        // 1. Suche nach PLZ + Ort
        $url = "https://admin.meeplemates.de/api/zipcodes?filters[zip][\$eq]=" . urlencode($zip)
             . "&filters[city][\$eq]=" . urlencode($city)
             . "&pagination[pageSize]=1";

        $response = fetchWithJwt($url, $jwt);
        $data = json_decode($response, true);

        if (!empty($data['data'][0]['coordinates'])) {
            $result = $data['data'][0]['coordinates'];
        } else {
            // 2. Fallback: Nur PLZ suchen
            $fallbackUrl = "https://admin.meeplemates.de/api/zipcodes?filters[zip][\$eq]=" . urlencode($zip)
                         . "&pagination[pageSize]=1";
            $response = fetchWithJwt($fallbackUrl, $jwt);
            $fallbackData = json_decode($response, true);

            if (!empty($fallbackData['data'][0]['coordinates'])) {
                $result = $fallbackData['data'][0]['coordinates'];
                $error = "⚠️ Ort nicht gefunden. Koordinaten stammen nur aus PLZ.";
            } else {
                $error = "❌ Keine Koordinaten gefunden.";
            }
        }
    } else {
        $error = "❌ Ungültiges Format. Bitte gib PLZ und Ort an (z. B. 33334 Gütersloh)";
    }
}

?>

<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <title>Koordinaten-Suche</title>
    <style>
        body { font-family: sans-serif; padding: 2em; max-width: 600px; margin: auto; }
        label, input, button { font-size: 1rem; display: block; margin-bottom: 1em; }
        input { padding: 0.5em; width: 100%; }
        pre { background: #f4f4f4; padding: 1em; }
    </style>
</head>
<body>
    <h1>📍 Koordinaten-Suche (via Strapi)</h1>

    <form method="POST">
        <label for="plz_ort">PLZ und Ort eingeben:</label>
        <input type="text" name="plz_ort" id="plz_ort" placeholder="z. B. 33334 Gütersloh" required>
        <button type="submit">Suchen</button>
    </form>

    <?php if ($result): ?>
    <?php if ($error): ?><p><?= $error ?></p><?php endif; ?>
    <h3>✅ Koordinaten gefunden:</h3>
    <pre id="coords"><?= json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES) ?></pre>
    <button onclick="copyCoords()">📋 In Zwischenablage kopieren</button>

    <script>
        function copyCoords() {
            const text = document.getElementById('coords').innerText;
            navigator.clipboard.writeText(text)
                .then()
                .catch(err => alert('Kopieren fehlgeschlagen: ' + err));
        }
    </script>
<?php elseif ($error): ?>
    <p><?= $error ?></p>
<?php endif; ?>

</body>
</html>

