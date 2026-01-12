<?php
session_start();
require_once('../../inc/functions.php');

// Sicherheit: Login prüfen
if (!is_logged_in_explizit()) {
    header("Location: " . WEBSITE_BASE . "/community/login.php?error=session_expired");
    exit();
}

// Eingabe prüfen
$newVersion = $_POST['version'] ?? null;
if (empty($newVersion)) {
    die("Keine Versionsnummer angegeben.");
}

// Aktuelles Log holen (aus Session oder API)
$existingLog = $_SESSION['profile']['privacy_acceptance_log'] ?? [];
if (is_string($existingLog)) {
    $existingLog = json_decode($existingLog, true) ?? [];
}
if (!is_array($existingLog)) {
    $existingLog = [];
}

// Neuen Eintrag hinzufügen
$existingLog[] = [
    'version' => $newVersion,
    'accepted_at' => date('c'),
];

// API-Aufruf: Eigenes Profil updaten
$url = STRAPI_API_BASE . '/profiles/me';
$payload = [
    'data' => [
        'privacy_acceptance_log' => $existingLog
    ]
];

$result = makeStrapiApiCall('PUT', $url, $payload);

if ($result['success']) {
    // Session aktualisieren
    $_SESSION['profile']['lastAcceptedPrivacyVersion'] = $newVersion;
    $_SESSION['profile']['privacy_acceptance_log'] = $existingLog;
    
    header("Location: " . WEBSITE_BASE);
    exit();
} else {
    debugLog("Privacy Save Error: " . json_encode($result));
    die("Fehler beim Speichern der Zustimmung.");
}
?>