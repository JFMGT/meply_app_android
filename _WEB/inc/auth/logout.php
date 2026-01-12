<?php
// ARCHITEKTUR: session_start() sollte zentral sein (z.B. in config.php)
session_start();
// ARCHITEKTUR: config.php sollte zentral geladen werden
require_once('../config.php'); 
// Optional: functions.php laden, falls debugLog genutzt werden soll
// require_once('../functions.php'); 

// --- 1. SICHERHEIT: CSRF-Schutz (Nur POST erlauben) ---
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405); // Method Not Allowed
    header('Allow: POST');
    // Optional: JSON-Antwort für JS-Aufrufe
    // header('Content-Type: application/json');
    // echo json_encode(['success' => false, 'error' => 'Nur POST erlaubt']);
    exit;
}

// --- 2. Logout-Logik (war korrekt) ---
session_unset();    // Entfernt alle Session-Variablen
session_destroy();  // Beendet die Session komplett

// --- 3. ROBUSTHEIT: Sicherer Redirect ---
// Optional: Umleitung zurück zur Startseite o.ä.
$redirectTo = '/'; // Hartcodiertes, sicheres Ziel

if (!headers_sent()) { // Prüfen, ob Ausgabe bereits stattgefunden hat
    header("Location: $redirectTo");
    exit;
} else {
    // Fallback, falls Header gesendet wurden (z.B. HTML-Redirect)
    // debugLog("Logout-Redirect fehlgeschlagen: Headers already sent.");
    echo "<p>Ausgeloggt. Sie werden weitergeleitet...</p>";
    echo "<script>window.location.href='{$redirectTo}';</script>";
    exit;
}
?>