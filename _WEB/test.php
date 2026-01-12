<?php
session_start();
header('Content-Type: application/json');

// 🔐 JWT muss vorhanden sein
if (!isset($_SESSION['jwt'])) {
  echo json_encode(['error' => 'Nicht eingeloggt']);
  exit;
}

$jwt = $_SESSION['jwt'];
$imageUrl = 'https://2025.meeplemates.de/baby.jpg';
$imageData = file_get_contents($imageUrl);

if (!$imageData) {
  echo json_encode(['error' => 'Bild konnte nicht geladen werden']);
  exit;
}

// 📁 Temporäre Datei erstellen
$tmpFile = tempnam(sys_get_temp_dir(), 'upload_');
file_put_contents($tmpFile, $imageData);
$cfile = new CURLFile($tmpFile, 'image/jpeg', 'baby.jpg');

// 🖼️ Bild zu Strapi hochladen
$upload = curl_init( STRAPI_API_BASE . '/upload');
curl_setopt($upload, CURLOPT_RETURNTRANSFER, true);
curl_setopt($upload, CURLOPT_POST, true);
curl_setopt($upload, CURLOPT_POSTFIELDS, [
  'files' => $cfile,
  'fileInfo' => json_encode(['alternativeText' => 'Babybild'])
]);
curl_setopt($upload, CURLOPT_HTTPHEADER, [
  "Authorization: Bearer $jwt"
]);
$response = curl_exec($upload);
$status = curl_getinfo($upload, CURLINFO_HTTP_CODE);
$error = curl_error($upload);
curl_close($upload);

// Datei löschen
unlink($tmpFile);

// ❌ Fehler beim Upload
if ($status !== 200 || !$response) {
  echo json_encode(['error' => 'Upload fehlgeschlagen', 'curl_error' => $error, 'raw' => $response]);
  exit;
}

// 📦 Antwort auswerten
$data = json_decode($response, true);
$imageId = $data[0]['id'] ?? null;

if (!$imageId) {
  echo json_encode(['error' => 'Keine Bild-ID erhalten', 'upload_response' => $data]);
  exit;
}

// ✅ Bild erfolgreich hochgeladen – Beitrag erstellen
$postData = [
  'data' => [
    'content' => 'Beitrag mit Babybild',
    'image' => [$imageId]  // falls multiple: true
  ]
];

$ch = curl_init( STRAPI_API_BASE . '/posts');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
  "Authorization: Bearer $jwt",
  "Content-Type: application/json"
]);
$postResponse = curl_exec($ch);
$postStatus = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// 🧾 Rückgabe
echo json_encode([
  'upload_status' => $status,
  'image_id' => $imageId,
  'post_status' => $postStatus,
  'post_response' => json_decode($postResponse, true)
]);
