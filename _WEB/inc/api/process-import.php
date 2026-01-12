<?php
ob_start();

header("Content-Type: application/json");
session_start();

require_once('../functions.php');

// CSRF-Token validieren
requireCsrfToken();
require_once('../config.php');

requireLogin();

$jwt = $_SESSION['jwt'] ?? null;
$profileId = $_SESSION['profile']['id'] ?? null;
$profileDocumentId = $_SESSION['profile']['documentId'] ?? null;

if (!$profileDocumentId || !$jwt) {
    ob_end_clean();
    echo json_encode(["error" => "Nicht autorisiert"]);
    exit;
}

$maxRating = 0;
$importSessionKey = hash('crc32b', $profileDocumentId) . "_" . time();

$data = json_decode(file_get_contents("php://input"), true);
if (!$data || !isset($data['filePath']) || !isset($data['mapping'])) {
    ob_end_clean();
    echo json_encode(["error" => "Fehlende Daten"]);
    exit;
}

// NEU: Batch-Parameter
$offset = isset($data['offset']) ? (int)$data['offset'] : 0;
$limit = isset($data['limit']) ? (int)$data['limit'] : 5; // 5 Spiele pro Batch

$allowedDelimiters = [",", ";", "\t", "|"];
$delimiter = $data['delimiter'] ?? ",";

if (!in_array($delimiter, $allowedDelimiters, true)) {
    ob_end_clean();
    echo json_encode(["error" => "Ungültiges Trennzeichen"]);
    exit;
}

$total = 0;
$existing = 0;
$new = 0;
$results = [];
$log = [];

$log[] = "Batch-Import gestartet für Profil: " . $profileDocumentId . " (Offset: $offset, Limit: $limit)";

try {
    $filePath = getSecureUploadPath($data['filePath']);
    $log[] = "Sicherer Pfad ermittelt: " . $filePath;
} catch (Exception $e) {
    $log[] = "Exception beim Pfad: " . $e->getMessage();
    ob_end_clean();
    echo json_encode(["error" => "Ungültiger Dateipfad: " . $e->getMessage(), "log" => $log]);
    exit;
}

$cleanPath = basename($data['filePath']);
if (!verifyFileOwnership($cleanPath, $profileDocumentId)) {
    $log[] = "Eigentümerschaft fehlgeschlagen";
    ob_end_clean();
    echo json_encode(["error" => "Zugriff verweigert", "log" => $log]);
    exit;
}

if (!file_exists($filePath)) {
    $log[] = "Datei existiert nicht";
    ob_end_clean();
    echo json_encode(["error" => "Datei nicht gefunden.", "log" => $log]);
    exit;
}

$mapping = $data['mapping'];
$titleIndex = $mapping['title'] ?? null;
if ($titleIndex === null) {
    ob_end_clean();
    echo json_encode(["error" => "Keine Spalte als 'title' zugewiesen."]);
    exit;
}

$bggIdIndex = $mapping['bgg_id'] ?? null;
$ratingIndex = $mapping['rating'] ?? null;

$handle = fopen($filePath, "r");
if (!$handle) {
    ob_end_clean();
    echo json_encode(["error" => "Datei konnte nicht geöffnet werden", "log" => $log]);
    exit;
}

$headers = fgetcsv($handle, 0, $delimiter);

// NEU: Zähle Gesamtanzahl der Zeilen (für Progress)
$totalRows = 0;
while (fgetcsv($handle, 0, $delimiter) !== false) {
    $totalRows++;
}
rewind($handle);
fgetcsv($handle, 0, $delimiter); // Header überspringen

// NEU: Springe zum Offset
$currentRow = 0;
while ($currentRow < $offset && fgetcsv($handle, 0, $delimiter) !== false) {
    $currentRow++;
}

$apiBase = STRAPI_API_BASE . "/boardgames";
$token = "Bearer " . $jwt;

function sanitizeCell($value) {
    $v = trim($value);
    return preg_match('/^[=+\-@]/', $v) ? "'" . $v : $v;
}

function normalizeRatingToFiveScale($value, $maxValue) {
    if ($maxValue <= 0) {
        return 0;
    }
    $scaled = ($value / $maxValue) * 5;
    return max(0, min(5, round($scaled)));
}

// NEU: Verarbeite nur $limit Zeilen
$processed = 0;
$rowNumber = $offset;

while (($row = fgetcsv($handle, 0, $delimiter)) !== false && $processed < $limit) {
    $rowNumber++;
    
    $title = sanitizeCell($row[$titleIndex] ?? "");
    if ($title === "") {
        $log[] = "Zeile $rowNumber: Leerer Titel, übersprungen";
        continue;
    }

    if (count($row) < count($headers)) {
        $log[] = "Zeile $rowNumber: Unvollständige Zeile, übersprungen";
        continue;
    }

    $total++;
    $processed++;
    
    $bggId = null;
    if ($bggIdIndex !== null && isset($row[$bggIdIndex])) {
        $rawBggId = sanitizeCell($row[$bggIdIndex]);
        if (is_numeric($rawBggId) && $rawBggId > 0) {
            $bggId = (int)$rawBggId;
        }
    }
    
    $log[] = "🔍 Zeile $rowNumber - Titel: '" . $title . "'" . ($bggId ? " (BGG ID: $bggId)" : "");

    $rating = null;
    if ($ratingIndex !== null && isset($row[$ratingIndex]) && is_numeric($row[$ratingIndex])) {
        $rating = floatval($row[$ratingIndex]);
        if ($rating > $maxRating) {
            $maxRating = $rating;
        }
    }

    $gameResult = createOrFindBoardGame($title, $bggId, $importSessionKey);

    if ($gameResult['success']) {
        $gameId = $gameResult['id']; 
        
        if ($gameResult['existed']) {
            $existing++;
            $log[] = "✅ Spiel gefunden (ID: $gameId)";
            $results[] = ["title" => $title, "status" => "existing", "bgg_id" => $bggId];
        } else {
            $new++;
            $log[] = "✅ Spiel neu angelegt (ID: $gameId)";
            $results[] = ["title" => $title, "status" => "new", "bgg_id" => $bggId];
        }
    } else {
        $log[] = "❌ Fehler bei Spiel '$title'";
        $results[] = ["title" => $title, "status" => "error", "bgg_id" => $bggId];
        continue;
    }

    if ($profileDocumentId) {
        $scaledRating = $rating ? normalizeRatingToFiveScale($rating, $maxRating) : null;
        
        $assignResult = assignGameToUser($profileDocumentId, $gameId, $scaledRating);

        if (!$assignResult['success']) {
            $log[] = "⚠️ Konnte Spiel nicht zuweisen";
            $results[count($results) - 1]['collection_added'] = false;
        } else {
            $results[count($results) - 1]['collection_added'] = true;
        }
    }
}

fclose($handle);

// NEU: Prüfen ob Import abgeschlossen
$isComplete = ($offset + $processed) >= $totalRows;

if ($isComplete) {
    $log[] = "Import abgeschlossen. Lösche Datei...";
    if (unlink($filePath)) {
        $log[] = "Datei erfolgreich gelöscht";
    }
    if (isset($_SESSION['uploaded_files'][$cleanPath])) {
        unset($_SESSION['uploaded_files'][$cleanPath]);
    }
}

ob_end_clean();

echo json_encode([
    "success" => true,
    "batch" => [
        "offset" => $offset,
        "limit" => $limit,
        "processed" => $processed,
        "total_rows" => $totalRows,
        "is_complete" => $isComplete
    ],
    "stats" => [
        "total" => $total,
        "existing" => $existing,
        "new" => $new
    ],
    "results" => $results,
    "log" => $log
]);
exit;
?>