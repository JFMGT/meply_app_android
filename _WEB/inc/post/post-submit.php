<?php
session_start();
header('Content-Type: application/json');

$jwt = $_SESSION['jwt'] ?? null;
if (!$jwt) {
  echo json_encode(['error' => 'Nicht eingeloggt']);
  exit;
}

require_once __DIR__ . '/../api/strapi-kit.php';

// 📝 Inhalt lesen
$content = trim($_POST['text'] ?? '');
if (!$content) {
  echo json_encode(['error' => 'Kein Inhalt']);
  exit;
}

// 📎 Parent-Beitrag (optional, bei Antwort)
$parentDocumentId = $_POST['parent'] ?? null;

// 🔁 Bilder + Alt-Texte
$imageIds = [];
if (!empty($_FILES['images'])) {
  foreach ($_FILES['images']['tmp_name'] as $i => $tmp) {
    $alt = $_POST['alts'][$i] ?? '';
    $cfile = new CURLFile($tmp, $_FILES['images']['type'][$i], $_FILES['images']['name'][$i]);
    $id = strapi_upload_image($cfile, $jwt, $alt, 'post');
    if ($id) {
      $imageIds[] = $id;
    }
  }
}

$visibility = $_POST['visibility'] ?? 'members';

// 🧠 Beitrag erstellen (mit optionalem Parent)
$post = strapi_create_post($content, $visibility, $imageIds, $parentDocumentId);

// 📤 Antwort senden
echo json_encode($post);
