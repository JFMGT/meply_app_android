<?php
// ARCHITEKTUR: Nur die zentrale functions.php laden.
// Sie lädt config.php, get_meetings.php, is_logged_in_explizit(), esc() etc.
require_once '../config.php';
require_once '../functions.php';

// 1. Authentifizierung erzwingen (WICHTIGSTE ÄNDERUNG)
// Annahme: Nur eingeloggte Nutzer dürfen Meetings sehen.
if (!is_logged_in_explizit()) {
    // Wenn die Seite HTML erwartet, geben wir einen HTML-Fehler aus
    http_response_code(401); // Unauthorized
    // Verwende esc() auch für die Fehlermeldung zur Sicherheit
    echo '<p class="error">' . esc('❌ Bitte logge dich ein, um das Meeting zu sehen.') . '</p>';
    exit;
}

// 2. Eingabe validieren und bereinigen
$documentIdRaw = $_GET['id'] ?? null;

if (empty($documentIdRaw)) {
    http_response_code(400); // Bad Request
    echo esc('Fehler: Fehlende ID'); // Sicher ausgeben
    exit;
}

// SICHERHEITS-FIX: Prüfen, ob die ID das erwartete Format hat.
// Passe die Funktion an deine ID-Struktur an (z.B. isDocumentId()).
// Hier eine einfache Längenprüfung als Beispiel:
if (strlen($documentIdRaw) > 50 || !preg_match('/^[a-z0-9]+$/i', $documentIdRaw)) { // Beispiel-Regex
     http_response_code(400);
     echo esc('Fehler: Ungültige ID');
     exit;
}
// Die ID selbst muss NICHT escaped werden, da get_meetings sie sicher verwendet (via Helfer).
$documentId = $documentIdRaw;

// 3. Daten abrufen und direkt ausgeben
// $profile = getProfileData(); // UNNÖTIG und entfernt

// get_meetings('single', $documentId) macht die API-Abfrage und das Rendering.
// Da get_meetings HTML zurückgibt, können wir es direkt ausgeben.
$html = get_meetings('single', $documentId);

// Prüfen, ob die Funktion einen Fehler-HTML-String zurückgegeben hat (optional)
// Die Fehlerbehandlung findet bereits in get_meetings statt.
if (strpos($html, 'Fehler beim Abrufen') !== false) {
    // Setze ggf. den HTTP-Statuscode basierend auf dem Fehler
     if (strpos($html, 'nicht gefunden') !== false) {
         http_response_code(404);
     } else {
         http_response_code(500); // Generischer Serverfehler
     }
}

// Direkte Ausgabe des von get_meetings generierten HTMLs
echo $html;
// ob_start/ob_get_clean sind nicht nötig.
?>