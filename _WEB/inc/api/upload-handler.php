<?php
header("Content-Type: application/json");

// Authentifizierung hinzufügen
session_start();
require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
require_once('../config.php');

requireLogin();

$jwt = $_SESSION['jwt'] ?? null;
$profileDocumentId = $_SESSION['profile']['documentId'] ?? null;

if (!$profileDocumentId) {
    echo json_encode(["success" => false, "error" => "Nicht autorisiert"]);
    exit;
}

// Rate Limiting prüfen
if (!checkUploadRateLimit($profileDocumentId)) {
    echo json_encode(["success" => false, "error" => "Zu viele Uploads. Bitte versuchen Sie es später erneut."]);
    exit;
}

require_once("../functions/analyzeCsvFile.php");

function detectDelimiter($line) {
    $delimiters = [",", ";", "\t", "|"];
    $counts = [];

    foreach ($delimiters as $delim) {
        $fields = str_getcsv($line, $delim);
        $counts[$delim] = count($fields);
    }

    arsort($counts);
    return array_key_first($counts);
}

// Pfade sicher definieren
$targetDir = realpath(__DIR__ . '/../../uploads'); 

if (!$targetDir) {
    $targetDir = __DIR__ . '/../../uploads';
    if (!mkdir($targetDir, 0755, true)) {
        echo json_encode(["success" => false, "error" => "Server-Fehler: Upload-Verzeichnis konnte nicht erstellt werden."]);
        exit;
    }
    $targetDir = realpath($targetDir);
}

// Fall A: Datei-Upload (FormData)
if (isset($_FILES["csvFile"])) {
    
    $fileTmp = $_FILES["csvFile"]["tmp_name"];
    $fileOriginalName = $_FILES["csvFile"]["name"];
    $fileSize = $_FILES["csvFile"]["size"];

    // MIME-Type prüfen
    $allowedMimeTypes = ['text/plain', 'text/csv', 'application/vnd.ms-excel', 'application/csv'];
    $finfo = finfo_open(FILEINFO_MIME_TYPE);
    $mimeType = finfo_file($finfo, $fileTmp);
    finfo_close($finfo);

    if (!in_array($mimeType, $allowedMimeTypes)) {
        echo json_encode(["success" => false, "error" => "Ungültiger Dateityp ($mimeType)."]);
        exit;
    }

    // Endung prüfen
    $ext = strtolower(pathinfo($fileOriginalName, PATHINFO_EXTENSION));
    if ($ext !== 'csv') {
        echo json_encode(["success" => false, "error" => "Nur .csv-Dateien sind erlaubt."]);
        exit;
    }

    // Größe begrenzen (2MB)
    $maxSize = 2 * 1024 * 1024;
    if ($fileSize > $maxSize) {
        echo json_encode(["success" => false, "error" => "Datei ist zu groß. Max. 2MB erlaubt."]);
        exit;
    }

    // DATEI-ANALYSE (VOR dem Verschieben)
    $allowedTempDir = sys_get_temp_dir();
    $analysisResult = analyzeCsvFile($fileTmp, $allowedTempDir, 250);

    if (!$analysisResult['success'] || !$analysisResult['isSafe'] || !$analysisResult['isUnderLimit']) {
        
        $error = "Fehler bei der Dateianalyse.";
        if (!$analysisResult['success']) {
            $error = $analysisResult['message'];
        } else if (!$analysisResult['isSafe']) {
            $error = "Die Datei enthält potenziell schädliche Inhalte (CSV-Injection).";
        } else if (!$analysisResult['isUnderLimit']) {
            $error = "Die Datei enthält mehr als 250 Einträge. Bitte kürzen.";
        }
        
        echo json_encode(["success" => false, "error" => $error]);
        exit;
    }

    // Datei speichern (mit User-ID im Namen für zusätzliche Sicherheit)
    $safeName = uniqid("csv_", true) . ".csv";
    $targetFile = $targetDir . DIRECTORY_SEPARATOR . $safeName;

    if (move_uploaded_file($fileTmp, $targetFile)) {
        
        // Datei-Eigentümerschaft registrieren
        registerUploadedFile($safeName, $profileDocumentId);
        
        // Vorschau erzeugen
        $handle = fopen($targetFile, "r");
        $firstLine = fgets($handle);
        $delimiter = detectDelimiter($firstLine);
        rewind($handle);

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
            "filePath" => $safeName,
            "delimiter" => $delimiter
        ]);
        
    } else {
        echo json_encode(["success" => false, "error" => "Finaler Upload-Fehler. Konnte Datei nicht verschieben."]);
    }
    exit;
}

// Fallback
echo json_encode(["success" => false, "error" => "Keine Datei empfangen."]);
?>