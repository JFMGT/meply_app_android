<?php
// ARCHITEKTUR: session_start() ist hier nicht nötig (Admin-Token)
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt debugLog etc.

// CSRF-Token validieren
requireCsrfToken();

header("Content-Type: application/json; charset=UTF-8");

// SICHERHEIT: Debug-Einstellungen HIER ENTFERNEN!
// ini_set('display_errors', 1);
// error_reporting(E_ALL);

$strapiToken = "Bearer " . STRAPI_API_TOKEN;

// 1. Eingabe parsen und validieren (war bereits gut)
$data  = json_decode(file_get_contents("php://input"), true);
$email = trim($data['email'] ?? '');
$zip = trim($data['zip'] ?? '');

if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "❌ Ungültige E-Mail-Adresse."]); // Konsistentes Format
    exit;
}

if (empty($zip) || !preg_match("/^\d{5}$/", $zip)) { // Annahme: 5-stellige PLZ
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "❌ Ungültige Postleitzahl."]); // Konsistentes Format
    exit;
}

// 2. Daten für Strapi vorbereiten (war ok)
$strapiUrl = STRAPI_API_BASE . "/waitinglists/register";
$payload = json_encode([
    "email" => strtolower($email),
    "zip" => $zip
]);

// --- 3. Manuelles cURL (Beibehalten) mit Fehlerbehandlung ---
$ch = curl_init($strapiUrl);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_HTTPHEADER     => [
        "Authorization: $strapiToken",
        "Content-Type: application/json",
    ],
    CURLOPT_POSTFIELDS     => $payload,
    CURLOPT_TIMEOUT        => 10, // Timeout hinzugefügt
]);
$response = curl_exec($ch);

// --- 4. ROBUSTHEIT: Fehlerbehandlung hinzugefügt ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlErr  = curl_error($ch);
curl_close($ch);

if ($curlErr) {
    http_response_code(502); // Bad Gateway (Netzwerkproblem)
    debugLog("Waitinglist Register cURL Error: " . $curlErr);
    echo json_encode(["success" => false, "message" => "❌ Netzwerkfehler zur API."]);
    exit;
}
// --- Ende Robustheits-Fix ---

// 5. Antwort auswerten
if ($httpCode === 200) { // Annahme 200 OK
    $data = json_decode($response, true);
    $newDocumentId = $data['data']['documentId'] ?? null;

    if (!$newDocumentId) {
        http_response_code(500);
        debugLog("Waitinglist Register Error: HTTP 200, but documentId missing. Response: " . $response);
        echo json_encode(["success" => false, "message" => "❌ Unerwartete Antwort von Strapi (ID fehlt)."]);
        exit;
    }

    // Abmelde-Link bauen (war ok)
    $confirmUrl = WEBSITE_BASE . "community/waitinglist/?token=" . urlencode($newDocumentId);
    $subject = "Bitte bestätige deine Wartelisten-Anmeldung bei Meply";
    $message = "
    Hallo!

    Bitte bestätige deine Anmeldung für die Warteliste bei Meply über folgenden Link:

    $confirmUrl

    Wenn du dich nicht registriert hast, kannst du diese Nachricht ignorieren.
    ";
    
    // WARTUNG: Konstanten verwenden
    $headers = "From: " . (defined('MAIL_FROM') ? MAIL_FROM : 'info@meply.de') . "\r\n";
    $headers .= "Content-Type: text/plain; charset=UTF-8\r\n";

    // ROBUSTHEIT: mail()-Fehler abfangen
    $sent = @mail($email, $subject, $message, $headers); // @ unterdrückt Fehler

    if ($sent) {
        echo json_encode(["success" => true, "message" => "✅ Wartelisten-Eintrag erfolgreich. Bitte bestätige deine E-Mail-Adresse."]);
    } else {
        http_response_code(500);
        debugLog("Waitinglist Register Mail Error: mail() returned false for: " . $email);
        // SICHERHEIT: Info Disclosure entfernt
        echo json_encode(["success" => false, "message" => "❌ Die Bestätigungs-E-Mail konnte nicht gesendet werden."]);
    }

} elseif ($httpCode === 409) {
    // Konflikt (war gut)
    http_response_code(409); // Sicherstellen, dass der Code gesetzt ist
    echo json_encode(["success" => false, "message" => "❌ Diese E-Mail-Adresse ist bereits auf der Warteliste."]);
} else {
    // Anderer API-Fehler
    http_response_code($httpCode);
    debugLog("Waitinglist Register API Error: HTTP {$httpCode} - " . $response);
    // SICHERHEIT: Info Disclosure entfernt
    echo json_encode(["success" => false, "message" => "❌ Fehler beim Eintrag in die Warteliste."]);
    // "debug" => $response entfernt!
}
?>