<?php
// ARCHITEKTUR: Abhängigkeiten zentral laden
session_start();
require_once('../config.php');
require_once('../functions.php'); // Lädt ALLE Helfer, logoutUser, setScheduledDeletion etc.

// CSRF-Token validieren
requireCsrfToken();

header('Content-Type: application/json'); // Header früh setzen

// --- 0. Method & Input Check ---
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit;
}
$data = json_decode(file_get_contents('php://input'), true);
$password = $data['password'] ?? null;
if (!$password) {
    http_response_code(400);
    echo json_encode(['error' => 'Passwort erforderlich']);
    exit;
}

// --- 1. SICHERHEIT: Explizite Login-Prüfung ---
// (Statt des unsicheren 'isset($_SESSION['jwt'])'-Checks)
if (!is_logged_in_explizit()) {
    http_response_code(401);
    echo json_encode(['error' => 'Nicht autorisiert oder Sitzung abgelaufen']);
    exit;
}
// $jwt wird vom Helfer automatisch aus der Session geholt

// --- 2. Benutzerdaten via JWT abrufen (mit Helfer) ---
$resultUserMe = makeStrapiApiCall('GET', STRAPI_API_BASE . '/users/me');

if (!$resultUserMe['success']) {
    http_response_code(401);
    echo json_encode(['error' => 'Benutzer konnte nicht abgerufen werden']);
    exit;
}
$user = $resultUserMe['response'];
$userId = $user['documentId'] ?? null; // userDocumentId
$userEmail = $user['email'] ?? null;

if (!$userId || !$userEmail) {
    http_response_code(500);
    echo json_encode(['error' => 'Benutzer-ID oder E-Mail fehlt in API-Antwort']);
    exit;
}

// --- 3. Passwort prüfen (mit Admin-Helfer) ---
// Verwendet makeAdminStrapiApiCall, wie für /auth/local besprochen
$authPayload = ['identifier' => $userEmail, 'password' => $password];
$resultAuth = makeAdminStrapiApiCall('POST', STRAPI_API_BASE . '/auth/local', $authPayload);

if (!$resultAuth['success']) {
    http_response_code(403); // 403 Forbidden (falsches Passwort)
    echo json_encode(['error' => 'Falsches Passwort']);
    exit;
}
// Passwort ist korrekt.

// --- 4. Profil finden (mit Helfer & Sicherer Query) ---
// SICHERHEITS-FIX: http_build_query statt String-Verkettung
$profileParams = ['filters[userDocumentId][$eq]' => $userId];
$profileUrl = STRAPI_API_BASE . '/profiles?' . http_build_query($profileParams);

$resultProfile = makeStrapiApiCall('GET', $profileUrl); // Nutzt User-JWT

if (!$resultProfile['success'] || empty($resultProfile['response']['data'])) {
    http_response_code($resultProfile['code'] ?: 404); // 404 Not Found
    echo json_encode(['error' => 'Kein passendes Profil gefunden']);
    exit;
}

$profileId = $resultProfile['response']['data'][0]['documentId'] ?? null;

if (!$profileId) {
    http_response_code(404);
    echo json_encode(['error' => 'Profil-ID im Profil-Objekt nicht gefunden']);
    exit;
}

// --- 5. Löschdatum setzen (Ihr korrigierter Code-Teil) ---
// (Dieser Teil war bereits korrekt an die neue Funktion angepasst)
$currentDeleteDate = $_SESSION['profile']['deleteDate'] ?? null; 
$result = setScheduledDeletion($profileId, $currentDeleteDate);

if ($result['success']) {
    $_SESSION['profile']['deleteDate'] = $result['scheduledDeletionAt'];
    logoutUser('/?message=deletion_update_successful'); 
    exit; // Wichtig nach logoutUser
} else {
    http_response_code(500); 
    echo json_encode(['error' => 'Fehler beim Aktualisieren des Profils']);
    exit;
}
?>