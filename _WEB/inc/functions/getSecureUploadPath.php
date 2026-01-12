<?php
/**
 * Gibt einen sicheren Pfad für Upload-Dateien zurück
 * Verhindert Path Traversal Attacken
 */
/**
 * Gibt einen sicheren Pfad für Upload-Dateien zurück
 * Verhindert Path Traversal Attacken
 */
function getSecureUploadPath($filename) {
    // WICHTIG: Einheitlicher Pfad für ALLE Upload-Skripte
    // Von /inc/api/ aus gesehen ist uploads in ../../uploads
    // Von /inc/functions.php aus gesehen ist uploads in ../uploads
    
    // Wir nutzen immer den absoluten Pfad vom DocumentRoot
    $documentRoot = $_SERVER['DOCUMENT_ROOT'] ?? __DIR__ . '/../../';
    $uploadDir = realpath($documentRoot . '/uploads');
    
    if (!$uploadDir) {
        // Falls nicht vorhanden, erstellen
        $uploadDir = $documentRoot . '/uploads';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }
        $uploadDir = realpath($uploadDir);
    }
    
    $cleanName = basename($filename);
    $fullPath = $uploadDir . DIRECTORY_SEPARATOR . $cleanName;
    
    // Prüfe, ob der finale Pfad wirklich im Upload-Verzeichnis liegt
    $realBase = realpath($uploadDir);
    $userPath = $uploadDir . DIRECTORY_SEPARATOR . $cleanName;
    $commonPath = substr($userPath, 0, strlen($realBase));
    
    if ($commonPath !== $realBase) {
        throw new Exception("Ungültiger Dateipfad");
    }
    
    return $fullPath;
}

/**
 * Prüft ob eine hochgeladene Datei dem aktuellen User gehört
 */
function verifyFileOwnership($filename, $profileDocumentId) {
    if (!isset($_SESSION['uploaded_files'])) {
        return false;
    }
    
    return isset($_SESSION['uploaded_files'][$filename]) && 
           $_SESSION['uploaded_files'][$filename] === $profileDocumentId;
}

/**
 * Registriert eine hochgeladene Datei für den aktuellen User
 */
function registerUploadedFile($filename, $profileDocumentId) {
    if (!isset($_SESSION['uploaded_files'])) {
        $_SESSION['uploaded_files'] = [];
    }
    $_SESSION['uploaded_files'][$filename] = $profileDocumentId;
}

/**
 * Rate Limiting für Uploads
 */
function checkUploadRateLimit($profileDocumentId, $maxUploads = 10, $timeWindow = 3600) {
    $uploadKey = 'upload_count_' . $profileDocumentId;
    
    if (!isset($_SESSION[$uploadKey])) {
        $_SESSION[$uploadKey] = ['count' => 0, 'reset' => time()];
    }
    
    $uploads = $_SESSION[$uploadKey];
    
    // Reset nach Zeitfenster
    if (time() - $uploads['reset'] > $timeWindow) {
        $_SESSION[$uploadKey] = ['count' => 0, 'reset' => time()];
        return true;
    }
    
    if ($uploads['count'] >= $maxUploads) {
        return false;
    }
    
    $_SESSION[$uploadKey]['count']++;
    return true;
}