<?php
require_once('../config.php');
function strapi_create_post(string $content, string $visibility = 'members', array $imageIds = [], ?string $parentDocumentId = null): array {
  $jwt = $_SESSION['jwt'];

  // 🧩 Basisdaten
  $payload = [
    'data' => [
      'content' => $content,
      'image' => $imageIds, // Feldname im Content Type
       'visibility' => $visibility
    ]
  ];

  // ➕ Parent-Beziehung (wenn vorhanden)
  if ($parentDocumentId) {
    $payload['data']['parent'] = [
      'connect' => ['documentId' => $parentDocumentId]
    ];
  }

  // 🌐 Anfrage an Strapi senden
  $ch = curl_init( STRAPI_API_BASE . '/posts');
  curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_HTTPHEADER => [
      "Authorization: Bearer $jwt",
      "Content-Type: application/json"
    ]
  ]);

  $res = curl_exec($ch);
  $data = json_decode($res, true);
  $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
  curl_close($ch);

  return [
    'status' => $status,
    'response' => $data,
    'raw' => $res
  ];
}



function strapi_upload_image(CURLFile $file, $jwt, string $alt = '', string $purpose = '', ): ?int {
  //$jwt = $_SESSION['jwt'];

  // Feldname MUSS zu ctx.request.files?.files passen → also "files"
  $form = [
    'files'   => $file,     // <— wichtig
    'alt'     => $alt,
    'purpose' => $purpose,
  ];

  $ch = curl_init(STRAPI_API_BASE . '/user-uploads/upload');
  curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => $form, // multipart/form-data wird automatisch gesetzt
    CURLOPT_HTTPHEADER     => ["Authorization: Bearer $jwt"],
  ]);





  $res = curl_exec($ch);
  curl_close($ch);

  $data = json_decode($res, true);

  // Falls dein Controller { success, data: entry } zurückgibt:
  if (isset($data['data']['file'])) {
    return (int)$data['data']['file'];
  }

  // Falls du später die Multi-Upload-Variante nutzt ({ uploads: [{ uploadId, ... }] }):
  if (isset($data['uploads'][0]['uploadId'])) {
    return (int)$data['uploads'][0]['uploadId'];
  }

  return null;
}




function strapi_fetch_feed(): array {
  $jwt = $_SESSION['jwt'] ?? null;

  if (!$jwt) {
    return ['error' => 'Nicht eingeloggt'];
  }

  $ch = curl_init( STRAPI_API_BASE . '/feed');
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: Bearer $jwt",
    "Content-Type: application/json"
  ]);

  $response = curl_exec($ch);
  $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
  $error = curl_error($ch);
  curl_close($ch);

  return [
    'status' => $status,
    'decoded' => json_decode($response, true),
    'raw' => $response,
    'curl_error' => $error
  ];
}
