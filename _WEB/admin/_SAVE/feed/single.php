<?php
session_start();

// Beispiel Document ID – hier DEINE einsetzen:
$documentId = 't79eg9gtebyv2ksvfs8j7q16'; // <-- Ersetzen!

$token = $_SESSION['jwt'] ?? null;
if (!$token) {
    die('Kein Token in der Session gefunden!');
}

$url = STRAPI_API_BASE . "/post-tree/" . urlencode($documentId);

// HTTP-Request vorbereiten
$curl = curl_init($url);
curl_setopt_array($curl, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        "Authorization: Bearer $token",
        "Accept: application/json",
    ],
]);

$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
curl_close($curl);

// Fehlerbehandlung
if ($httpCode !== 200) {
    echo "<strong>Fehler beim Abruf!</strong><br>Status: $httpCode<br>";
    echo "<pre>$response</pre>";
    exit;
}
echo "<pre>$response</pre>";
// JSON-Daten dekodieren
$data = json_decode($response, true);

// 🧪 Quick & Dirty Ausgabe
echo "<h2>Diskussionsbaum für: $documentId</h2>";
echo "<pre>";
printTree($data);
echo "</pre>";

// Funktion zum schönen Ausgeben
function printTree($node, $indent = 0) {
    if (!$node) return;

    echo str_repeat("  ", $indent) . "- ";
    echo htmlspecialchars($node['content']) . " ";
    echo "(" . htmlspecialchars($node['author']['username'] ?? '???') . ")";
    echo $node['isLiked'] ? " ❤️" : "";
    echo "\n";

    foreach ($node['children'] as $child) {
        printTree($child, $indent + 1);
    }
}
?>