<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../../inc/functions.php'); // Lädt config, Helfer, esc etc.

$documentId = $_GET['token'] ?? null;
$message = '';
$color   = 'red'; // Standardfarbe für Fehler

if ($documentId) {
    // 1. API-Aufruf mit Admin-Helfer
    // Wir nutzen den Admin-Helfer, da der User hier nicht eingeloggt sein muss
    $url = STRAPI_API_BASE . "/waitinglists/unsubscribe";
    $payload = ["documentId" => $documentId];

    $result = makeAdminStrapiApiCall('POST', $url, $payload);

    // 2. Ergebnis auswerten
    if ($result['success']) {
        $message = "✅ Du wurdest erfolgreich von der Warteliste abgemeldet.";
        $color   = "green";
    } else {
        // Differenzierte Fehlermeldungen je nach Code
        if ($result['code'] === 404) {
             $message = "❌ Der Abmeldelink ist ungültig oder wurde bereits verwendet.";
        } else {
             // Allgemeiner technischer Fehler (loggen für Admin)
             debugLog("Unsubscribe Confirm Error: " . json_encode($result));
             $message = "❌ Technischer Fehler bei der Abmeldung. Bitte versuche es später erneut.";
        }
    }
} else {
    $message = "❌ Ungültiger Link – kein Token vorhanden.";
}

include('../../inc/header.php');
?>

<div class="content top-margin content-plain">
    <div class="card" style="color: <?= esc($color) ?>; padding:2rem; margin-top:2rem">
        <p><?= esc($message) ?></p>
    </div>
    
    <div class="card">
        <a href="/">Zurück zur Startseite</a>
    </div>
</div>

<?php include('../../inc/footer.php'); ?>