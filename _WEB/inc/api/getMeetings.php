<?php
// ARCHITEKTUR: Session ZENTRAL starten (z.B. in config.php)
session_start();
header('Content-Type: application/json');

// ARCHITEKTUR: Nur zentrale functions.php laden
require_once('../config.php'); 
require_once('../functions.php'); // Lädt config, Helfer, get_meetings, requireLogin etc.

// 1. Authentifizierung erzwingen
if (!requireLogin()) { // Annahme: requireLogin behandelt Fehler und exit
    // Fallback, falls requireLogin nur false zurückgibt
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Nicht autorisiert']);
    exit;
}

// 2. Eingabe holen und validieren
$type = $_GET['type'] ?? null;
$documentId = $_GET['documentId'] ?? null;
$onlyUnlinked = isset($_GET['onlyUnlinked']) && $_GET['onlyUnlinked'] == 'true';

// Optionale Validierung (Beispiel)
if ($documentId && function_exists('isDocumentId') && !isDocumentId($documentId)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Ungültige documentId']);
    exit;
}
if ($type && !in_array($type, ['single', 'author', 'location', 'event'])) { // Erlaubte Typen definieren
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'Ungültiger Typ']);
    exit;
}


// 3. Funktion aufrufen
// get_meetings gibt bei Erfolg HTML zurück, bei Fehler HTML mit Fehlermeldung
$html = get_meetings($type, $documentId, $onlyUnlinked);

// 4. Fehlerbehandlung basierend auf dem HTML-Output von get_meetings
// Prüfen, ob der zurückgegebene String eine bekannte Fehlermeldung enthält
if (strpos($html, 'Fehler beim Abrufen') !== false ||
    strpos($html, 'Keine Location-ID') !== false ||
    strpos($html, 'Du musst eingeloggt sein') !== false || // Sollte durch requireLogin abgedeckt sein
    strpos($html, 'nicht gefunden') !== false ) // z.B. bei 'single' und 404
{
    // Extrahiere ggf. den Statuscode aus der Fehlermeldung oder setze Standard
    $errorCode = 500; // Standard-Serverfehler
     if (strpos($html, 'nicht gefunden') !== false) $errorCode = 404;
     if (strpos($html, 'Nicht eingeloggt') !== false) $errorCode = 401;

    http_response_code($errorCode);
    // Gib nur eine generische Fehlermeldung oder die Meldung aus dem HTML (sicher escaped)
    echo json_encode([
        'success' => false,
        // Optional: Die HTML-Fehlermeldung extrahieren und sicher ausgeben
        'error' => esc(strip_tags($html)) // Entfernt HTML, escaped den Rest
    ]);
} else {
    // Erfolg
    echo json_encode([
        'success' => true,
        'html' => $html, // Das von get_meetings generierte HTML
    ]);
}
?>