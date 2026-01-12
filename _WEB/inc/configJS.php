<?php
header('Content-Type: application/json');
require_once('config.php'); // definiert STRAPI_API_BASE

echo json_encode([
  'apiBaseUrl' => rtrim(STRAPI_API_BASE, '/'),
  'imageBaseUrl' => rtrim(STRAPI_IMAGE_BASE, '/'),
]);