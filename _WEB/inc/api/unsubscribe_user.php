<?php
// ARCHITEKTUR: session_start() ist hier nicht nötig (Admin-Token)
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../functions.php'); // Lädt config, Helfer, debugLog etc.

// CSRF-Token validieren
requireCsrfToken();

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// SICHERHEIT: Debug-Einstellungen HIER ENTFERNEN!
// ini_set('display_errors', 1);
// error_reporting(E_ALL);

// 1. Eingabe prüfen (Logik war gut)
$data  = json_decode(file_get_contents("php://input"), true);
$email = trim($data['email'] ?? '');

if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "❌ Ungültige E-Mail-Adresse."]);
    exit;
}

// 2. API-Aufruf mit Admin-Helfer (DRY & Robustheit)
$lookupUrl = STRAPI_API_BASE . "/waitinglists/lookup";
$payload = [ "email" => strtolower($email) ];

// KORREKTUR: Admin-Helfer verwenden
$result = makeAdminStrapiApiCall('POST', $lookupUrl, $payload);
debugLog("Waitinglist Lookup Call: " . json_encode($result)); // Log für Debugging

// 3. Fehlerbehandlung (aus dem Helfer)
if (!$result['success']) {
    // Spezialfall 404: E-Mail nicht gefunden (war bereits gut)
    if ($result['code'] === 404) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "❌ Keine Anmeldung unter dieser E-Mail gefunden."]);
        exit;
    }

    // Anderer Fehler (Netzwerk, 500 etc.)
    http_response_code(500); // Generischer Serverfehler
    // SICHERHEIT: Detaillierten Fehler nur server-seitig loggen
    debugLog("Waitinglist Lookup Error: HTTP {$result['code']} - " . json_encode($result));
    echo json_encode([
        "success" => false,
        "message" => "❌ Fehler beim Abrufen der Daten."
        // "debug"-Schlüssel entfernt
    ]);
    exit;
}

// 4. Daten extrahieren (Logik war gut)
$documentId = $result['response']['data']['documentId'] ?? null;

if (!$documentId) {
    http_response_code(500);
    debugLog("Waitinglist Lookup Error: API success, but documentId missing in response: " . json_encode($result['response']));
    echo json_encode(["success" => false, "message" => "❌ Unerwartete Antwort von Strapi (kein Token)."]);
    exit;
}

// 5. E-Mail senden (Logik beibehalten, aber sicherer)
$unsubscribeUrl = WEBSITE_BASE . "community/waitinglist/unsubscribe_confirm.php?token=" . urlencode($documentId); // urlencode war gut

// ARCHITEKTUR-HINWEIS: mail() ist unzuverlässig. Besser PHPMailer mit SMTP verwenden.
$subject = "Deine Abmeldung von der Meply-Warteliste";
$message = "
Hallo,

du hast angefragt, deine Wartelisten-Anmeldung bei Meply zu beenden.

Bitte bestätige deine Abmeldung über folgenden Link:
$unsubscribeUrl

Falls du dich nicht austragen möchtest, kannst du diese Nachricht ignorieren.
";

// WARTUNG: Konstanten verwenden statt hartcodierter E-Mail
$headers = "From: " . (defined('MAIL_FROM') ? MAIL_FROM : 'info@meply.de') . "\r\n";
$headers .= "Content-Type: text/plain; charset=UTF-8\r\n"; // Wichtig für Umlaute

$sent = @mail($email, $subject, $message, $headers); // @ unterdrückt PHP-Fehler bei mail()

if ($sent) {
    echo json_encode(["success" => true, "message" => "✅ Wir haben dir eine E-Mail mit einem Abmeldelink geschickt."]);
} else {
    http_response_code(500);
    debugLog("Waitinglist Unsubscribe Mail Error: mail() returned false for: " . $email);
    // SICHERHEIT: Keine Debug-Infos an Client
    echo json_encode([
        "success" => false,
        "message" => "❌ Die E-Mail konnte nicht gesendet werden."
        // "debug"-Schlüssel entfernt
    ]);
}