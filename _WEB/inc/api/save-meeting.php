<?php
// ARCHITEKTUR: session_start() sollte zentral sein
session_start();
// ARCHITEKTUR: Abhängigkeiten zentral laden
require_once('../config.php');
require_once('../functions.php'); // Lädt config, Helfer, is_logged_in_explizit, debugLog etc.

// CSRF-Token validieren
requireCsrfToken();

// Setzt JSON Header FRÜH
header('Content-Type: application/json; charset=utf-8');

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode(['success' => false, 'error' => 'Nicht autorisiert']);
    exit;
}
$jwt = $_SESSION['jwt']; // JWT ist gültig

// --- 2. Eingabe holen und 'dates'-Logik (Unverändert) ---
$input = json_decode(file_get_contents('php://input'), true);

if (!$input || !isset($input['data'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Ungültige Eingabe']);
    exit;
}

$data = $input['data'];

// ✅ SICHERHEITS-FIX: author-Feld vom Client IMMER ignorieren
// Das Strapi-Backend setzt den author automatisch auf den eingeloggten User
unset($data['author']);

$dates = null;
$date = null; // $date initialisieren

// 1. Event-Gesuch
if (!empty($data['event']) && !empty($data['eventDays']) && is_array($data['eventDays'])) {
    if (count($data['eventDays']) === 0) {
        http_response_code(400);
        echo json_encode(['error' => 'Bitte wähle mindestens einen Tag beim Event aus.']);
        exit;
    }
    $dates = [
        'type' => 'eventDays',
        'value' => [
            'eventId' => $data['event'],
            'days' => $data['eventDays']
        ]
    ];
    $date = end($data['eventDays']);
}
// 2. Freies Gesuch mit dateType
elseif (!empty($data['dateType'])) {
    switch ($data['dateType']) {
        case 'fixed':
            if (empty($data['date'])) {
                http_response_code(400);
                echo json_encode(['error' => 'Bitte gib ein Datum & Uhrzeit an.']);
                exit;
            }
            $dates = [
                'type' => 'fixed',
                'value' => [ 'date' => $data['date'] ]
            ];
            $date = $data['date'];
            break;

        case 'range':
            if (empty($data['dateFrom']) || empty($data['dateTo'])) {
                http_response_code(400);
                echo json_encode(['error' => 'Bitte gib einen Zeitraum mit Start- und Enddatum an.']);
                exit;
            }
            $dates = [
                'type' => 'range',
                'value' => [ 'start' => $data['dateFrom'], 'end' => $data['dateTo'] ]
            ];
            $date = $data['dateTo'];
            break;

        case 'recurring':
            if (empty($data['recurringDays']) || !is_array($data['recurringDays']) || count($data['recurringDays']) === 0) {
                http_response_code(400);
                echo json_encode(['error' => 'Bitte wähle mindestens einen Wochentag aus.']);
                exit;
            }
            $frequency = $data['recurringFrequency'] ?? 'weekly';
            if (!in_array($frequency, ['weekly', 'monthly', 'quarterly'])) {
                http_response_code(400);
                echo json_encode(['error' => 'Ungültige Wiederholungsfrequenz.']);
                exit;
            }
            $dates = [
                'type' => 'recurring',
                'value' => [
                    'days' => $data['recurringDays'],
                    'frequency' => $frequency
                ]
            ];
            // $date bleibt null, was ok ist für recurring
            break;

        default:
            http_response_code(400);
            echo json_encode(['error' => 'Ungültiger Zeittyp.']);
            exit;
    }
}

if (!$dates) {
    http_response_code(400);
    echo json_encode(['error' => 'Keine gültigen Datumsangaben gefunden.']);
    exit;
}

$data['dates'] = $dates;
$data['date'] = $date; 
unset(
    $data['dateFrom'], $data['dateTo'], $data['recurringDays'],
    $data['recurringFrequency'], $data['eventDays'], $data['dateType']
);
// --- Ende 'dates'-Logik ---


// --- 3. API-Aufruf vorbereiten (Mit Sicherheits-Fix) ---
$meetingId = isset($input['id']) ? $input['id'] : null;

// --- SICHERHEITS-FIX: Path Injection verhindern ---
$url = $meetingId
    ? STRAPI_API_BASE . "/meetings/" . urlencode($meetingId) // ID wird URL-encoded
    : STRAPI_API_BASE . "/meetings";

$method = $meetingId ? 'PUT' : 'POST';
debugLog("Create/Update Meeting URL: " . $url . " (Method: $method)");

// --- 4. Manuelles cURL (Beibehalten) mit Fehlerbehandlung ---
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(['data' => $data]));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $jwt
]);
curl_setopt($ch, CURLOPT_TIMEOUT, 10); // Timeout hinzugefügt

$response = curl_exec($ch);

// --- ROBUSTHEITS-FIX: Fehlerbehandlung hinzugefügt ---
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

debugLog("Create/Update Meeting Response Code: " . $httpCode);
debugLog("Create/Update Meeting cURL Error: " . ($curlError ?: 'Kein Fehler'));

if ($curlError) {
    http_response_code(502); // Bad Gateway (Netzwerkproblem)
    debugLog("Create/Update Meeting cURL Error: " . $curlError);
    echo json_encode(['success' => false, 'error' => 'Netzwerkfehler zur API.']);
    exit;
}

// --- 5. Antwort zurückgeben (Stil beibehalten, aber sicherer) ---
http_response_code($httpCode); // Setze den von Strapi erhaltenen Code

if ($httpCode >= 200 && $httpCode < 300) {
    echo $response; // Erfolg: Roh-Antwort (wie im Original)
} else {
    // Fehlerfall
    debugLog("Create/Update Meeting API Error: HTTP {$httpCode} - " . $response);
    // SICHERHEITS-FIX: Info Disclosure verhindern
    // Versuche, Strapi-Fehler zu parsen
    $errorData = json_decode($response, true);
    $errorMsg = $errorData['error']['message'] ?? 'Fehler bei der Aktion.';
    echo json_encode(['success' => false, 'error' => $errorMsg]); // Nur saubere Fehlermeldung
}