<?php
header("Content-Type: application/json");

if (!isset($_POST['filePath'], $_POST['delimiter'])) {
    echo json_encode(["success" => false, "error" => "Fehlende Parameter"]);
    exit;
}

$cleanPath = basename($_POST['filePath']); // verhindert directory traversal
$delimiter = $_POST['delimiter'];
$targetFile = __DIR__ . "/../../uploads/" . $cleanPath;

if (!file_exists($targetFile)) {
    echo json_encode(["success" => false, "error" => "Datei nicht gefunden"]);
    exit;
}

$handle = fopen($targetFile, "r");
if (!$handle) {
    echo json_encode(["success" => false, "error" => "Datei konnte nicht geöffnet werden"]);
    exit;
}

$headers = fgetcsv($handle, 0, $delimiter);
$rows = [];

for ($i = 0; $i < 5 && ($row = fgetcsv($handle, 0, $delimiter)); $i++) {
    $rows[] = $row;
}
fclose($handle);

echo json_encode([
    "success" => true,
    "headers" => $headers,
    "preview" => $rows,
    "filePath" => $cleanPath,
    "delimiter" => $delimiter
]);
exit;
